package com.agents.qaagent.service

import com.agents.qaagent.agent.buildQaAgent
import com.agents.qaagent.model.AnalyzeResponse
import com.agents.qaagent.model.ReportableAttribute
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.genai.Client
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files

/**
 * Service that orchestrates the QA agentic workflow:
 *
 * 1. Saves the uploaded PDF to a temporary file.
 * 2. Builds a koog [AIAgent] backed by Vertex AI Gemini (via the GenAI SDK).
 * 3. Runs the agent with a prompt that includes the PDF path.
 * 4. Parses the JSON array of [ReportableAttribute] objects from the agent's response.
 * 5. Assembles and returns an [AnalyzeResponse].
 *
 * The temporary file is always cleaned up after processing.
 */
@Service
open class QaAgentService(
    private val genAiClient: Client,
    private val objectMapper: ObjectMapper,
    @Value("\${vertex.ai.model:gemini-2.0-flash}") private val modelName: String
) {

    private val log = LoggerFactory.getLogger(QaAgentService::class.java)

    /**
     * Analyze a regulatory reporting specification PDF and return all
     * reportable attributes found within it.
     *
     * @param file PDF file uploaded via multipart request.
     * @return [AnalyzeResponse] containing the extracted attributes.
     */
    fun analyzeDocument(file: MultipartFile): AnalyzeResponse {
        val documentName = file.originalFilename ?: "document.pdf"
        log.info("Starting QA analysis for document: {}", documentName)

        val tmpFile = Files.createTempFile("qa-agent-", ".pdf").toFile()
        try {
            file.transferTo(tmpFile)
            log.debug("Saved uploaded file to: {}", tmpFile.absolutePath)

            val rawResponse = runAgent(tmpFile.absolutePath)
            log.debug("Agent raw response: {}", rawResponse)

            val attributes = parseAttributes(rawResponse)
            log.info("Extracted {} reportable attributes from {}", attributes.size, documentName)

            return AnalyzeResponse(
                documentName = documentName,
                totalAttributes = attributes.size,
                mandatoryCount = attributes.count { it.mandatory },
                optionalCount = attributes.count { !it.mandatory },
                attributes = attributes
            )
        } finally {
            tmpFile.delete()
            log.debug("Deleted temporary file: {}", tmpFile.absolutePath)
        }
    }

    /**
     * Build and run the koog QA agent against the PDF at [pdfPath].
     *
     * Extracted into its own open method so that tests can override it without
     * requiring a live Vertex AI endpoint.
     */
    open fun runAgent(pdfPath: String): String {
        val agent = buildQaAgent(genAiClient, modelName)
        val userPrompt = """
            Please extract all reportable attributes from the regulatory
            reporting specification located at: $pdfPath

            Use the extract_pdf_text tool to read the document first, then
            identify and return every reportable attribute as a JSON array.
        """.trimIndent()
        return runBlocking { agent.run(userPrompt) } ?: "[]"
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Parse the JSON array string returned by the agent into a list of
     * [ReportableAttribute] objects.
     *
     * The function tolerates responses that wrap the JSON array in a markdown
     * code fence (```json ... ```) because the model occasionally adds one.
     */
    private fun parseAttributes(raw: String): List<ReportableAttribute> {
        val json = extractJsonArray(raw.trim())
        return try {
            objectMapper.readValue(json)
        } catch (e: Exception) {
            log.error("Failed to parse agent response as JSON: {}", json, e)
            emptyList()
        }
    }

    private fun extractJsonArray(text: String): String {
        // Strip optional markdown code fences
        val stripped = text
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        // Find the JSON array boundaries
        val start = stripped.indexOf('[')
        val end = stripped.lastIndexOf(']')
        return if (start != -1 && end != -1 && end > start) {
            stripped.substring(start, end + 1)
        } else {
            "[]"
        }
    }
}
