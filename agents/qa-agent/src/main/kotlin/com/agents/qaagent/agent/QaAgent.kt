package com.agents.qaagent.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import com.agents.qaagent.executor.GenAIVertexPromptExecutor
import com.google.genai.Client

/**
 * Builds and returns the koog [AIAgent] responsible for extracting reportable
 * attributes from a regulatory reporting specification document.
 *
 * ## Workflow
 * 1. **attribute_extract_node** – the agent sends the attached PDF together
 *    with a detailed system prompt to Vertex AI Gemini, which identifies and
 *    structures the reportable attributes as JSON using the binary document
 *    (not text extraction).
 *
 * @param genAiClient        Configured [Client] from the Spring context.
 * @param modelName          Gemini model to use (e.g. `gemini-2.0-flash`).
 * @param cachedContentName  Cached content handle for the uploaded PDF.
 */
fun buildQaAgent(genAiClient: Client, modelName: String, cachedContentName: String): AIAgent {

    val promptExecutor = GenAIVertexPromptExecutor(genAiClient, modelName, cachedContentName)

    val model = LLModel(LLMProvider.Google, modelName, listOf(LLMCapability.Tools))

    val agentConfig = AIAgentConfig.withSystemPrompt(SYSTEM_PROMPT, model, "qa-agent")

    val agentStrategy = strategy("regulatory-qa-strategy") {
        val extractNode by nodeLLMRequest("extract_node")

        edge(nodeStart forwardTo extractNode)
        edge((extractNode forwardTo nodeFinish).onAssistantMessage { true })
    }

    return AIAgent(
        promptExecutor = promptExecutor,
        strategy = agentStrategy,
        agentConfig = agentConfig
    )
}

/**
 * Builds and returns the koog [AIAgent] responsible for reviewing and refining
 * a previously extracted set of reportable attributes.
 *
 * ## Workflow
 * 1. **review_node** – the agent re-reads the attached PDF (cached binary
 *    content) to look up section references, then returns the refined
 *    attribute JSON.
 *
 * @param genAiClient        Configured [Client] from the Spring context.
 * @param modelName          Gemini model to use (e.g. `gemini-2.0-flash`).
 * @param cachedContentName  Cached content handle for the uploaded PDF.
 */
fun buildReviewAgent(genAiClient: Client, modelName: String, cachedContentName: String): AIAgent {

    val promptExecutor = GenAIVertexPromptExecutor(genAiClient, modelName, cachedContentName)

    val model = LLModel(LLMProvider.Google, modelName, listOf(LLMCapability.Tools))

    val agentConfig = AIAgentConfig.withSystemPrompt(REVIEW_SYSTEM_PROMPT, model, "review-agent")

    val agentStrategy = strategy("regulatory-review-strategy") {
        val reviewNode by nodeLLMRequest("review_node")

        edge(nodeStart forwardTo reviewNode)
        edge((reviewNode forwardTo nodeFinish).onAssistantMessage { true })
    }

    return AIAgent(
        promptExecutor = promptExecutor,
        strategy = agentStrategy,
        agentConfig = agentConfig
    )
}

// ─── System prompts ───────────────────────────────────────────────────────────

