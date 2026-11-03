package com.project.farmingapp.fieldguard.agent

import com.project.farmingapp.fieldguard.llm.ToolDefinition
import com.project.farmingapp.fieldguard.llm.ToolFunction

object ToolSchemas {
    val KNOWN_TOOLS = setOf(
        "get_next_frame",
        "get_sensor_readings",
        "record_incident",
        "send_alert",
        "trigger_field_indicator",
        "queue_alert",
        "check_cooldown",
        "suggest_next_steps"
    )

    fun all(): List<ToolDefinition> = listOf(
        tool(
            "get_next_frame",
            "Fetch the next camera frame for the current field-node scenario. Call when the current image is unclear.",
            emptyObject()
        ),
        tool(
            "get_sensor_readings",
            "Read simulated Agri Lanka IoT node sensors: temperature, humidity, PIR motion, GPS, timestamp.",
            emptyObject()
        ),
        tool(
            "record_incident",
            "Persist an evidence-based incident record. Prefer recording uncertain events over alerting.",
            mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "type" to mapOf("type" to "string", "description" to "Incident type, e.g. elephant, ambiguous_motion, farmer, other"),
                    "evidence_summary" to mapOf("type" to "string"),
                    "model_confidence_estimate" to mapOf(
                        "type" to "number",
                        "description" to "Model's estimate of confidence 0-1 — never claim as accuracy"
                    ),
                    "severity" to mapOf("type" to "string", "enum" to listOf("low", "medium", "high", "critical"))
                ),
                "required" to listOf("type", "evidence_summary", "model_confidence_estimate", "severity")
            )
        ),
        tool(
            "send_alert",
            "Send a real Android notification. For wildlife/elephant alerts the app appends mandatory safety text. Cannot harm or chase animals.",
            mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "level" to mapOf("type" to "string", "enum" to listOf("info", "warning", "wildlife")),
                    "message" to mapOf("type" to "string")
                ),
                "required" to listOf("level", "message")
            )
        ),
        tool(
            "trigger_field_indicator",
            "Flash the on-screen Simulated Agri Lanka IoT node panel (LED colour + buzzer). Patterns: idle, alert_red, warn_amber, ok_green.",
            mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "pattern" to mapOf(
                        "type" to "string",
                        "enum" to listOf("idle", "alert_red", "warn_amber", "ok_green")
                    )
                ),
                "required" to listOf("pattern")
            )
        ),
        tool(
            "queue_alert",
            "Queue an alert for later delivery when the network is down. Stored in Room and retried with WorkManager.",
            mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "message" to mapOf("type" to "string")
                ),
                "required" to listOf("message")
            )
        ),
        tool(
            "check_cooldown",
            "Check whether an alert of this type was already sent within the last 10 minutes.",
            mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "type" to mapOf("type" to "string")
                ),
                "required" to listOf("type")
            )
        ),
        tool(
            "suggest_next_steps",
            "Optional: suggest navigation links into existing Agri Lanka screens (leaf scanner, community feed). Navigation only.",
            mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "incident" to mapOf("type" to "string")
                ),
                "required" to listOf("incident")
            )
        )
    )

    fun rejectUnknown(name: String): String? {
        if (name !in KNOWN_TOOLS) {
            return "Rejected unknown tool: $name. Allowed: ${KNOWN_TOOLS.joinToString()}"
        }
        return null
    }

    private fun emptyObject(): Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to emptyMap<String, Any>()
    )

    private fun tool(name: String, description: String, parameters: Map<String, Any>): ToolDefinition {
        return ToolDefinition(
            type = "function",
            function = ToolFunction(name = name, description = description, parameters = parameters)
        )
    }
}
