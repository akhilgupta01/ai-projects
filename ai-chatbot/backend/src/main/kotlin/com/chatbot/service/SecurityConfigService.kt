package com.chatbot.service

import com.chatbot.model.*
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class SecurityConfigService {
    
    private val sslConfigs = ConcurrentHashMap<String, SslConfig>()
    private val databaseCredentials = ConcurrentHashMap<String, DatabaseCredentials>()
    
    fun createSslConfig(request: CreateSslConfigRequest): SslConfig {
        val config = SslConfig(
            name = request.name,
            description = request.description,
            trustStorePath = request.trustStorePath,
            trustStorePassword = request.trustStorePassword,
            keyStorePath = request.keyStorePath,
            keyStorePassword = request.keyStorePassword,
            verifyHostname = request.verifyHostname
        )
        sslConfigs[config.id] = config
        return config
    }
    
    fun getAllSslConfigs(): List<SslConfig> {
        return sslConfigs.values.sortedByDescending { it.createdAt }
    }
    
    fun getSslConfig(id: String): SslConfig? {
        return sslConfigs[id]
    }
    
    fun deleteSslConfig(id: String): Boolean {
        return sslConfigs.remove(id) != null
    }
    
    fun createDatabaseCredentials(request: CreateDatabaseCredentialsRequest): DatabaseCredentials {
        val credentials = DatabaseCredentials(
            name = request.name,
            description = request.description,
            jdbcUrl = request.jdbcUrl,
            username = request.username,
            password = request.password,
            driverClassName = request.driverClassName
        )
        databaseCredentials[credentials.id] = credentials
        return credentials
    }
    
    fun getAllDatabaseCredentials(): List<DatabaseCredentials> {
        return databaseCredentials.values.sortedByDescending { it.createdAt }
    }
    
    fun getDatabaseCredentials(id: String): DatabaseCredentials? {
        return databaseCredentials[id]
    }
    
    fun deleteDatabaseCredentials(id: String): Boolean {
        return databaseCredentials.remove(id) != null
    }
    
    fun getAllSecurityConfigs(): List<SecurityConfigResponse> {
        val sslResponses = sslConfigs.values.map { config ->
            SecurityConfigResponse(
                id = config.id,
                name = config.name,
                description = config.description,
                type = SecurityConfigType.SSL_CONFIG,
                createdAt = config.createdAt
            )
        }
        
        val dbResponses = databaseCredentials.values.map { cred ->
            SecurityConfigResponse(
                id = cred.id,
                name = cred.name,
                description = cred.description,
                type = SecurityConfigType.DATABASE_CREDENTIALS,
                createdAt = cred.createdAt
            )
        }
        
        return (sslResponses + dbResponses).sortedByDescending { it.createdAt }
    }
}
