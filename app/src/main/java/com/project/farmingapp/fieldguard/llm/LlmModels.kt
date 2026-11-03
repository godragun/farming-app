package com.project.farmingapp.fieldguard.llm

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

/** OpenAI-compatible Chat Completions request/response models. */

data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val tools: List<ToolDefinition>? = null,
    @SerializedName("tool_choice") val toolChoice: String? = "auto",
    val temperature: Double? = 0.2
)

data class ChatMessage(
    val role: String,
    val content: JsonElement? = null,
    @SerializedName("tool_calls") val toolCalls: List<ToolCall>? = null,
    @SerializedName("tool_call_id") val toolCallId: String? = null,
    val name: String? = null
)

data class ToolDefinition(
    val type: String = "function",
    val function: ToolFunction
)

data class ToolFunction(
    val name: String,
    val description: String,
    val parameters: Map<String, Any>
)

data class ToolCall(
    val id: String,
    val type: String? = "function",
    val function: ToolCallFunction
)

data class ToolCallFunction(
    val name: String,
    val arguments: String
)

data class ChatCompletionResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<ChatChoice>? = null,
    val error: LlmApiError? = null
)

data class ChatChoice(
    val index: Int? = null,
    val message: ChatMessage? = null,
    @SerializedName("finish_reason") val finishReason: String? = null
)

data class LlmApiError(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null
)

data class ConnectionTestResult(
    val ok: Boolean,
    val reply: String,
    val latencyMs: Long,
    val model: String,
    val baseUrl: String,
    val error: String? = null
)
