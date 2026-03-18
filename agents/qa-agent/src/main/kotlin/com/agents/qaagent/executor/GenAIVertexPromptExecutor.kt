package com.agents.qaagent.executor

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import com.google.genai.Client
import com.google.genai.types.Content
import com.google.genai.types.FunctionCall
import com.google.genai.types.FunctionDeclaration
import com.google.genai.types.FunctionResponse
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.Part
import com.google.genai.types.Schema
import com.google.genai.types.Tool
import com.google.genai.types.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * A koog [PromptExecutor] that delegates all LLM calls to **Vertex AI Gemini**
 * using the official Google GenAI SDK (`com.google.genai:google-genai`).
 *
 * The executor converts koog's [Prompt] messages into the GenAI SDK's [Content]
 * format, executes the request against Vertex AI, and maps the response back
 * into koog's [Message.Response] types.
 *
 * @param client     Configured [Client] (Vertex AI mode).
 * @param modelName  Gemini model to use (e.g. `gemini-2.0-flash`).
 */
class GenAIVertexPromptExecutor(
    private val client: Client,
    private val modelName: String
) : PromptExecutor {

    /**
     * Execute a single prompt turn and return the model's response messages.
     *
     * @param prompt  Conversation history as a koog [Prompt].
     * @param model   The koog [LLModel] (its id is used as fallback model name).
     * @param tools   Tool descriptors exposed to the model.
     */
    override suspend fun execute(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>
    ): List<Message.Response> = withContext(Dispatchers.IO) {

        val contents = prompt.messages.mapNotNull { it.toGenAIContent() }

        val configBuilder = GenerateContentConfig.builder()
        if (tools.isNotEmpty()) {
            configBuilder.tools(listOf(tools.toGenAITool()))
        }

        val response = client.models.generateContent(
            modelName.ifBlank { model.id },
            contents,
            configBuilder.build()
        )

        val candidates = response.candidates().orElse(emptyList())
        val candidate = candidates.firstOrNull() ?: return@withContext emptyList()

        val resultMessages = mutableListOf<Message.Response>()

        val parts: List<Part> = candidate.content()
            .orElse(null)
            ?.parts()
            ?.orElse(emptyList())
            ?: emptyList()

        for (part in parts) {
            val textOpt = part.text()
            val fcOpt = part.functionCall()
            when {
                textOpt.isPresent && textOpt.get().isNotBlank() ->
                    resultMessages += Message.Assistant(textOpt.get())

                fcOpt.isPresent -> {
                    val fc = fcOpt.get()
                    val toolName = fc.name().orElse("")
                    val toolArgs = fc.args().map { it.toString() }.orElse("{}")
                    resultMessages += Message.Tool.Call(
                        id = toolName,
                        tool = toolName,
                        content = toolArgs
                    )
                }
            }
        }

        resultMessages
    }

    /** Streaming is not used by the current agent workflow. */
    override suspend fun executeStreaming(prompt: Prompt, model: LLModel): Flow<String> = flow {
        throw UnsupportedOperationException(
            "Streaming is not supported by GenAIVertexPromptExecutor"
        )
    }

    // ─── Conversion helpers ───────────────────────────────────────────────────

    private fun Message.toGenAIContent(): Content? = when (this) {
        is Message.System -> Content.builder()
            .role("user")
            .parts(listOf(Part.builder().text("[SYSTEM] $content").build()))
            .build()

        is Message.User -> Content.builder()
            .role("user")
            .parts(listOf(Part.builder().text(content).build()))
            .build()

        is Message.Assistant -> Content.builder()
            .role("model")
            .parts(listOf(Part.builder().text(content).build()))
            .build()

        is Message.Tool.Call -> Content.builder()
            .role("model")
            .parts(listOf(
                Part.builder()
                    .functionCall(
                        FunctionCall.builder()
                            .name(tool)
                            .build()
                    )
                    .build()
            ))
            .build()

        is Message.Tool.Result -> Content.builder()
            .role("user")
            .parts(listOf(
                Part.builder()
                    .functionResponse(
                        FunctionResponse.builder()
                            .name(tool)
                            .id(id)
                            .response(mapOf("result" to content))
                            .build()
                    )
                    .build()
            ))
            .build()

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
        is ToolParameterType.String  -> Type.Known.STRING
        is ToolParameterType.Float   -> Type.Known.NUMBER
        is ToolParameterType.Integer -> Type.Known.INTEGER
        is ToolParameterType.Boolean -> Type.Known.BOOLEAN
        is ToolParameterType.List    -> Type.Known.ARRAY
        is ToolParameterType.Object  -> Type.Known.OBJECT
        else                         -> Type.Known.STRING
    }
}
