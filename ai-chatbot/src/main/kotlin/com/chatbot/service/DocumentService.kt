package com.chatbot.service

import com.chatbot.model.*
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class DocumentService {
    
    private val documents = ConcurrentHashMap<String, KnowledgeDocument>()
    
    fun uploadDocument(request: UploadDocumentRequest): KnowledgeDocument {
        val document = KnowledgeDocument(
            name = request.name,
            content = request.content,
            contentType = request.contentType,
            size = request.content.length.toLong(),
            tags = request.tags ?: emptyList()
        )
        documents[document.id] = document
        return document
    }
    
    fun getAllDocuments(): List<DocumentResponse> {
        return documents.values
            .sortedByDescending { it.uploadedAt }
            .map { doc ->
                DocumentResponse(
                    id = doc.id,
                    name = doc.name,
                    contentType = doc.contentType,
                    size = doc.size,
                    uploadedAt = doc.uploadedAt,
                    tags = doc.tags
                )
            }
    }
    
    fun getDocument(documentId: String): KnowledgeDocument? {
        return documents[documentId]
    }
    
    fun deleteDocument(documentId: String): Boolean {
        return documents.remove(documentId) != null
    }
    
    fun searchDocuments(query: String, tags: List<String>?): List<DocumentResponse> {
        val lowerQuery = query.lowercase()
        
        return documents.values
            .filter { doc ->
                val matchesQuery = doc.name.lowercase().contains(lowerQuery) ||
                        doc.content.lowercase().contains(lowerQuery) ||
                        doc.tags.any { it.lowercase().contains(lowerQuery) }
                
                val matchesTags = tags.isNullOrEmpty() || 
                        tags.any { tag -> doc.tags.any { it.lowercase() == tag.lowercase() } }
                
                matchesQuery && matchesTags
            }
            .sortedByDescending { it.uploadedAt }
            .map { doc ->
                DocumentResponse(
                    id = doc.id,
                    name = doc.name,
                    contentType = doc.contentType,
                    size = doc.size,
                    uploadedAt = doc.uploadedAt,
                    tags = doc.tags
                )
            }
    }
}
