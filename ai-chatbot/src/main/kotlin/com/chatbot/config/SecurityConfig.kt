package com.chatbot.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtDecoders
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter

@Configuration
@EnableWebSecurity
class SecurityConfig {

    private val logger = LoggerFactory.getLogger(SecurityConfig::class.java)

    @Value("\${jwt.authorities-claim-name}")
    private lateinit var authoritiesClaimName: String

    @Value("\${jwt.authority-prefix}")
    private lateinit var authorityPrefix: String

    @Value("\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private lateinit var jwkSetUri: String

    @Value("\${app.security.auth-enabled:true}")
    private var authEnabled: Boolean = true

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }

        if (!authEnabled) {
            logger.warn("Authentication is disabled (app.security.auth-enabled=false). All endpoints are publicly accessible.")
            http.authorizeHttpRequests { authz ->
                authz.anyRequest().permitAll()
            }
            return http.build()
        }

        http
            .authorizeHttpRequests { authz ->
                authz
                    .requestMatchers("/actuator/health", "/error").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.decoder(jwtDecoder())
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                }
                oauth2.authenticationEntryPoint { request, response, authException ->
                    logger.error("JWT Authentication failed: ${authException.message}", authException)
                    println("========= JWT AUTHENTICATION ERROR =========")
                    println("Error: ${authException.message}")
                    println("Request URI: ${request.requestURI}")
                    println("Auth Header: ${request.getHeader("Authorization")?.take(50)}...")
                    println("==========================================")
                    response.sendError(401, "Unauthorized: ${authException.message}")
                }
            }
            .addFilterAfter(UserAuthenticationLoggingFilter(), BasicAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun jwtDecoder(): JwtDecoder {
        logger.info("Configuring JWT Decoder with JWK Set URI: $jwkSetUri")
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build()
    }

    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val grantedAuthoritiesConverter = JwtGrantedAuthoritiesConverter()
        grantedAuthoritiesConverter.setAuthoritiesClaimName(authoritiesClaimName)
        grantedAuthoritiesConverter.setAuthorityPrefix(authorityPrefix)

        val jwtAuthenticationConverter = JwtAuthenticationConverter()
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter)
        return jwtAuthenticationConverter
    }

    /**
     * Filter to log authenticated user information and their permissions
     */
    inner class UserAuthenticationLoggingFilter : OncePerRequestFilter() {
        private val filterLogger = LoggerFactory.getLogger(UserAuthenticationLoggingFilter::class.java)

        override fun doFilterInternal(
            request: HttpServletRequest,
            response: HttpServletResponse,
            filterChain: FilterChain
        ) {
            val authentication = SecurityContextHolder.getContext().authentication

            if (authentication != null && authentication.isAuthenticated) {
                val username = authentication.name
                val authorities = authentication.authorities.map { it.authority }.joinToString(", ")
                val requestUri = request.requestURI
                val method = request.method

                filterLogger.info("===============================================")
                filterLogger.info("User Access Details:")
                filterLogger.info("  - Username: $username")
                filterLogger.info("  - Authorities/Permissions: [$authorities]")
                filterLogger.info("  - Endpoint: $method $requestUri")
                filterLogger.info("  - Principal Type: ${authentication.principal.javaClass.simpleName}")
                filterLogger.info("===============================================")

                // Also print to console for immediate visibility
                println("===============================================")
                println("User Access Details:")
                println("  - Username: $username")
                println("  - Authorities/Permissions: [$authorities]")
                println("  - Endpoint: $method $requestUri")
                println("  - Principal Type: ${authentication.principal.javaClass.simpleName}")
                println("===============================================")
            }

            filterChain.doFilter(request, response)
        }
    }
}
