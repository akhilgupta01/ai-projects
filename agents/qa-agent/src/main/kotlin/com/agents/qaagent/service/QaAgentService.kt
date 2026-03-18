package com.agents.qaagent.service

import com.agents.qaagent.agent.buildQaAgent
import com.agents.qaagent.agent.buildReviewAgent
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
 * 3. Runs the extraction agent with a prompt that includes the PDF path.
 * 4. Runs up to [reviewReworkCycles] review passes, each of which refines the
 *    extracted attributes (resolving section references, filling missing rules, etc.).
 * 5. Parses the final JSON array of [ReportableAttribute] objects.
 * 6. Assembles and returns an [AnalyzeResponse].
 *
 * The temporary file is always cleaned up after processing.
 */
@Service
open class QaAgentService(
    private val genAiClient: Client,
    private val objectMapper: ObjectMapper,
    @Value("\${vertex.ai.model:gemini-2.0-flash}") private val modelName: String,
    @Value("\${agent.review.rework.cycles:1}") private val reviewReworkCycles: Int = 1
) {

    private val log = LoggerFactory.getLogger(QaAgentService::class.java)

    /**
     * Analyze a regulatory reporting specification PDF and return all
     * reportable attributes found within it.
     *
     * The extraction is followed by [reviewReworkCycles] review-and-rework
     * passes that refine the result before it is returned to the caller.
     *
     * @param file PDF file uploaded via multipart request.
     * @return [AnalyzeResponse] containing the extracted and refined attributes.
     */
    fun analyzeDocument(file: MultipartFile): AnalyzeResponse {
        val documentName = file.originalFilename ?: "document.pdf"
        log.info("Starting QA analysis for document: {}", documentName)

        val tmpFile = Files.createTempFile("qa-agent-", ".pdf").toFile()
        try {
            file.transferTo(tmpFile)
            log.debug("Saved uploaded file to: {}", tmpFile.absolutePath)

            // Step 1: initial extraction
            var rawResponse = runAgent(tmpFile.absolutePath)
            log.debug("Agent raw response (initial): {}", rawResponse)

            // Step 2: review-rework cycles
            repeat(reviewReworkCycles) { cycle ->
                log.info("Starting review-rework cycle {}/{}", cycle + 1, reviewReworkCycles)
                rawResponse = runReviewAgent(tmpFile.absolutePath, rawResponse)
                log.debug("Agent raw response (review cycle {}): {}", cycle + 1, rawResponse)
            }

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
     * Build and run the extraction koog agent against the PDF at [pdfPath].
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

    /**
     * Build and run the review koog agent for one review-rework cycle.
     *
     * The review agent may re-read the document via the PDF tool to resolve
     * section cross-references, then returns a refined JSON array.
     *
     * Extracted into its own open method so that tests can override it without
     * requiring a live Vertex AI endpoint.
     *
     * @param pdfPath      Path to the original regulatory document.
     * @param currentJson  The JSON array string produced by the previous pass.
     * @return             Refined JSON array string.
     */
    open fun runReviewAgent(pdfPath: String, currentJson: String): String {
        val agent = buildReviewAgent(genAiClient, modelName)
        val userPrompt = """
            Please review and refine the following extracted reportable attributes
            from the regulatory reporting specification located at: $pdfPath

            Use the extract_pdf_text tool to re-read the document and resolve any
            section cross-references, verify completeness of rules, and correct
            any inaccuracies in applicableReportTypes and applicableAssetClasses.

            Current extraction:
            $currentJson

            Return the refined JSON array.
        """.trimIndent()
        return runBlocking { agent.run(userPrompt) } ?: currentJson
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
