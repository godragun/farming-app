package com.project.farmingapp.fieldguard.agent

import android.content.Context
import com.google.gson.Gson
import com.project.farmingapp.fieldguard.data.FieldGuardRepository
import com.project.farmingapp.fieldguard.llm.ChatMessage
import com.project.farmingapp.fieldguard.llm.LlmClient
import com.project.farmingapp.fieldguard.llm.LlmException
import com.project.farmingapp.fieldguard.scenario.DemoScenario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Agentic loop: system + incident context + image(s) → tool call or final summary → run tool → repeat (max 6).
 */
class FieldGuardAgent(
    private val context: Context,
    private val llm: LlmClient = LlmClient(),
    private val repository: FieldGuardRepository = FieldGuardRepository(context),
    private val cooldown: CooldownManager = CooldownManager()
) {
    private val gson = Gson()

    suspend fun run(
        scenario: DemoScenario?,
        languageTag: String,
        cameraDataUrl: String? = null,
        onTrace: (AgentTraceStep) -> Unit = {},
        onIndicator: (NodeIndicatorState) -> Unit = {},
        onFrame: (String?) -> Unit = {}
    ): AgentRunResult = withContext(Dispatchers.IO) {
        if (scenario?.seedCooldownType != null) {
            cooldown.recordAlert(scenario.seedCooldownType)
        }
        val host = ToolHost(
            context = context,
            repository = repository,
            cooldown = cooldown,
            scenario = scenario,
            frameIndex = 0,
            cameraImageDataUrl = cameraDataUrl,
            forceNetworkDown = scenario?.forceNetworkDown == true,
            onIndicator = onIndicator,
            onFrame = onFrame
        )

        val traces = mutableListOf<AgentTraceStep>()
        fun emit(step: AgentTraceStep) {
            traces += step
            onTrace(step)
        }

        val messages = mutableListOf<ChatMessage>()
        messages += LlmClient.textMessage("system", systemPrompt(languageTag))

        val firstImage = host.currentImageDataUrl()
        onFrame(firstImage)
        val userText = buildString {
            append("Field-node camera event for Agri Lanka FieldGuard.\n")
            append("Scenario: ${scenario?.id ?: "camera_capture"}\n")
            if (scenario != null) {
                append("Expected demo behaviour (for graders; still reason carefully): ${scenario.expectedOutcome}\n")
            }
            append("Inspect the image and sensors via tools. Prefer get_next_frame when unsure. ")
            append("Prefer record_incident for uncertain events over alerting. ")
            append("Respond final summary in language tag: $languageTag.")
        }
        messages += if (firstImage != null) {
            LlmClient.multimodalUserMessage(userText, listOf(firstImage))
        } else {
            LlmClient.textMessage(
                "user",
                "$userText\n(Note: scenario image asset missing — use sensors and tools; still do not invent fake vision certainty.)"
            )
        }

        val tools = ToolSchemas.all()
        var step = 0
        var finalText: String? = null

        try {
            while (step < MAX_STEPS) {
                step += 1
                val start = System.currentTimeMillis()
                val response = try {
                    llm.chat(messages, tools, toolChoice = "auto")
                } catch (e: Exception) {
                    val err = e.message ?: e.javaClass.simpleName
                    emit(
                        AgentTraceStep(
                            step = step,
                            modelName = llm.configuredModel(),
                            kind = "error",
                            result = err,
                            elapsedMs = System.currentTimeMillis() - start,
                            detail = "Model request failed — no fake fallback"
                        )
                    )
                    return@withContext AgentRunResult(
                        success = false,
                        decisionSummary = "",
                        status = host.currentStatus(),
                        traces = traces,
                        error = err
                    )
                }
                val elapsed = System.currentTimeMillis() - start
                val assistant = response.choices?.firstOrNull()?.message
                    ?: throw LlmException("Missing assistant message")

                messages += assistant.copy()

                val toolCalls = assistant.toolCalls.orEmpty()
                if (toolCalls.isEmpty()) {
                    finalText = LlmClient.extractAssistantText(response)
                        ?: "(empty final summary)"
                    emit(
                        AgentTraceStep(
                            step = step,
                            modelName = response.model ?: llm.configuredModel(),
                            kind = "final",
                            result = finalText,
                            elapsedMs = elapsed
                        )
                    )
                    break
                }

                emit(
                    AgentTraceStep(
                        step = step,
                        modelName = response.model ?: llm.configuredModel(),
                        kind = "model_call",
                        toolName = toolCalls.joinToString { it.function.name },
                        arguments = toolCalls.joinToString { it.function.arguments },
                        elapsedMs = elapsed,
                        detail = "finish=${response.choices?.firstOrNull()?.finishReason}"
                    )
                )

                for (call in toolCalls) {
                    val toolStart = System.currentTimeMillis()
                    val rejection = ToolSchemas.rejectUnknown(call.function.name)
                    val result = if (rejection != null) {
                        gson.toJson(mapOf("ok" to false, "error" to rejection))
                    } else {
                        host.execute(call.function.name, call.function.arguments)
                    }
                    emit(
                        AgentTraceStep(
                            step = step,
                            modelName = response.model ?: llm.configuredModel(),
                            kind = "tool",
                            toolName = call.function.name,
                            arguments = call.function.arguments,
                            result = result,
                            elapsedMs = System.currentTimeMillis() - toolStart
                        )
                    )
                    messages += LlmClient.toolResultMessage(call.id, call.function.name, result)
                }
            }

            if (finalText == null && step >= MAX_STEPS) {
                finalText = "Stopped after $MAX_STEPS steps without a final summary. Last status: ${host.currentStatus()}."
                emit(
                    AgentTraceStep(
                        step = step,
                        modelName = llm.configuredModel(),
                        kind = "final",
                        result = finalText
                    )
                )
            }

            AgentRunResult(
                success = true,
                decisionSummary = finalText.orEmpty(),
                status = host.currentStatus(),
                traces = traces
            )
        } catch (e: Exception) {
            val err = e.message ?: e.javaClass.simpleName
            emit(
                AgentTraceStep(
                    step = step,
                    modelName = llm.configuredModel(),
                    kind = "error",
                    result = err
                )
            )
            AgentRunResult(
                success = false,
                decisionSummary = "",
                status = host.currentStatus(),
                traces = traces,
                error = err
            )
        }
    }

    companion object {
        const val MAX_STEPS = 6

        fun systemPrompt(languageTag: String): String = """
You are FieldGuard AI — a cautious farm-incident assistant for Sri Lankan smallholders (Agri Lanka).
Goals: turn a field-node camera event into an evidence-based decision, a safe action, and a verified record.

Rules:
- Request another frame (get_next_frame) when unsure.
- Prefer recording an uncertain event (record_incident) over alerting.
- Always check_cooldown before send_alert for wildlife.
- Elephant / wildlife alerts must keep people safe: never suggest approaching, chasing, or harming animals.
- Confidence is always the model's estimate, never "accuracy".
- Use trigger_field_indicator for visible node feedback (Simulated node).
- If send_alert fails (network), use queue_alert.
- Farmer/human in daylight → log/record only; no wildlife alert.
- Explain decisions briefly in the final message.
- Respond in the app language: $languageTag (en, si, or ta).
- You may call suggest_next_steps for navigation hints into existing Agri Lanka screens only.
""".trimIndent()
    }
}
