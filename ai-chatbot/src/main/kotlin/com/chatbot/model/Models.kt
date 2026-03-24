package com.chatbot.model

import java.time.Instant
import java.util.UUID

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Chat",
    val createdAt: Instant = Instant.now(),
    val messages: MutableList<ChatMessage> = mutableListOf()
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Instant = Instant.now()
)

enum class MessageRole {
    USER,
    ASSISTANT
}

data class CreateSessionRequest(
    val title: String? = null
)

data class SendMessageRequest(
    val content: String
)

data class SessionResponse(
    val id: String,
    val title: String,
    val createdAt: Instant,
    val messageCount: Int
)

data class SessionDetailResponse(
    val id: String,
    val title: String,
    val createdAt: Instant,
    val messages: List<ChatMessage>
)

data class MessageResponse(
    val userMessage: ChatMessage,
    val assistantMessage: ChatMessage
)

data class KnowledgeDocument(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val content: String,
    val contentType: String,
    val size: Long,
    val uploadedAt: Instant = Instant.now(),
    val tags: List<String> = emptyList()
)

data class UploadDocumentRequest(
    val name: String,
    val content: String,
    val contentType: String,
    val tags: List<String>? = null
)

data class DocumentResponse(
    val id: String,
    val name: String,
    val contentType: String,
    val size: Long,
    val uploadedAt: Instant,
    val tags: List<String>
)

data class SearchDocumentsRequest(
    val query: String,
    val tags: List<String>? = null
)

// Security Configuration Models
enum class SecurityConfigType {
    SSL_CONFIG,
    DATABASE_CREDENTIALS
}

data class SslConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val trustStorePath: String? = null,
    val trustStorePassword: String? = null,
    val keyStorePath: String? = null,
    val keyStorePassword: String? = null,
    val verifyHostname: Boolean = true,
    val createdAt: Instant = Instant.now()
)

data class DatabaseCredentials(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val driverClassName: String? = null,
    val createdAt: Instant = Instant.now()
)

data class SecurityConfigResponse(
    val id: String,
    val name: String,
    val description: String?,
    val type: SecurityConfigType,
    val createdAt: Instant
)

data class CreateSslConfigRequest(
    val name: String,
    val description: String? = null,
    val trustStorePath: String? = null,
    val trustStorePassword: String? = null,
    val keyStorePath: String? = null,
    val keyStorePassword: String? = null,
    val verifyHostname: Boolean = true
)

data class CreateDatabaseCredentialsRequest(
    val name: String,
    val description: String? = null,
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val driverClassName: String? = null
)

// Tool and Toolset Models
enum class ToolType {
    HTTP_ENDPOINT,
    JDBC_QUERY
}

data class ToolParameter(
    val name: String,
    val description: String? = null,
    val type: String = "string",
    val required: Boolean = false,
    val defaultValue: String? = null
)

data class HttpEndpointConfig(
    val url: String,
    val method: String = "GET",
    val headers: Map<String, String> = emptyMap(),
    val bodyTemplate: String? = null,
    val sslConfigId: String? = null
)

data class JdbcQueryConfig(
    val queryTemplate: String,
    val databaseCredentialsId: String
)

data class Tool(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val type: ToolType,
    val parameters: List<ToolParameter> = emptyList(),
    val httpConfig: HttpEndpointConfig? = null,
    val jdbcConfig: JdbcQueryConfig? = null,
    val createdAt: Instant = Instant.now()
)

data class Toolset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val tools: MutableList<Tool> = mutableListOf(),
    val createdAt: Instant = Instant.now()
)

data class ToolsetResponse(
    val id: String,
    val name: String,
    val description: String?,
    val toolCount: Int,
    val createdAt: Instant
)

data class CreateToolsetRequest(
    val name: String,
    val description: String? = null
)

data class CreateToolRequest(
    val name: String,
    val description: String? = null,
    val type: ToolType,
    val parameters: List<ToolParameter> = emptyList(),
    val httpConfig: HttpEndpointConfig? = null,
    val jdbcConfig: JdbcQueryConfig? = null
)
