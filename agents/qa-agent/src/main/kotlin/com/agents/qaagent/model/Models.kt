package com.agents.qaagent.model

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * A single reportable attribute extracted from a regulatory document.
 *
 * @param name        Short identifier / field name (e.g. "trade_date").
 * @param description Human-readable description from the spec.
 * @param dataType    Expected data type (e.g. "Date", "Decimal", "String").
 * @param mandatory   Whether the field is mandatory or optional.
 * @param format      Any specific format requirement (e.g. "ISO 8601", "LEI").
 * @param source      Section or page reference in the source document.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ReportableAttribute(
    val name: String,
    val description: String,
    val dataType: String,
    val mandatory: Boolean,
    val format: String? = null,
    val source: String? = null
)

/**
 * Request model for the /api/qa/analyze endpoint.
 */
data class AnalyzeRequest(
    val documentName: String = "uploaded-document.pdf"
)

/**
 * Response model returned after processing a regulatory document.
 */
data class AnalyzeResponse(
    val documentName: String,
    val totalAttributes: Int,
    val mandatoryCount: Int,
    val optionalCount: Int,
    val attributes: List<ReportableAttribute>
)
