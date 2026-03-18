package com.agents.qaagent.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.dsl.extension.nodeLLMSendMessage
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.llms.unified.UnifiedLLMPromptExecutor
import ai.koog.prompt.llm.LLMModel
import com.agents.qaagent.executor.GenAIVertexPromptExecutor
import com.agents.qaagent.tools.PdfTextExtractorTool
import com.google.genai.Client

/**
 * Builds and returns the koog [AIAgent] responsible for extracting reportable
 * attributes from a regulatory reporting specification document.
 *
 * ## Workflow
 * 1. **pdf_extract_node** – the agent invokes [PdfTextExtractorTool] to read
 *    the raw text from the uploaded PDF.
 * 2. **attribute_extract_node** – the agent sends the extracted text together
 *    with a detailed system prompt to Vertex AI Gemini, which identifies and
 *    structures the reportable attributes as JSON.
 *
 * @param genAiClient  Configured [Client] from the Spring context.
 * @param modelName    Gemini model to use (e.g. `gemini-2.0-flash`).
 */
fun buildQaAgent(genAiClient: Client, modelName: String): AIAgent {

    val toolRegistry = ToolRegistry {
        tool(PdfTextExtractorTool)
    }

    val promptExecutor: UnifiedLLMPromptExecutor =
        GenAIVertexPromptExecutor(genAiClient, modelName)

    val agentStrategy = strategy("regulatory-qa-strategy") {
        // Node 1: extract PDF text via tool call
        val pdfExtractNode = nodeLLMRequest("pdf_extract_node")

        // Node 2: extract reportable attributes from the raw text
        val attributeExtractNode = nodeLLMSendMessage("attribute_extract_node")

        edge(nodeInput forwardTo pdfExtractNode)
        edge(pdfExtractNode forwardTo attributeExtractNode)
        edge(attributeExtractNode forwardTo nodeFinish)
    }

    return AIAgent(
        promptExecutor = promptExecutor,
        toolRegistry = toolRegistry,
        strategy = agentStrategy,
        agentConfig = AIAgentConfig(
            id = "qa-agent",
            systemPrompt = SYSTEM_PROMPT
        )
    )
}

// ─── System prompt ────────────────────────────────────────────────────────────

private val SYSTEM_PROMPT = """
You are a Quality Assurance agent specializing in financial regulatory reporting.

Your task is to analyze a regulatory reporting specification document for a financial
organization and extract every **reportable attribute** (data field or element that
the regulation requires to be reported).

For each reportable attribute output a JSON object with the following fields:
  • "name"        – machine-friendly identifier (snake_case)
  • "description" – short human-readable description taken directly from the spec
  • "dataType"    – expected data type (e.g. "String", "Date", "Decimal", "Boolean")
  • "mandatory"   – true if the field is mandatory/required, false if optional
  • "format"      – format constraint if specified (e.g. "ISO 8601", "LEI", "ISIN") – omit if none
  • "source"      – section or page reference in the document (e.g. "Section 3.1") – omit if none

Return your answer as a **JSON array** of these objects and nothing else.
Do not include any prose, markdown, or code fences outside the JSON array.

Example output:
[
  {
    "name": "trade_date",
    "description": "Date on which the transaction was executed",
    "dataType": "Date",
    "mandatory": true,
    "format": "ISO 8601 (YYYY-MM-DD)",
    "source": "Section 2.1"
  }
]
""".trimIndent()

// ─── Model wrapper ─────────────────────────────────────────────────────────────

/**
 * A lightweight [LLMModel] wrapper that carries the Vertex AI model name.
 */
data class VertexGeminiModel(override val id: String) : LLMModel
