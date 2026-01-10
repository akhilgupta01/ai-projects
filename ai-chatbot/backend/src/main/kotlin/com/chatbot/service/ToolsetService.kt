package com.chatbot.service

import com.chatbot.model.*
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class ToolsetService {
    
    private val toolsets = ConcurrentHashMap<String, Toolset>()
    
    fun createToolset(request: CreateToolsetRequest): Toolset {
        val toolset = Toolset(
            name = request.name,
            description = request.description
        )
        toolsets[toolset.id] = toolset
        return toolset
    }
    
    fun getAllToolsets(): List<ToolsetResponse> {
        return toolsets.values
            .sortedByDescending { it.createdAt }
            .map { toolset ->
                ToolsetResponse(
                    id = toolset.id,
                    name = toolset.name,
                    description = toolset.description,
                    toolCount = toolset.tools.size,
                    createdAt = toolset.createdAt
                )
            }
    }
    
    fun getToolset(id: String): Toolset? {
        return toolsets[id]
    }
    
    fun deleteToolset(id: String): Boolean {
        return toolsets.remove(id) != null
    }
    
    fun addToolToToolset(toolsetId: String, request: CreateToolRequest): Tool? {
        val toolset = toolsets[toolsetId] ?: return null
        
        val tool = Tool(
            name = request.name,
            description = request.description,
            type = request.type,
            parameters = request.parameters,
            httpConfig = request.httpConfig,
            jdbcConfig = request.jdbcConfig
        )
        
        toolset.tools.add(tool)
        return tool
    }
    
    fun getToolsInToolset(toolsetId: String): List<Tool>? {
        val toolset = toolsets[toolsetId] ?: return null
        return toolset.tools.sortedByDescending { it.createdAt }
    }
    
    fun getTool(toolsetId: String, toolId: String): Tool? {
        val toolset = toolsets[toolsetId] ?: return null
        return toolset.tools.find { it.id == toolId }
    }
    
    fun deleteToolFromToolset(toolsetId: String, toolId: String): Boolean {
        val toolset = toolsets[toolsetId] ?: return false
        return toolset.tools.removeIf { it.id == toolId }
    }
    
    fun updateToolset(id: String, request: CreateToolsetRequest): Toolset? {
        val existing = toolsets[id] ?: return null
        val updated = existing.copy(
            name = request.name,
            description = request.description
        )
        toolsets[id] = updated
        return updated
    }
}
