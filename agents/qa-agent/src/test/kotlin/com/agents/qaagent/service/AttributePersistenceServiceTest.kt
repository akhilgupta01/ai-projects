package com.agents.qaagent.service

import com.agents.qaagent.model.ReportableAttribute
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate

class AttributePersistenceServiceTest {

    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    @Test
    fun `inserts when update affects no rows`() {
        val jdbcTemplate = mockk<JdbcTemplate>(relaxed = true)
        every { jdbcTemplate.update(match { it.startsWith("UPDATE") }, any(), any(), any(), any()) } returns 0
        every { jdbcTemplate.update(match { it.startsWith("INSERT") }, any(), any(), any(), any()) } returns 1

        val service = AttributePersistenceService(jdbcTemplate, objectMapper)
        service.saveAttributes("EU", listOf(sampleAttribute()))

        verify(exactly = 1) {
            jdbcTemplate.update(match { it.startsWith("UPDATE") }, any(), any(), "trade_date", "EU")
        }
        verify(exactly = 1) {
            jdbcTemplate.update(match { it.startsWith("INSERT") }, "trade_date", "EU", any(), any())
        }
    }

    @Test
    fun `updates existing definition when row exists`() {
        val jdbcTemplate = mockk<JdbcTemplate>(relaxed = true)
        every { jdbcTemplate.update(match { it.startsWith("UPDATE") }, any(), any(), any(), any()) } returns 1

        val service = AttributePersistenceService(jdbcTemplate, objectMapper)
        service.saveAttributes("EU", listOf(sampleAttribute(description = "Updated description")))

        verify(exactly = 1) {
            jdbcTemplate.update(match { it.startsWith("UPDATE") }, any(), any(), "trade_date", "EU")
        }
        verify(exactly = 0) {
            jdbcTemplate.update(match { it.startsWith("INSERT") }, any(), any(), any(), any())
        }
    }

    private fun sampleAttribute(description: String = "Trade date"): ReportableAttribute =
        ReportableAttribute(
            name = "trade_date",
            description = description,
            dataType = "Date",
            mandatory = true
        )
}
