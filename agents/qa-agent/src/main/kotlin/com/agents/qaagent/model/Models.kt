package com.agents.qaagent.model

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * A single rule that governs when and how a reportable attribute must be populated.
 *
 * @param ruleType             Category of the rule: "FORMAT", "MANDATORY_CONDITION",
 *                             "VALUE_RESTRICTION", "PRODUCT_RULE", or "ASSET_CLASS_RULE".
 * @param description          Human-readable description of the rule.
 * @param condition            The condition under which this rule applies (optional).
 * @param allowedValues        Enumerated list of permitted values, if applicable.
 * @param applicableProductTypes  Product types to which this rule is scoped (optional).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AttributeRule(
    val ruleType: String,
    val description: String,
    val condition: String? = null,
    val allowedValues: List<String>? = null,
    val applicableProductTypes: List<String>? = null
)

/**
 * A single reportable attribute extracted from a regulatory document.
 *
 * @param name                  Short identifier / field name (e.g. "trade_date").
 * @param description           Human-readable description from the spec.
 * @param dataType              Expected data type (e.g. "Date", "Decimal", "String").
 * @param mandatory             Whether the field is mandatory or optional.
 * @param format                Any specific format requirement (e.g. "ISO 8601", "LEI").
 * @param source                Section or page reference in the source document.
 * @param mandatoryCondition    Condition(s) under which the field becomes mandatory (if conditionally required).
 * @param valueRestrictions     Enumerated list of allowed or restricted values.
 * @param rules                 All rules applicable to this attribute (format, mandatory-condition,
 *                              value-restriction, product-rule, asset-class-rule).
 * @param applicableReportTypes Report types (e.g. "Trade Report", "Position Report") to which
 *                              this attribute applies.
 * @param applicableAssetClasses Asset classes (e.g. "Rates", "Credit", "FX", "Equity") to which
 *                              this attribute applies.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ReportableAttribute(
    val name: String,
    val description: String,
    val dataType: String,
    val mandatory: Boolean,
    val format: String? = null,
    val source: String? = null,
    val mandatoryCondition: String? = null,
    val valueRestrictions: List<String>? = null,
    val rules: List<AttributeRule>? = null,
    val applicableReportTypes: List<String>? = null,
    val applicableAssetClasses: List<String>? = null
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
