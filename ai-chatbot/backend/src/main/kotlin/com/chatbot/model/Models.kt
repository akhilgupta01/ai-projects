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
