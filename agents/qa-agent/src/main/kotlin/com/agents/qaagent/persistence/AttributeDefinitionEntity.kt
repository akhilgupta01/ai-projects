package com.agents.qaagent.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "attribute_definitions",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["attribute_name", "jurisdiction"])
    ]
)
class AttributeDefinitionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "attribute_name", nullable = false)
    var attributeName: String,

    @Column(name = "jurisdiction", nullable = false)
    var jurisdiction: String,

    @Lob
    @Column(name = "definition_json", nullable = false)
    var definitionJson: String,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    // JPA requires a no-arg constructor
    constructor() : this(
        id = null,
        attributeName = "",
        jurisdiction = "",
        definitionJson = "",
        updatedAt = Instant.now()
    )
}
