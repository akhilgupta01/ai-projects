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
        val saved = slot<AttributeDefinitionEntity>()
        every { repository.findByAttributeNameAndJurisdiction("trade_date", "EU") } returns null
        every { repository.save(capture(saved)) } answers { saved.captured }

        val service = AttributePersistenceService(repository, objectMapper)
        service.saveAttributes("EU", listOf(sampleAttribute()))

        assertEquals("trade_date", saved.captured.attributeName)
        assertEquals("EU", saved.captured.jurisdiction)
        assertTrue(saved.captured.definitionJson.contains("\"trade_date\""))
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
        val saved = slot<AttributeDefinitionEntity>()
        val previousUpdatedAt = existing.updatedAt

        every { repository.findByAttributeNameAndJurisdiction("trade_date", "EU") } returns existing
        every { repository.save(capture(saved)) } answers { saved.captured }

        val service = AttributePersistenceService(repository, objectMapper)
        service.saveAttributes(
            "EU",
            listOf(sampleAttribute(description = "Updated description"))
        )

        assertEquals(existing.id, saved.captured.id)
        assertTrue(saved.captured.definitionJson.contains("Updated description"))
        assertTrue(saved.captured.updatedAt.isAfter(previousUpdatedAt))
    }

    private fun sampleAttribute(description: String = "Trade date"): ReportableAttribute =
        ReportableAttribute(
            name = "trade_date",
            description = description,
            dataType = "Date",
            mandatory = true
        )
}
