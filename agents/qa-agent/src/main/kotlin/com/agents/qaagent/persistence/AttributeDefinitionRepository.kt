package com.agents.qaagent.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface AttributeDefinitionRepository : JpaRepository<AttributeDefinitionEntity, Long> {
    fun findByJurisdictionAndAttributeNameIn(
        jurisdiction: String,
        attributeNames: List<String>
    ): List<AttributeDefinitionEntity>
}
