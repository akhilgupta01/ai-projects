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

        attributes.forEach { attribute ->
            val serialized = objectMapper.writeValueAsString(attribute)
            val existing = repository.findByAttributeNameAndJurisdiction(attribute.name, jurisdiction)

            if (existing != null) {
                existing.definitionJson = serialized
                existing.updatedAt = Instant.now()
                repository.save(existing)
                log.debug(
                    "Updated attribute definition for name={} jurisdiction={}",
                    attribute.name,
                    jurisdiction
                )
            } else {
                repository.save(
                    AttributeDefinitionEntity(
                        attributeName = attribute.name,
                        jurisdiction = jurisdiction,
                        definitionJson = serialized,
                        updatedAt = Instant.now()
                    )
                )
                log.debug(
                    "Created attribute definition for name={} jurisdiction={}",
                    attribute.name,
                    jurisdiction
                )
            }
        }
    }
}
