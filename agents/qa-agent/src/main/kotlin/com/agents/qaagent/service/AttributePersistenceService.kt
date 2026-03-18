package com.agents.qaagent.service

import com.agents.qaagent.model.ReportableAttribute
import com.agents.qaagent.persistence.AttributeDefinitionEntity
import com.agents.qaagent.persistence.AttributeDefinitionRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class AttributePersistenceService(
    private val repository: AttributeDefinitionRepository,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(AttributePersistenceService::class.java)

    @Transactional
    fun saveAttributes(jurisdiction: String, attributes: List<ReportableAttribute>) {
        if (attributes.isEmpty()) {
            log.debug("No attributes to persist for jurisdiction={}", jurisdiction)
            return
        }

        val attributeNames = attributes.map { it.name }
        val existingByName = repository
            .findByJurisdictionAndAttributeNameIn(jurisdiction, attributeNames)
            .associateBy { it.attributeName }

        var createdCount = 0
        var updatedCount = 0

        val entitiesToSave = attributes.map { attribute ->
            val serialized = objectMapper.writeValueAsString(attribute)
            val existing = existingByName[attribute.name]
            if (existing != null) {
                existing.definitionJson = serialized
                existing.updatedAt = Instant.now()
                updatedCount++
                existing
            } else {
                createdCount++
                AttributeDefinitionEntity(
                    attributeName = attribute.name,
                    jurisdiction = jurisdiction,
                    definitionJson = serialized,
                    updatedAt = Instant.now()
                )
            }
        }

        repository.saveAll(entitiesToSave)
        log.debug(
            "Persisted {} attributes for jurisdiction={} (created={}, updated={})",
            attributes.size,
            jurisdiction,
            createdCount,
            updatedCount
        )
    }
}
