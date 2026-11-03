package com.project.farmingapp.fieldguard.llm

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.project.farmingapp.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * OpenAI-compatible Chat Completions client for FieldGuard AI.
 * Uses OkHttp (same stack as Agri Lanka AI helpers). Never invents answers on failure.
 */
class LlmClient(
    private val baseUrl: String = BuildConfig.LLM_BASE_URL,
    private val model: String = BuildConfig.LLM_MODEL,
    private val apiKey: String = BuildConfig.LLM_API_KEY
) {
    private val gson: Gson = GsonBuilder().serializeNulls().create()
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    fun configuredModel(): String = model
    fun configuredBaseUrl(): String = baseUrl.trimEnd('/')
    fun hasApiKey(): Boolean = apiKey.isNotBlank() || looksLikeLocalServer(baseUrl)

    /**
     * One real Chat Completions request for the Settings "Test connection" button.
     */
    fun testConnection(): ConnectionTestResult {
        val start = System.currentTimeMillis()
        return try {
            if (!hasApiKey() && !looksLikeLocalServer(baseUrl)) {
                return ConnectionTestResult(
                    ok = false,
                    reply = "",
                    latencyMs = 0,
                    model = model,
                    baseUrl = configuredBaseUrl(),
                    error = "LLM_API_KEY missing in local.properties (required for Meta Model API)"
                )
            }
            val messages = listOf(
                textMessage("system", "You are FieldGuard AI connectivity check. Reply with one short sentence confirming you are online."),
                textMessage("user", "Ping. Reply briefly that FieldGuard can reach you.")
            )
            val response = chat(messages, tools = null, toolChoice = null)
            val latency = System.currentTimeMillis() - start
            val reply = extractAssistantText(response)
                ?: throw LlmException("Empty model reply")
            ConnectionTestResult(
                ok = true,
                reply = reply,
                latencyMs = latency,
                model = response.model ?: model,
                baseUrl = configuredBaseUrl()
            )
        } catch (e: Exception) {
            ConnectionTestResult(
                ok = false,
                reply = "",
                latencyMs = System.currentTimeMillis() - start,
                model = model,
                baseUrl = configuredBaseUrl(),
                error = e.message ?: e.javaClass.simpleName
            )
        }
    }

    fun chat(
        messages: List<ChatMessage>,
        tools: List<ToolDefinition>?,
        toolChoice: String? = "auto"
    ): ChatCompletionResponse {
        val endpoint = configuredBaseUrl() + "/chat/completions"
        val body = ChatCompletionRequest(
            model = model,
            messages = messages,
            tools = tools,
            toolChoice = if (tools.isNullOrEmpty()) null else toolChoice,
            temperature = 0.2
        )
        val json = gson.toJson(body)
        val builder = Request.Builder()
            .url(endpoint)
            .post(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
        if (apiKey.isNotBlank()) {
            builder.header("Authorization", "Bearer $apiKey")
        }
        val httpResponse = client.newCall(builder.build()).execute()
        val raw = httpResponse.body?.string().orEmpty()
        if (!httpResponse.isSuccessful) {
            throw LlmException("HTTP ${httpResponse.code}: ${raw.take(500).ifBlank { httpResponse.message }}")
        }
        val parsed = gson.fromJson(raw, ChatCompletionResponse::class.java)
            ?: throw LlmException("Could not parse model response")
        if (parsed.error != null) {
            throw LlmException(parsed.error.message ?: "Model API error")
        }
        if (parsed.choices.isNullOrEmpty()) {
            throw LlmException("Model returned no choices")
        }
        return parsed
    }

    companion object {
        fun textMessage(role: String, text: String): ChatMessage {
            val el = JsonParser.parseString(Gson().toJson(text))
            return ChatMessage(role = role, content = el)
        }

        fun multimodalUserMessage(text: String, imageDataUrls: List<String>): ChatMessage {
            val parts = JsonArray()
            val textPart = JsonObject()
            textPart.addProperty("type", "text")
            textPart.addProperty("text", text)
            parts.add(textPart)
            imageDataUrls.forEach { url ->
                val imagePart = JsonObject()
                imagePart.addProperty("type", "image_url")
                val imageUrl = JsonObject()
                imageUrl.addProperty("url", url)
                imagePart.add("image_url", imageUrl)
                parts.add(imagePart)
            }
            return ChatMessage(role = "user", content = parts)
        }

        fun toolResultMessage(toolCallId: String, name: String, resultJson: String): ChatMessage {
            return ChatMessage(
                role = "tool",
                toolCallId = toolCallId,
                name = name,
                content = JsonParser.parseString(Gson().toJson(resultJson))
            )
        }

        fun extractAssistantText(response: ChatCompletionResponse): String? {
            val content = response.choices?.firstOrNull()?.message?.content ?: return null
            return when {
                content.isJsonPrimitive -> content.asString
                content.isJsonArray -> {
                    content.asJsonArray.mapNotNull { el ->
                        if (el.isJsonObject && el.asJsonObject.has("text")) {
                            el.asJsonObject.get("text")?.asString
                        } else null
                    }.joinToString("\n").ifBlank { null }
                }
                else -> content.toString()
            }
        }

        fun looksLikeLocalServer(url: String): Boolean {
            val u = url.lowercase()
            return u.contains("localhost") ||
                u.contains("127.0.0.1") ||
                u.contains("10.0.2.2") ||
                Regex("""https?://(10\.|192\.168\.|172\.(1[6-9]|2\d|3[0-1])\.)""").containsMatchIn(u)
        }
    }
}

class LlmException(message: String) : Exception(message)
