package com.chatbot.controller

import com.chatbot.model.*
import com.chatbot.service.SecurityConfigService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/security-configs")
class SecurityConfigController(private val securityConfigService: SecurityConfigService) {
    
    @GetMapping
    fun getAllSecurityConfigs(): ResponseEntity<List<SecurityConfigResponse>> {
        val configs = securityConfigService.getAllSecurityConfigs()
        return ResponseEntity.ok(configs)
    }
    
    @PostMapping("/ssl")
    fun createSslConfig(@RequestBody request: CreateSslConfigRequest): ResponseEntity<SslConfig> {
        if (request.name.isBlank()) {
            return ResponseEntity.badRequest().build()
        }
        val config = securityConfigService.createSslConfig(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(config)
    }
    
    @GetMapping("/ssl")
    fun getAllSslConfigs(): ResponseEntity<List<SslConfig>> {
        val configs = securityConfigService.getAllSslConfigs()
        return ResponseEntity.ok(configs)
    }
    
    @GetMapping("/ssl/{id}")
    fun getSslConfig(@PathVariable id: String): ResponseEntity<SslConfig> {
        val config = securityConfigService.getSslConfig(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(config)
    }
    
    @DeleteMapping("/ssl/{id}")
    fun deleteSslConfig(@PathVariable id: String): ResponseEntity<Void> {
        val deleted = securityConfigService.deleteSslConfig(id)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @PostMapping("/database")
    fun createDatabaseCredentials(@RequestBody request: CreateDatabaseCredentialsRequest): ResponseEntity<DatabaseCredentials> {
        if (request.name.isBlank() || request.jdbcUrl.isBlank() || request.username.isBlank()) {
            return ResponseEntity.badRequest().build()
        }
        val credentials = securityConfigService.createDatabaseCredentials(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(credentials)
    }
    
    @GetMapping("/database")
    fun getAllDatabaseCredentials(): ResponseEntity<List<DatabaseCredentials>> {
        val credentials = securityConfigService.getAllDatabaseCredentials()
        return ResponseEntity.ok(credentials)
    }
    
    @GetMapping("/database/{id}")
    fun getDatabaseCredentials(@PathVariable id: String): ResponseEntity<DatabaseCredentials> {
        val credentials = securityConfigService.getDatabaseCredentials(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(credentials)
    }
    
    @DeleteMapping("/database/{id}")
    fun deleteDatabaseCredentials(@PathVariable id: String): ResponseEntity<Void> {
        val deleted = securityConfigService.deleteDatabaseCredentials(id)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
