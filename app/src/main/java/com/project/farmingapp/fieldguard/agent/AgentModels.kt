package com.project.farmingapp.fieldguard.agent

data class AgentTraceStep(
    val step: Int,
    val modelName: String,
    val kind: String, // model_call | tool | final | error
    val toolName: String = "",
    val arguments: String = "",
    val result: String = "",
    val elapsedMs: Long = 0,
    val detail: String = ""
)

data class NodeIndicatorState(
    val pattern: String = "idle",
    val ledColor: String = "#5DAE3A",
    val buzzerOn: Boolean = false,
    val label: String = "Simulated node"
)

data class AgentRunResult(
    val success: Boolean,
    val decisionSummary: String,
    val status: String,
    val traces: List<AgentTraceStep>,
    val error: String? = null
)

data class SensorReading(
    val temperatureC: Double,
    val humidityPct: Double,
    val pirMotion: Boolean,
    val lat: Double,
    val lng: Double,
    val timestampIso: String
)
