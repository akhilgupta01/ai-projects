package com.agents.qaagent

import com.agents.qaagent.model.ReportableAttribute
import com.agents.qaagent.service.QaAgentService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.genai.Client
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile

/**
 * Unit tests for [QaAgentService].
 *
 * These tests validate the JSON parsing, response assembly and edge-case
 * handling logic *without* making real Vertex AI calls.
 */
class QaAgentServiceTest {

    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    // ─── JSON parsing ─────────────────────────────────────────────────────────

    @Test
    fun `extractJsonArray strips markdown code fence`() {
        val service = qaAgentServiceWithFixedResponse(
            """
            ```json
            [{"name":"trade_date","description":"Trade date","dataType":"Date","mandatory":true}]
            ```
            """.trimIndent()
        )

        val mockFile = pdfMockFile("trade_date\nTrade date\nDate\ntrue")
        val result = service.analyzeDocument(mockFile)

        assertEquals(1, result.totalAttributes)
        assertEquals("trade_date", result.attributes.first().name)
        assertTrue(result.attributes.first().mandatory)
    }

    @Test
    fun `returns empty list when agent returns invalid JSON`() {
        val service = qaAgentServiceWithFixedResponse("This is not JSON at all.")

        val mockFile = pdfMockFile("bad content")
        val result = service.analyzeDocument(mockFile)

        assertEquals(0, result.totalAttributes)
        assertEquals(emptyList<ReportableAttribute>(), result.attributes)
    }

    @Test
    fun `response counts mandatory and optional attributes correctly`() {
        val json = """
            [
              {"name":"a","description":"desc a","dataType":"String","mandatory":true},
              {"name":"b","description":"desc b","dataType":"Date","mandatory":false},
              {"name":"c","description":"desc c","dataType":"Decimal","mandatory":true}
            ]
        """.trimIndent()
        val service = qaAgentServiceWithFixedResponse(json)

        val mockFile = pdfMockFile("some pdf content")
        val result = service.analyzeDocument(mockFile)

        assertEquals(3, result.totalAttributes)
        assertEquals(2, result.mandatoryCount)
        assertEquals(1, result.optionalCount)
    }

    @Test
    fun `document name is preserved in response`() {
        val service = qaAgentServiceWithFixedResponse("[]")

        val mockFile = MockMultipartFile(
            "file",
            "EMIR_Refit_Spec.pdf",
            "application/pdf",
            ByteArray(0)
        )

        val result = service.analyzeDocument(mockFile)
        assertEquals("EMIR_Refit_Spec.pdf", result.documentName)
    }

    @Test
    fun `optional fields are nullable in ReportableAttribute`() {
        val attr = ReportableAttribute(
            name = "notional_amount",
            description = "Notional amount of the contract",
            dataType = "Decimal",
            mandatory = true
        )
        assertNotNull(attr)
        assertEquals(null, attr.format)
        assertEquals(null, attr.source)
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Creates a [QaAgentService] whose [buildQaAgent] call is short-circuited
     * to return the given [fixedResponse] instead of calling Vertex AI.
     *
     * This works by subclassing [QaAgentService] and overriding [analyzeDocument]
     * to inject a mock [MultipartFile] that does not need a real PDF on disk.
     */
    private fun qaAgentServiceWithFixedResponse(fixedResponse: String): TestableQaAgentService {
        val mockClient = mockk<Client>()
        return TestableQaAgentService(
            genAiClient = mockClient,
            objectMapper = objectMapper,
            modelName = "gemini-2.0-flash",
            fixedAgentResponse = fixedResponse
        )
    }

    private fun pdfMockFile(content: String) = MockMultipartFile(
        "file",
        "test.pdf",
        "application/pdf",
        content.toByteArray()
    )
}

/**
 * Testable subclass of [QaAgentService] that bypasses the real agent call and
 * instead returns a predetermined response string.  This lets us test the
 * parsing and response-assembly logic in isolation.
 */
class TestableQaAgentService(
    genAiClient: Client,
    objectMapper: ObjectMapper,
    modelName: String,
    private val fixedAgentResponse: String
) : QaAgentService(genAiClient, objectMapper, modelName) {

    override fun runAgent(pdfPath: String): String = fixedAgentResponse
}
