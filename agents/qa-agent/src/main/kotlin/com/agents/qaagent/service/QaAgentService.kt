package com.agents.qaagent.service

import com.agents.qaagent.agent.buildQaAgent
import com.agents.qaagent.agent.buildReviewAgent
import com.agents.qaagent.model.AnalyzeResponse
import com.agents.qaagent.model.ReportableAttribute
import com.agents.qaagent.service.AttributePersistenceService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.genai.Client
import com.google.genai.types.Content
import com.google.genai.types.CreateCachedContentConfig
import com.google.genai.types.FileData
import com.google.genai.types.Part
import com.google.genai.types.UploadFileConfig
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files

/**
 * Service that orchestrates the QA agentic workflow:
 *
 * 1. Saves the uploaded PDF to a temporary file.
 * 2. Uploads the PDF to Vertex AI using the GenAI file API and creates a cached
 *    content handle so the binary can be reused across prompt turns.
 * 3. Builds a koog [AIAgent] backed by Vertex AI Gemini (via the GenAI SDK).
 * 4. Runs the extraction agent with the cached binary PDF attached.
 * 5. Runs up to [reviewReworkCycles] review passes, reusing the same cached PDF
 *    to refine the extracted attributes (resolve section references, fill missing rules, etc.).
 * 6. Parses the final JSON array of [ReportableAttribute] objects.
 * 7. Assembles and returns an [AnalyzeResponse].
 *
 * The temporary file is always cleaned up after processing.
 */
@Service
open class QaAgentService(
    private val genAiClient: Client,
    private val objectMapper: ObjectMapper,
    @Value("\${vertex.ai.model:gemini-2.0-flash}") private val modelName: String,
    @Value("\${agent.review.rework.cycles:1}") private val reviewReworkCycles: Int = 1,
    private val attributePersistenceService: AttributePersistenceService,
    @Value("\${qa.default-jurisdiction:GLOBAL}") private val defaultJurisdiction: String = "GLOBAL"
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
     * @param jurisdiction Optional jurisdiction to associate with persisted attributes.
     * @return [AnalyzeResponse] containing the extracted and refined attributes.
     */
    fun analyzeDocument(file: MultipartFile, jurisdiction: String? = null): AnalyzeResponse {
        val documentName = file.originalFilename ?: "document.pdf"
        log.info("Starting QA analysis for document: {}", documentName)

        val tmpFile = Files.createTempFile("qa-agent-", ".pdf").toFile()
        try {
            file.transferTo(tmpFile)
            log.debug("Saved uploaded file to: {}", tmpFile.absolutePath)

            val cachedContentName = cacheDocument(tmpFile, documentName)

            // Step 1: initial extraction
            var rawResponse = runAgent(tmpFile.absolutePath, cachedContentName)
            log.debug("Agent raw response (initial): {}", rawResponse)

            // Step 2: review-rework cycles
            repeat(reviewReworkCycles) { cycle ->
                log.info("Starting review-rework cycle {}/{}", cycle + 1, reviewReworkCycles)
                rawResponse = runReviewAgent(tmpFile.absolutePath, cachedContentName, rawResponse)
                log.debug("Agent raw response (review cycle {}): {}", cycle + 1, rawResponse)
            }

            val attributes = parseAttributes(rawResponse)
            log.info("Extracted {} reportable attributes from {}", attributes.size, documentName)

            val resolvedJurisdiction = jurisdiction?.takeUnless { it.isBlank() } ?: defaultJurisdiction
            attributePersistenceService.saveAttributes(resolvedJurisdiction, attributes)
            log.info(
                "Persisted {} attributes for jurisdiction {}",
                attributes.size,
                resolvedJurisdiction
            )

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
    open fun runAgent(pdfPath: String, cachedContentName: String): String {
        val agent = buildQaAgent(genAiClient, modelName, cachedContentName)
        val userPrompt = """
            Please extract all reportable attributes from the regulatory
            reporting specification located at: $pdfPath.

            The PDF is already attached to the conversation context (binary,
            not text-extracted). Use that attachment directly—do not try to
            re-extract or summarize the document yourself. Identify and return
            every reportable attribute as a JSON array.
        """.trimIndent()
        return runBlocking { agent.runAndGetResult(userPrompt) } ?: "[]"
    }

    /**
     * Build and run the review koog agent for one review-rework cycle.
     *
     * The review agent re-reads the cached binary document to resolve section
     * cross-references, then returns a refined JSON array.
     *
     * Extracted into its own open method so that tests can override it without
     * requiring a live Vertex AI endpoint.
     *
     * @param pdfPath      Path to the original regulatory document.
     * @param currentJson  The JSON array string produced by the previous pass.
     * @return             Refined JSON array string.
     */
    open fun runReviewAgent(pdfPath: String, cachedContentName: String, currentJson: String): String {
        val agent = buildReviewAgent(genAiClient, modelName, cachedContentName)
        val userPrompt = """
            Please review and refine the following extracted reportable attributes
            from the regulatory reporting specification located at: $pdfPath.

            The original PDF is already attached to the conversation context as
            binary cached content. Re-read it directly to resolve any
            section cross-references, verify completeness of rules, and correct
            any inaccuracies in applicableReportTypes and applicableAssetClasses.

            Current extraction:
            $currentJson

            Return the refined JSON array.
        """.trimIndent()
        return runBlocking { agent.runAndGetResult(userPrompt) } ?: currentJson
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

    /**
     * Upload the PDF to Vertex AI via the GenAI SDK and create a cached content
     * handle that can be reused across extraction and review cycles without
     * re-sending the binary.
     */
    protected open fun cacheDocument(file: File, displayName: String): String {
        val uploadConfig = UploadFileConfig.builder()
            .displayName(displayName)
            .mimeType("application/pdf")
            .build()

        val uploaded = genAiClient.files.upload(file, uploadConfig)
        val fileUri = uploaded.uri().orElseThrow {
            IllegalStateException("Uploaded file URI was not returned by GenAI for file: $displayName")
        }

        val fileData = FileData.builder()
            .fileUri(fileUri)
            .displayName(uploaded.displayName().orElse(displayName))
            .mimeType(uploaded.mimeType().orElse("application/pdf"))
            .build()

        val cached = genAiClient.caches.create(
            modelName,
            CreateCachedContentConfig.builder()
                .contents(
                    listOf(
                        Content.builder()
                            .role("user")
                            .parts(
                                listOf(
                                    Part.builder()
                                        .fileData(fileData)
                                        .build()
                                )
                            )
                            .build()
                    )
                )
                .build()
        )

        return cached.name().orElseThrow {
            IllegalStateException("Cached content name was not returned by GenAI for file: $displayName")
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
