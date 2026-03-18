package com.agents.qaagent.service

import com.agents.qaagent.model.ReportableAttribute
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Instant

@Service
class AttributePersistenceService(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(AttributePersistenceService::class.java)

    @Transactional
    fun saveAttributes(jurisdiction: String, attributes: List<ReportableAttribute>) {
        if (attributes.isEmpty()) {
            log.debug("No attributes to persist for jurisdiction={}", jurisdiction)
            return
        }

        var createdCount = 0
        var updatedCount = 0
        val now = Timestamp.from(Instant.now())

        attributes.forEach { attribute ->
            val serialized = objectMapper.writeValueAsString(attribute)
            val updated = jdbcTemplate.update(
                """
                UPDATE attribute_definitions
                SET definition_json = ?, updated_at = ?
                WHERE attribute_name = ? AND jurisdiction = ?
                """.trimIndent(),
                serialized,
                now,
                attribute.name,
                jurisdiction
            )

            if (updated == 0) {
                jdbcTemplate.update(
                    """
                    INSERT INTO attribute_definitions (attribute_name, jurisdiction, definition_json, updated_at)
                    VALUES (?, ?, ?, ?)
                    """.trimIndent(),
                    attribute.name,
                    jurisdiction,
                    serialized,
                    now
                )
                createdCount++
            } else {
                updatedCount++
            }
        }

        log.debug(
            "Persisted {} attributes for jurisdiction={} (created={}, updated={})",
            attributes.size,
            jurisdiction,
            createdCount,
            updatedCount
        )
    }
}
