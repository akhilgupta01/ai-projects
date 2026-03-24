package com.chatbot.service

import com.chatbot.model.*
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service
class ChatService {
    
    private val sessions = ConcurrentHashMap<String, ChatSession>()
    
    fun createSession(title: String?): ChatSession {
        val session = ChatSession(
            title = title ?: "New Chat"
        )
        sessions[session.id] = session
        return session
    }
    
    fun getAllSessions(): List<SessionResponse> {
        return sessions.values
            .sortedByDescending { it.createdAt }
            .map { session ->
                SessionResponse(
                    id = session.id,
                    title = session.title,
                    createdAt = session.createdAt,
                    messageCount = session.messages.size
                )
            }
    }
    
    fun getSession(sessionId: String): SessionDetailResponse? {
        val session = sessions[sessionId] ?: return null
        return SessionDetailResponse(
            id = session.id,
            title = session.title,
            createdAt = session.createdAt,
            messages = session.messages.toList()
        )
    }
    
    fun deleteSession(sessionId: String): Boolean {
        return sessions.remove(sessionId) != null
    }
    
    fun sendMessage(sessionId: String, content: String): MessageResponse? {
        val session = sessions[sessionId] ?: return null
        
        val userMessage = ChatMessage(
            sessionId = sessionId,
            role = MessageRole.USER,
            content = content
        )
        session.messages.add(userMessage)
        
        val assistantResponse = generateBotResponse(content, session.messages)
        val assistantMessage = ChatMessage(
            sessionId = sessionId,
            role = MessageRole.ASSISTANT,
            content = assistantResponse
        )
        session.messages.add(assistantMessage)
        
        if (session.messages.size == 2 && session.title == "New Chat") {
            val updatedSession = session.copy(
                title = content.take(50) + if (content.length > 50) "..." else ""
            )
            sessions[sessionId] = updatedSession.copy(messages = session.messages)
        }
        
        return MessageResponse(
            userMessage = userMessage,
            assistantMessage = assistantMessage
        )
    }
    
    private fun generateBotResponse(userMessage: String, conversationHistory: List<ChatMessage>): String {
        val lowerMessage = userMessage.lowercase()
        
        return when {
            lowerMessage.contains("hello") || lowerMessage.contains("hi") || lowerMessage.contains("hey") ->
                "Hello! I'm your AI Operational Support Assistant. How can I help you today? I can assist with troubleshooting, system status inquiries, operational procedures, and general support questions."
            
            lowerMessage.contains("help") ->
                "I'm here to help! As your Operational Support Assistant, I can assist you with:\n\n" +
                "1. **System Status** - Check the status of various systems and services\n" +
                "2. **Troubleshooting** - Help diagnose and resolve common issues\n" +
                "3. **Procedures** - Guide you through operational procedures\n" +
                "4. **Documentation** - Provide information about processes and policies\n\n" +
                "What would you like assistance with?"
            
            lowerMessage.contains("status") || lowerMessage.contains("system") ->
                "All systems are currently operational. Here's a quick status overview:\n\n" +
                "- **Primary Services**: Online (99.9% uptime)\n" +
                "- **Database Cluster**: Healthy\n" +
                "- **API Gateway**: Responding normally\n" +
                "- **Background Jobs**: Processing as expected\n\n" +
                "Is there a specific system you'd like more details about?"
            
            lowerMessage.contains("error") || lowerMessage.contains("issue") || lowerMessage.contains("problem") ->
                "I understand you're experiencing an issue. To help you better, could you please provide:\n\n" +
                "1. A description of the problem you're encountering\n" +
                "2. Any error messages you're seeing\n" +
                "3. When the issue started occurring\n" +
                "4. Steps you've already tried to resolve it\n\n" +
                "With this information, I can provide more targeted assistance."
            
            lowerMessage.contains("restart") || lowerMessage.contains("reboot") ->
                "For restart procedures, please follow these steps:\n\n" +
                "1. Ensure all critical processes are saved or completed\n" +
                "2. Notify affected users if applicable\n" +
                "3. Follow the standard restart procedure for the specific service\n" +
                "4. Monitor the service after restart to confirm normal operation\n\n" +
                "Would you like specific instructions for a particular service?"
            
            lowerMessage.contains("log") || lowerMessage.contains("logs") ->
                "To access and analyze logs:\n\n" +
                "1. **Application Logs**: Available in the centralized logging system\n" +
                "2. **System Logs**: Check /var/log for system-level events\n" +
                "3. **Audit Logs**: Available through the compliance dashboard\n\n" +
                "What type of logs are you looking for? I can help you locate and interpret them."
            
            lowerMessage.contains("thank") ->
                "You're welcome! I'm glad I could help. If you have any more questions or need further assistance, feel free to ask. I'm here to support your operational needs."
            
            lowerMessage.contains("bye") || lowerMessage.contains("goodbye") ->
                "Goodbye! If you need any assistance in the future, don't hesitate to start a new chat. Have a great day!"
            
            else ->
                "Thank you for your message. As your Operational Support Assistant, I'm here to help with system-related inquiries, troubleshooting, and operational procedures.\n\n" +
                "Could you please provide more details about what you need assistance with? For example:\n" +
                "- Are you experiencing a specific issue?\n" +
                "- Do you need information about a system or process?\n" +
                "- Are you looking for guidance on a particular procedure?\n\n" +
                "I'll do my best to assist you!"
        }
    }
}