private val SYSTEM_PROMPT = """
You are a Quality Assurance agent specializing in financial regulatory reporting.
The full PDF is attached to the conversation as binary cached content. Read the
attached document directly (do not rely on text-only summaries).

Your task is to analyze a regulatory reporting specification document for a financial
organization and extract every **reportable attribute** (data field or element that
the regulation requires to be reported).

For each reportable attribute output a JSON object with the following fields:
  • "name"                  – machine-friendly identifier (snake_case)
  • "description"           – short human-readable description taken directly from the spec
  • "dataType"              – expected data type (e.g. "String", "Date", "Decimal", "Boolean")
  • "mandatory"             – true if the field is always mandatory/required, false if always optional
  • "format"                – format constraint if specified (e.g. "ISO 8601", "LEI", "ISIN") – omit if none
  • "source"                – section or page reference in the document (e.g. "Section 3.1") – omit if none
  • "mandatoryCondition"    – if the field is conditionally mandatory, describe the condition(s)
                              under which it is required – omit if the field is always mandatory/optional
  • "valueRestrictions"     – JSON array of allowed or restricted values if an enumeration is defined
                              (e.g. ["BUY", "SELL"]) – omit if no value restriction applies
  • "rules"                 – JSON array of rule objects that govern this attribute; each rule has:
      - "ruleType"               – one of: "FORMAT", "MANDATORY_CONDITION", "VALUE_RESTRICTION",
                                   "PRODUCT_RULE", "ASSET_CLASS_RULE"
      - "description"            – description of the rule
      - "condition"              – condition under which the rule applies (omit if unconditional)
      - "allowedValues"          – array of permitted values relevant to this rule (omit if not applicable)
      - "applicableProductTypes" – array of product/contract types this rule is scoped to
                                   (omit if it applies to all products)
  • "applicableReportTypes" – JSON array of report types to which this attribute applies
                              (e.g. ["Trade Report", "Position Report", "Collateral Report"])
  • "applicableAssetClasses"– JSON array of asset classes to which this attribute applies
                              (e.g. ["Rates", "Credit", "FX", "Equity", "Commodity"])

Return your answer as a **JSON array** of these objects and nothing else.
Do not include any prose, markdown, or code fences outside the JSON array.

Example output:
[
  {
    "name": "trade_date",
    "description": "Date on which the transaction was executed",
    "dataType": "Date",
    "mandatory": true,
    "format": "ISO 8601 (YYYY-MM-DD)",
    "source": "Section 2.1",
    "applicableReportTypes": ["Trade Report"],
    "applicableAssetClasses": ["Rates", "Credit", "FX", "Equity", "Commodity"],
    "rules": [
      {
        "ruleType": "FORMAT",
        "description": "Must be reported in ISO 8601 date format YYYY-MM-DD"
      }
    ]
  },
  {
    "name": "notional_amount",
    "description": "Notional or nominal value of the contract",
    "dataType": "Decimal",
    "mandatory": false,
    "mandatoryCondition": "Mandatory for interest rate and credit derivative contracts",
    "source": "Section 2.4",
    "applicableReportTypes": ["Trade Report"],
    "applicableAssetClasses": ["Rates", "Credit"],
    "rules": [
      {
        "ruleType": "MANDATORY_CONDITION",
        "description": "Required for interest rate and credit derivatives",
        "condition": "Contract type is interest rate swap or credit default swap",
        "applicableProductTypes": ["Interest Rate Swap", "Credit Default Swap"]
      },
      {
        "ruleType": "VALUE_RESTRICTION",
        "description": "Must be a positive decimal value with up to 2 decimal places"
      }
    ]
  }
]
""".trimIndent()

private val REVIEW_SYSTEM_PROMPT = """
You are a Quality Assurance reviewer specializing in financial regulatory reporting.

You will be given:
1. The file path of a regulatory reporting specification PDF.
2. A JSON array of reportable attributes extracted from that document.

Your task is to **review and refine** the extracted attributes by:
  1. Re-reading the attached PDF (binary cached content) to verify accuracy and completeness.
  2. Resolving any section cross-references (e.g. "see Section 4.2" → look up that section and
     fill in the actual rule details instead of leaving a reference).
  3. Ensuring every attribute has all applicable rules populated, including:
     – FORMAT rules (expected format constraints)
     – MANDATORY_CONDITION rules (conditions under which the field is required)
     – VALUE_RESTRICTION rules (enumerated allowed values)
     – PRODUCT_RULE rules (rules specific to certain product/contract types)
     – ASSET_CLASS_RULE rules (rules specific to certain asset classes)
  4. Verifying that "applicableReportTypes" and "applicableAssetClasses" are accurate and complete.
  5. Correcting any inaccuracies or gaps in "mandatoryCondition" and "valueRestrictions".

Return the **refined JSON array** of reportable attributes and nothing else.
Do not include any prose, markdown, or code fences outside the JSON array.
Each object must include all the fields from the original schema:
  name, description, dataType, mandatory, format (if applicable), source (if applicable),
  mandatoryCondition (if applicable), valueRestrictions (if applicable), rules,
  applicableReportTypes, applicableAssetClasses.
""".trimIndent()
