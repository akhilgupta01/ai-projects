package com.agents.qaagent

import com.agents.qaagent.model.AttributeRule
import com.agents.qaagent.model.ReportableAttribute
import com.agents.qaagent.service.AttributePersistenceService
import com.agents.qaagent.service.QaAgentService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.genai.Client
import io.mockk.mockk
import io.mockk.verify
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
    fun `persists attributes with resolved jurisdiction`() {
        val mockClient = mockk<Client>()
        val persistence = mockk<AttributePersistenceService>(relaxed = true)
        val service = object : QaAgentService(
            genAiClient = mockClient,
            objectMapper = objectMapper,
            modelName = "gemini-2.0-flash",
            reviewReworkCycles = 0,
            attributePersistenceService = persistence,
            defaultJurisdiction = "EU"
        ) {
            override fun cacheDocument(file: java.io.File, displayName: String): String = "cache-id"
            override fun runAgent(pdfPath: String, cachedContentName: String): String =
                """[{"name":"trade_date","description":"Trade date","dataType":"Date","mandatory":true}]"""
            override fun runReviewAgent(
                pdfPath: String,
                cachedContentName: String,
                currentJson: String
            ): String = currentJson
        }

        service.analyzeDocument(pdfMockFile("content"))

        verify {
            persistence.saveAttributes(
                "EU",
                match { it.size == 1 && it.first().name == "trade_date" }
            )
        }

        service.analyzeDocument(pdfMockFile("content"), "US")

        verify {
            persistence.saveAttributes(
                "US",
                match { it.size == 1 && it.first().name == "trade_date" }
            )
        }
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
        assertEquals(null, attr.mandatoryCondition)
        assertEquals(null, attr.valueRestrictions)
        assertEquals(null, attr.rules)
        assertEquals(null, attr.applicableReportTypes)
        assertEquals(null, attr.applicableAssetClasses)
    }

    // ─── New fields: rules, applicableReportTypes, applicableAssetClasses ─────

    @Test
    fun `parses new fields rules applicableReportTypes and applicableAssetClasses`() {
        val json = """
            [
              {
                "name": "trade_date",
                "description": "Date of trade execution",
                "dataType": "Date",
                "mandatory": true,
                "format": "ISO 8601",
                "source": "Section 2.1",
                "applicableReportTypes": ["Trade Report", "Position Report"],
                "applicableAssetClasses": ["Rates", "FX"],
                "rules": [
                  {
                    "ruleType": "FORMAT",
                    "description": "Must be in ISO 8601 YYYY-MM-DD format"
                  }
                ]
              }
            ]
        """.trimIndent()
        val service = qaAgentServiceWithFixedResponse(json)

        val result = service.analyzeDocument(pdfMockFile("content"))

        assertEquals(1, result.totalAttributes)
        val attr = result.attributes.first()
        assertEquals("trade_date", attr.name)
        assertEquals(listOf("Trade Report", "Position Report"), attr.applicableReportTypes)
        assertEquals(listOf("Rates", "FX"), attr.applicableAssetClasses)
        assertNotNull(attr.rules)
        assertEquals(1, attr.rules!!.size)
        assertEquals("FORMAT", attr.rules.first().ruleType)
        assertEquals("Must be in ISO 8601 YYYY-MM-DD format", attr.rules.first().description)
    }

    @Test
    fun `parses mandatoryCondition and valueRestrictions`() {
        val json = """
            [
              {
                "name": "direction",
                "description": "Direction of the trade",
                "dataType": "String",
                "mandatory": false,
                "mandatoryCondition": "Mandatory for equity and FX trades",
                "valueRestrictions": ["BUY", "SELL"],
                "applicableReportTypes": ["Trade Report"],
                "applicableAssetClasses": ["Equity", "FX"],
                "rules": [
                  {
                    "ruleType": "MANDATORY_CONDITION",
                    "description": "Required for equity and FX trades",
                    "condition": "Asset class is Equity or FX",
                    "applicableProductTypes": ["Equity Swap", "FX Forward"]
                  },
                  {
                    "ruleType": "VALUE_RESTRICTION",
                    "description": "Only BUY or SELL are permitted",
                    "allowedValues": ["BUY", "SELL"]
                  }
                ]
              }
            ]
        """.trimIndent()
        val service = qaAgentServiceWithFixedResponse(json)

        val result = service.analyzeDocument(pdfMockFile("content"))
        val attr = result.attributes.first()

        assertEquals("Mandatory for equity and FX trades", attr.mandatoryCondition)
        assertEquals(listOf("BUY", "SELL"), attr.valueRestrictions)
        assertEquals(2, attr.rules!!.size)

        val mandatoryRule = attr.rules.first { it.ruleType == "MANDATORY_CONDITION" }
        assertEquals("Asset class is Equity or FX", mandatoryRule.condition)
        assertEquals(listOf("Equity Swap", "FX Forward"), mandatoryRule.applicableProductTypes)

        val valueRule = attr.rules.first { it.ruleType == "VALUE_RESTRICTION" }
        assertEquals(listOf("BUY", "SELL"), valueRule.allowedValues)
    }

    @Test
    fun `AttributeRule optional fields are nullable`() {
        val rule = AttributeRule(
            ruleType = "FORMAT",
            description = "Must be ISO 8601"
        )
        assertNotNull(rule)
        assertEquals(null, rule.condition)
        assertEquals(null, rule.allowedValues)
        assertEquals(null, rule.applicableProductTypes)
    }

    // ─── Review-rework cycle ──────────────────────────────────────────────────

    @Test
    fun `review rework cycle is called the configured number of times`() {
        val mockClient = mockk<Client>()
        val tracker = mutableListOf<String>()

        val service = object : QaAgentService(
            genAiClient = mockClient,
            objectMapper = objectMapper,
            modelName = "gemini-2.0-flash",
            reviewReworkCycles = 3,
            attributePersistenceService = mockk(relaxed = true)
        ) {
            override fun cacheDocument(file: java.io.File, displayName: String): String = "cache-id"

            override fun runAgent(pdfPath: String, cachedContentName: String): String {
                tracker += "extract"
                return "[]"
            }

            override fun runReviewAgent(pdfPath: String, cachedContentName: String, currentJson: String): String {
                tracker += "review"
                return currentJson
            }
        }

        service.analyzeDocument(pdfMockFile("content"))

        assertEquals(listOf("extract", "review", "review", "review"), tracker)
    }

    @Test
    fun `review rework cycle zero skips review step`() {
        val mockClient = mockk<Client>()
        val tracker = mutableListOf<String>()

        val service = object : QaAgentService(
            genAiClient = mockClient,
            objectMapper = objectMapper,
            modelName = "gemini-2.0-flash",
            reviewReworkCycles = 0,
            attributePersistenceService = mockk(relaxed = true)
        ) {
            override fun cacheDocument(file: java.io.File, displayName: String): String = "cache-id"

            override fun runAgent(pdfPath: String, cachedContentName: String): String {
                tracker += "extract"
                return "[]"
            }

            override fun runReviewAgent(pdfPath: String, cachedContentName: String, currentJson: String): String {
                tracker += "review"
                return currentJson
            }
        }

        service.analyzeDocument(pdfMockFile("content"))

        assertEquals(listOf("extract"), tracker)
    }

    @Test
    fun `binary document is cached once and reused across cycles`() {
        val mockClient = mockk<Client>()
        val tracker = mutableListOf<String>()

        val service = object : QaAgentService(
            genAiClient = mockClient,
            objectMapper = objectMapper,
            modelName = "gemini-2.0-flash",
            reviewReworkCycles = 2,
            attributePersistenceService = mockk(relaxed = true)
        ) {
            override fun cacheDocument(file: java.io.File, displayName: String): String {
                tracker += "cache:$displayName"
                return "cache-id"
            }

            override fun runAgent(pdfPath: String, cachedContentName: String): String {
                tracker += "agent:$cachedContentName"
                return "[]"
            }

            override fun runReviewAgent(
                pdfPath: String,
                cachedContentName: String,
                currentJson: String
            ): String {
                tracker += "review:$cachedContentName"
                return currentJson
            }
        }

        service.analyzeDocument(pdfMockFile("content"))

        assertEquals(
            listOf(
                "cache:test.pdf",
                "agent:cache-id",
                "review:cache-id",
                "review:cache-id"
            ),
            tracker
        )
    }

    @Test
    fun `review agent receives previous extraction output`() {
        val mockClient = mockk<Client>()
        val initialJson =
            """[{"name":"x","description":"d","dataType":"String","mandatory":true}]"""
        val refinedJson =
            """[{"name":"x","description":"refined","dataType":"String","mandatory":true}]"""

        val service = object : QaAgentService(
            genAiClient = mockClient,
            objectMapper = objectMapper,
            modelName = "gemini-2.0-flash",
            reviewReworkCycles = 1,
            attributePersistenceService = mockk(relaxed = true)
        ) {
            override fun cacheDocument(file: java.io.File, displayName: String): String = "cache-id"

            override fun runAgent(pdfPath: String, cachedContentName: String) = initialJson
            override fun runReviewAgent(pdfPath: String, cachedContentName: String, currentJson: String): String {
                assertEquals(initialJson, currentJson)
                return refinedJson
            }
        }

        val result = service.analyzeDocument(pdfMockFile("content"))
        assertEquals("refined", result.attributes.first().description)
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
 * Testable subclass of [QaAgentService] that bypasses the real agent calls and
 * instead returns a predetermined response string.  This lets us test the
 * parsing and response-assembly logic in isolation.
 *
 * Review cycles are also bypassed: [runReviewAgent] simply returns [currentJson]
 * unchanged, so the fixed extraction response flows through unmodified.
 */
class TestableQaAgentService(
    genAiClient: Client,
    objectMapper: ObjectMapper,
    modelName: String,
    private val fixedAgentResponse: String,
    attributePersistenceService: AttributePersistenceService = mockk(relaxed = true)
) : QaAgentService(
    genAiClient,
    objectMapper,
    modelName,
    attributePersistenceService = attributePersistenceService
) {

    override fun cacheDocument(file: java.io.File, displayName: String): String = "cached/$displayName"

    override fun runAgent(pdfPath: String, cachedContentName: String): String = fixedAgentResponse

    /** Pass-through: review cycles do not alter the fixed response in unit tests. */
    override fun runReviewAgent(pdfPath: String, cachedContentName: String, currentJson: String): String = currentJson
}
