package com.chatbot.controller

import com.chatbot.model.*
import com.chatbot.service.DocumentService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/documents")
class DocumentController(private val documentService: DocumentService) {
    
    @PostMapping
    fun uploadDocument(@RequestBody request: UploadDocumentRequest): ResponseEntity<KnowledgeDocument> {
        if (request.name.isBlank() || request.content.isBlank()) {
            return ResponseEntity.badRequest().build()
        }
        val document = documentService.uploadDocument(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(document)
    }
    
    @GetMapping
    fun getAllDocuments(): ResponseEntity<List<DocumentResponse>> {
        val documents = documentService.getAllDocuments()
        return ResponseEntity.ok(documents)
    }
    
    @GetMapping("/{documentId}")
    fun getDocument(@PathVariable documentId: String): ResponseEntity<KnowledgeDocument> {
        val document = documentService.getDocument(documentId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(document)
    }
    
    @DeleteMapping("/{documentId}")
    fun deleteDocument(@PathVariable documentId: String): ResponseEntity<Void> {
        val deleted = documentService.deleteDocument(documentId)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @PostMapping("/search")
    fun searchDocuments(@RequestBody request: SearchDocumentsRequest): ResponseEntity<List<DocumentResponse>> {
        if (request.query.isBlank()) {
            return ResponseEntity.badRequest().build()
        }
        val documents = documentService.searchDocuments(request.query, request.tags)
        return ResponseEntity.ok(documents)
    }
}
