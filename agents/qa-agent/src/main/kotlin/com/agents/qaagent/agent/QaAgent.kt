package com.agents.qaagent.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.dsl.extension.nodeLLMSendMessage
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.llms.unified.UnifiedLLMPromptExecutor
import ai.koog.prompt.llm.LLMModel
import com.agents.qaagent.executor.GenAIVertexPromptExecutor
import com.agents.qaagent.tools.PdfTextExtractorTool
import com.google.genai.Client

/**
 * Builds and returns the koog [AIAgent] responsible for extracting reportable
 * attributes from a regulatory reporting specification document.
 *
 * ## Workflow
 * 1. **pdf_extract_node** – the agent invokes [PdfTextExtractorTool] to read
 *    the raw text from the uploaded PDF.
 * 2. **attribute_extract_node** – the agent sends the extracted text together
 *    with a detailed system prompt to Vertex AI Gemini, which identifies and
 *    structures the reportable attributes as JSON.
 *
 * @param genAiClient  Configured [Client] from the Spring context.
 * @param modelName    Gemini model to use (e.g. `gemini-2.0-flash`).
 */
fun buildQaAgent(genAiClient: Client, modelName: String): AIAgent {

    val toolRegistry = ToolRegistry {
        tool(PdfTextExtractorTool)
    }

    val promptExecutor: UnifiedLLMPromptExecutor =
        GenAIVertexPromptExecutor(genAiClient, modelName)

    val agentStrategy = strategy("regulatory-qa-strategy") {
        // Node 1: extract PDF text via tool call
        val pdfExtractNode = nodeLLMRequest("pdf_extract_node")

        // Node 2: extract reportable attributes from the raw text
        val attributeExtractNode = nodeLLMSendMessage("attribute_extract_node")

        edge(nodeInput forwardTo pdfExtractNode)
        edge(pdfExtractNode forwardTo attributeExtractNode)
        edge(attributeExtractNode forwardTo nodeFinish)
    }

    return AIAgent(
        promptExecutor = promptExecutor,
        toolRegistry = toolRegistry,
        strategy = agentStrategy,
        agentConfig = AIAgentConfig(
            id = "qa-agent",
            systemPrompt = SYSTEM_PROMPT
        )
    )
}

/**
 * Builds and returns the koog [AIAgent] responsible for reviewing and refining
 * a previously extracted set of reportable attributes.
 *
 * ## Workflow
 * 1. **review_node** – the agent may re-invoke [PdfTextExtractorTool] to look
 *    up section references, then returns the refined attribute JSON.
 *
 * @param genAiClient  Configured [Client] from the Spring context.
 * @param modelName    Gemini model to use (e.g. `gemini-2.0-flash`).
 */
fun buildReviewAgent(genAiClient: Client, modelName: String): AIAgent {

    val toolRegistry = ToolRegistry {
        tool(PdfTextExtractorTool)
    }

    val promptExecutor: UnifiedLLMPromptExecutor =
        GenAIVertexPromptExecutor(genAiClient, modelName)

    val agentStrategy = strategy("regulatory-review-strategy") {
        // Single node: may call the PDF tool to resolve section references,
        // then returns the refined JSON array
        val reviewNode = nodeLLMRequest("review_node")

        edge(nodeInput forwardTo reviewNode)
        edge(reviewNode forwardTo nodeFinish)
    }

    return AIAgent(
        promptExecutor = promptExecutor,
        toolRegistry = toolRegistry,
        strategy = agentStrategy,
        agentConfig = AIAgentConfig(
            id = "review-agent",
            systemPrompt = REVIEW_SYSTEM_PROMPT
        )
    )
}

// ─── System prompts ───────────────────────────────────────────────────────────

private val SYSTEM_PROMPT = """
You are a Quality Assurance agent specializing in financial regulatory reporting.

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
  1. Re-reading the document (use the extract_pdf_text tool) to verify accuracy and completeness.
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

// ─── Model wrapper ─────────────────────────────────────────────────────────────

/**
 * A lightweight [LLMModel] wrapper that carries the Vertex AI model name.
 */
data class VertexGeminiModel(override val id: String) : LLMModel
