package com.agents.qaagent.service

import com.agents.qaagent.model.ReportableAttribute
import com.agents.qaagent.persistence.AttributeDefinitionEntity
import com.agents.qaagent.persistence.AttributeDefinitionRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class AttributePersistenceServiceTest {

    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    @Test
    fun `creates new definition when none exists`() {
        val repository = mockk<AttributeDefinitionRepository>()
        val saved = slot<List<AttributeDefinitionEntity>>()
        every {
            repository.findByJurisdictionAndAttributeNameIn("EU", listOf("trade_date"))
        } returns emptyList()
        every { repository.saveAll(capture(saved)) } answers { saved.captured }

        val service = AttributePersistenceService(repository, objectMapper)
        service.saveAttributes("EU", listOf(sampleAttribute()))

        val savedEntity = saved.captured.single()
        assertEquals("trade_date", savedEntity.attributeName)
        assertEquals("EU", savedEntity.jurisdiction)
        assertTrue(savedEntity.definitionJson.contains("\"trade_date\""))
    }

    @Test
    fun `updates existing definition for same key`() {
        val repository = mockk<AttributeDefinitionRepository>()
        val existing = AttributeDefinitionEntity(
            id = 1,
            attributeName = "trade_date",
            jurisdiction = "EU",
            definitionJson = """{"name":"trade_date","description":"old"}""",
            updatedAt = Instant.now().minusSeconds(120)
        )
        val saved = slot<List<AttributeDefinitionEntity>>()
        val previousUpdatedAt = existing.updatedAt

        every {
            repository.findByJurisdictionAndAttributeNameIn("EU", listOf("trade_date"))
        } returns listOf(existing)
        every { repository.saveAll(capture(saved)) } answers { saved.captured }

        val service = AttributePersistenceService(repository, objectMapper)
        service.saveAttributes(
            "EU",
            listOf(sampleAttribute(description = "Updated description"))
        )

        val savedEntity = saved.captured.single()
        assertEquals(existing.id, savedEntity.id)
        assertTrue(savedEntity.definitionJson.contains("Updated description"))
        assertTrue(savedEntity.updatedAt.isAfter(previousUpdatedAt))
    }

    private fun sampleAttribute(description: String = "Trade date"): ReportableAttribute =
        ReportableAttribute(
            name = "trade_date",
            description = description,
            dataType = "Date",
            mandatory = true
        )
}
