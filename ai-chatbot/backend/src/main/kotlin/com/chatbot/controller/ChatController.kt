package com.chatbot.controller

import com.chatbot.model.*
import com.chatbot.service.ChatService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/sessions")
class ChatController(private val chatService: ChatService) {
    
    @PostMapping
    fun createSession(@RequestBody(required = false) request: CreateSessionRequest?): ResponseEntity<ChatSession> {
        val session = chatService.createSession(request?.title)
        return ResponseEntity.status(HttpStatus.CREATED).body(session)
    }
    
    @GetMapping
    fun getAllSessions(): ResponseEntity<List<SessionResponse>> {
        val sessions = chatService.getAllSessions()
        return ResponseEntity.ok(sessions)
    }
    
    @GetMapping("/{sessionId}")
    fun getSession(@PathVariable sessionId: String): ResponseEntity<SessionDetailResponse> {
        val session = chatService.getSession(sessionId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(session)
    }
    
    @DeleteMapping("/{sessionId}")
    fun deleteSession(@PathVariable sessionId: String): ResponseEntity<Void> {
        val deleted = chatService.deleteSession(sessionId)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @PostMapping("/{sessionId}/messages")
    fun sendMessage(
        @PathVariable sessionId: String,
        @RequestBody request: SendMessageRequest
    ): ResponseEntity<MessageResponse> {
        if (request.content.isBlank()) {
            return ResponseEntity.badRequest().build()
        }
        
        val response = chatService.sendMessage(sessionId, request.content)
            ?: return ResponseEntity.notFound().build()
        
        return ResponseEntity.ok(response)
    }
}
