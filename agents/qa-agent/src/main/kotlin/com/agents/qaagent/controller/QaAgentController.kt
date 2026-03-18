package com.agents.qaagent.controller

import com.agents.qaagent.model.AnalyzeResponse
import com.agents.qaagent.service.QaAgentService
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * REST controller exposing the QA agent API.
 *
 * ## Endpoints
 *
 * ### POST /api/qa/analyze
 * Accepts a multipart PDF upload and returns all reportable attributes
 * extracted from the regulatory reporting specification.
 *
 * #### Request
 * ```
 * Content-Type: multipart/form-data
 * file: <binary PDF data>
 * ```
 *
 * #### Response  200 OK
 * ```json
 * {
 *   "documentName": "EMIR_Refit_Spec.pdf",
 *   "totalAttributes": 42,
 *   "mandatoryCount": 35,
 *   "optionalCount": 7,
 *   "attributes": [
 *     {
 *       "name": "trade_date",
 *       "description": "Date on which the transaction was executed",
 *       "dataType": "Date",
 *       "mandatory": true,
 *       "format": "ISO 8601 (YYYY-MM-DD)",
 *       "source": "Section 2.1"
 *     }
 *   ]
 * }
 * ```
 */
@RestController
@RequestMapping("/api/qa")
class QaAgentController(private val qaAgentService: QaAgentService) {

    private val log = LoggerFactory.getLogger(QaAgentController::class.java)

    /**
     * Analyze a regulatory reporting specification PDF and extract its
     * reportable attributes.
     *
     * @param file The PDF file uploaded as `multipart/form-data`.
     * @return [AnalyzeResponse] with all extracted attributes, or 400 if
     *         the uploaded file is not a PDF.
     */
    @PostMapping(
        "/analyze",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun analyze(
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<AnalyzeResponse> {
        val filename = file.originalFilename ?: ""

        if (!filename.endsWith(".pdf", ignoreCase = true) &&
            file.contentType != "application/pdf"
        ) {
            log.warn("Rejected upload with name='{}' contentType='{}'", filename, file.contentType)
            return ResponseEntity.badRequest().build()
        }

        log.info("Received analyze request for file: {}", filename)
        val response = qaAgentService.analyzeDocument(file)
        return ResponseEntity.ok(response)
    }
}
