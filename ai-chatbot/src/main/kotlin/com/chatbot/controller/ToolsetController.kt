package com.chatbot.controller

import com.chatbot.model.*
import com.chatbot.service.ToolsetService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/toolsets")
class ToolsetController(private val toolsetService: ToolsetService) {
    
    @PostMapping
    fun createToolset(@RequestBody request: CreateToolsetRequest): ResponseEntity<Toolset> {
        if (request.name.isBlank()) {
            return ResponseEntity.badRequest().build()
        }
        val toolset = toolsetService.createToolset(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(toolset)
    }
    
    @GetMapping
    fun getAllToolsets(): ResponseEntity<List<ToolsetResponse>> {
        val toolsets = toolsetService.getAllToolsets()
        return ResponseEntity.ok(toolsets)
    }
    
    @GetMapping("/{id}")
    fun getToolset(@PathVariable id: String): ResponseEntity<Toolset> {
        val toolset = toolsetService.getToolset(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(toolset)
    }
    
    @PutMapping("/{id}")
    fun updateToolset(@PathVariable id: String, @RequestBody request: CreateToolsetRequest): ResponseEntity<Toolset> {
        if (request.name.isBlank()) {
            return ResponseEntity.badRequest().build()
        }
        val toolset = toolsetService.updateToolset(id, request)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(toolset)
    }
    
    @DeleteMapping("/{id}")
    fun deleteToolset(@PathVariable id: String): ResponseEntity<Void> {
        val deleted = toolsetService.deleteToolset(id)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @PostMapping("/{toolsetId}/tools")
    fun addToolToToolset(
        @PathVariable toolsetId: String,
        @RequestBody request: CreateToolRequest
    ): ResponseEntity<Tool> {
        if (request.name.isBlank()) {
            return ResponseEntity.badRequest().build()
        }
        val tool = toolsetService.addToolToToolset(toolsetId, request)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.status(HttpStatus.CREATED).body(tool)
    }
    
    @GetMapping("/{toolsetId}/tools")
    fun getToolsInToolset(@PathVariable toolsetId: String): ResponseEntity<List<Tool>> {
        val tools = toolsetService.getToolsInToolset(toolsetId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(tools)
    }
    
    @GetMapping("/{toolsetId}/tools/{toolId}")
    fun getTool(
        @PathVariable toolsetId: String,
        @PathVariable toolId: String
    ): ResponseEntity<Tool> {
        val tool = toolsetService.getTool(toolsetId, toolId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(tool)
    }
    
    @DeleteMapping("/{toolsetId}/tools/{toolId}")
    fun deleteToolFromToolset(
        @PathVariable toolsetId: String,
        @PathVariable toolId: String
    ): ResponseEntity<Void> {
        val deleted = toolsetService.deleteToolFromToolset(toolsetId, toolId)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
