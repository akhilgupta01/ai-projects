package com.agents.qaagent.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface AttributeDefinitionRepository : JpaRepository<AttributeDefinitionEntity, Long> {
    fun findByAttributeNameAndJurisdiction(attributeName: String, jurisdiction: String): AttributeDefinitionEntity?
}
