package com.agents.qaagent.executor

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.prompt.executor.llms.unified.UnifiedLLMPromptExecutor
import ai.koog.prompt.llm.LLMModel
import ai.koog.prompt.message.AssistantMessage
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.SystemMessage
import ai.koog.prompt.message.ToolCallMessage
import ai.koog.prompt.message.ToolResultMessage
import ai.koog.prompt.message.UserMessage
import com.google.genai.Client
import com.google.genai.types.Content
import com.google.genai.types.FunctionDeclaration
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.Part
import com.google.genai.types.Schema
import com.google.genai.types.Tool
import com.google.genai.types.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A koog [UnifiedLLMPromptExecutor] that delegates all LLM calls to
 * **Vertex AI Gemini** using the official Google GenAI SDK
 * (`com.google.genai:google-genai`).
 *
 * The executor converts koog's prompt messages into the GenAI SDK's
 * [Content] format, executes the request against Vertex AI, and maps
 * the response back into koog's [Message] types.
 *
 * @param client     Configured [Client] (Vertex AI mode).
 * @param modelName  Gemini model to use (e.g. `gemini-2.0-flash`).
 */
class GenAIVertexPromptExecutor(
    private val client: Client,
    private val modelName: String
) : UnifiedLLMPromptExecutor {

    /**
     * Execute a single prompt turn and return the model's response messages.
     *
     * @param messages   Conversation history as koog [Message] objects.
     * @param model      The koog [LLMModel] (its id is used as fallback model name).
     * @param tools      Tool descriptors exposed to the model.
     * @param jsonSchema Optional JSON schema for structured output.
     */
    override suspend fun executeRequest(
        messages: List<Message>,
        model: LLMModel,
        tools: List<ToolDescriptor>,
        jsonSchema: String?
    ): List<Message> = withContext(Dispatchers.IO) {

        val contents = messages.mapNotNull { it.toGenAIContent() }

        val configBuilder = GenerateContentConfig.builder()
        if (tools.isNotEmpty()) {
            configBuilder.tools(listOf(tools.toGenAITool()))
        }

        val response = client.models.generateContent(
            modelName.ifBlank { model.id },
            contents,
            configBuilder.build()
        )

        val candidate = response.candidates()?.firstOrNull()
            ?: return@withContext emptyList()

        val resultMessages = mutableListOf<Message>()

        candidate.content()?.parts()?.forEach { part ->
            when {
                part.text() != null -> resultMessages += AssistantMessage(part.text()!!)
                part.functionCall() != null -> {
                    val fc = part.functionCall()!!
                    resultMessages += ToolCallMessage(
                        toolName = fc.name() ?: "",
                        toolArgs = fc.args()?.toString() ?: "{}"
                    )
                }
            }
        }

        resultMessages
    }

    // ─── Conversion helpers ───────────────────────────────────────────────────

    private fun Message.toGenAIContent(): Content? = when (this) {
        is SystemMessage -> Content.builder()
            .role("user")
            .parts(listOf(Part.builder().text("[SYSTEM] $content").build()))
            .build()

        is UserMessage -> Content.builder()
            .role("user")
            .parts(listOf(Part.builder().text(content).build()))
            .build()

        is AssistantMessage -> Content.builder()
            .role("model")
            .parts(listOf(Part.builder().text(content).build()))
            .build()

        is ToolResultMessage -> Content.builder()
            .role("user")
            .parts(listOf(Part.builder().text("[TOOL RESULT: $toolName] $result").build()))
            .build()

        is ToolCallMessage -> null // tool calls come from the model, not the user

        else -> null
    }

    private fun List<ToolDescriptor>.toGenAITool(): Tool {
        val declarations = map { descriptor ->
            val allParams = descriptor.requiredParameters + descriptor.optionalParameters
            val properties = allParams.associate { param ->
                param.name to Schema.builder()
                    .type(param.type.toGenAIType())
                    .description(param.description)
                    .build()
            }
            FunctionDeclaration.builder()
                .name(descriptor.name)
                .description(descriptor.description)
                .parameters(
                    Schema.builder()
                        .type(Type.Known.OBJECT)
                        .properties(properties)
                        .required(descriptor.requiredParameters.map { it.name })
                        .build()
                )
                .build()
        }
        return Tool.builder().functionDeclarations(declarations).build()
    }

    private fun ToolParameterType.toGenAIType(): Type.Known = when (this) {
        ToolParameterType.String  -> Type.Known.STRING
        ToolParameterType.Number  -> Type.Known.NUMBER
        ToolParameterType.Integer -> Type.Known.INTEGER
        ToolParameterType.Boolean -> Type.Known.BOOLEAN
        ToolParameterType.Array   -> Type.Known.ARRAY
        ToolParameterType.Object  -> Type.Known.OBJECT
        else                      -> Type.Known.STRING
    }
}
