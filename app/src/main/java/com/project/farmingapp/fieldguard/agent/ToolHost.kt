package com.project.farmingapp.fieldguard.agent

import android.content.Context
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.project.farmingapp.fieldguard.data.FieldGuardIncident
import com.project.farmingapp.fieldguard.data.FieldGuardRepository
import com.project.farmingapp.fieldguard.data.IncidentStatus
import com.project.farmingapp.fieldguard.scenario.DemoScenario
import com.project.farmingapp.fieldguard.work.QueuedAlertWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Executes validated FieldGuard tools against scenario state + Room + notifications.
 */
class ToolHost(
    private val context: Context,
    private val repository: FieldGuardRepository,
    private val cooldown: CooldownManager,
    private var scenario: DemoScenario?,
    private var frameIndex: Int = 0,
    private var cameraImageDataUrl: String? = null,
    private var forceNetworkDown: Boolean = false,
    private val onIndicator: (NodeIndicatorState) -> Unit = {},
    private val onFrame: (String?) -> Unit = {}
) {
    private val gson = Gson()
    private var lastStatus: String = IncidentStatus.NO_ACTION.name
    private var alertSentThisIncident = false
    var lastRecordedId: Long = -1L
        private set

    fun currentStatus(): String = lastStatus

    suspend fun execute(name: String, argumentsJson: String): String {
        ToolSchemas.rejectUnknown(name)?.let { return errorJson(it) }
        val args = parseArgs(argumentsJson)
        return when (name) {
            "get_next_frame" -> getNextFrame()
            "get_sensor_readings" -> getSensorReadings()
            "record_incident" -> recordIncident(args)
            "send_alert" -> sendAlert(args)
            "trigger_field_indicator" -> triggerIndicator(args)
            "queue_alert" -> queueAlert(args)
            "check_cooldown" -> checkCooldown(args)
            "suggest_next_steps" -> suggestNextSteps(args)
            else -> errorJson("Rejected unknown tool: $name")
        }
    }

    private fun getNextFrame(): String {
        val sc = scenario
        if (cameraImageDataUrl != null && sc == null) {
            onFrame(cameraImageDataUrl)
            return gson.toJson(
                mapOf(
                    "ok" to true,
                    "frame_index" to 0,
                    "total_frames" to 1,
                    "note" to "Using camera capture",
                    "image_data_url_present" to true
                )
            )
        }
        if (sc == null) return errorJson("No scenario frames available")
        if (frameIndex >= sc.frames.lastIndex) {
            val path = sc.frames.last()
            onFrame(assetToDataUrl(path))
            return gson.toJson(
                mapOf(
                    "ok" to true,
                    "frame_index" to frameIndex,
                    "total_frames" to sc.frames.size,
                    "note" to "Already at last frame; returning current frame again",
                    "asset" to path
                )
            )
        }
        frameIndex += 1
        val path = sc.frames[frameIndex]
        onFrame(assetToDataUrl(path))
        return gson.toJson(
            mapOf(
                "ok" to true,
                "frame_index" to frameIndex,
                "total_frames" to sc.frames.size,
                "asset" to path,
                "clearer" to (frameIndex > 0)
            )
        )
    }

    private fun getSensorReadings(): String {
        val sc = scenario
        val overrides = sc?.sensorOverrides.orEmpty()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val reading = SensorReading(
            temperatureC = (overrides["temperature_c"] as? Number)?.toDouble() ?: 27.2,
            humidityPct = (overrides["humidity_pct"] as? Number)?.toDouble() ?: 76.0,
            pirMotion = overrides["pir_motion"] as? Boolean ?: true,
            lat = 7.8731,
            lng = 80.7718,
            timestampIso = sdf.format(Date())
        )
        return gson.toJson(
            mapOf(
                "ok" to true,
                "node" to "Agri Lanka IoT node (simulated)",
                "temperature_c" to reading.temperatureC,
                "humidity_pct" to reading.humidityPct,
                "pir_motion" to reading.pirMotion,
                "gps" to mapOf("lat" to reading.lat, "lng" to reading.lng),
                "timestamp" to reading.timestampIso,
                "night" to (sc?.night ?: false)
            )
        )
    }

    private suspend fun recordIncident(args: JsonObject): String {
        val type = args.str("type") ?: return errorJson("Missing type")
        val evidence = args.str("evidence_summary") ?: return errorJson("Missing evidence_summary")
        val confidence = args.num("model_confidence_estimate")
            ?: return errorJson("Missing model_confidence_estimate")
        val severity = args.str("severity") ?: return errorJson("Missing severity")
        val status = when {
            alertSentThisIncident -> IncidentStatus.ALERTED.name
            confidence < 0.55 -> IncidentStatus.UNCERTAIN.name
            type.contains("farmer", true) || type.contains("human", true) -> IncidentStatus.NO_ACTION.name
            else -> IncidentStatus.UNCERTAIN.name
        }
        lastStatus = status
        val id = repository.recordIncident(
            FieldGuardIncident(
                type = type,
                evidenceSummary = evidence,
                modelConfidenceEstimate = confidence,
                severity = severity,
                status = status,
                scenarioId = scenario?.id.orEmpty(),
                decisionSummary = "${SafetyRules.confidenceLabel(confidence)}. $evidence"
            )
        )
        lastRecordedId = id
        return gson.toJson(
            mapOf(
                "ok" to true,
                "incident_id" to id,
                "status" to status,
                "confidence_label" to SafetyRules.confidenceLabel(confidence)
            )
        )
    }

    private suspend fun sendAlert(args: JsonObject): String {
        val level = args.str("level") ?: return errorJson("Missing level")
        var message = args.str("message") ?: return errorJson("Missing message")
        if (SafetyRules.rejectsHarmfulIntent(message)) {
            return errorJson("Blocked: tools must not harm or chase animals")
        }
        val typeHint = if (level.equals("wildlife", true) || message.contains("elephant", true)) {
            "elephant"
        } else level

        if (cooldown.isInCooldown(typeHint) || (alertSentThisIncident && cooldown.isInCooldown(typeHint))) {
            return gson.toJson(
                mapOf(
                    "ok" to false,
                    "delivered" to false,
                    "reason" to "cooldown_active",
                    "remaining_ms" to cooldown.remainingMs(typeHint)
                )
            )
        }

        if (SafetyRules.isWildlifeAlertType(typeHint) || message.contains("elephant", true)) {
            message = SafetyRules.elephantAlertMessage(message)
        }

        val result = FieldGuardNotifier.send(
            context,
            level,
            message,
            forceFail = forceNetworkDown
        )
        if (forceNetworkDown) {
            // Only fail the first send_alert in network_down demo
            forceNetworkDown = false
        }
        if (result == "delivered") {
            cooldown.recordAlert(typeHint)
            alertSentThisIncident = true
            lastStatus = IncidentStatus.ALERTED.name
        }
        return gson.toJson(
            mapOf(
                "ok" to (result == "delivered"),
                "result" to result,
                "message_sent" to message
            )
        )
    }

    private fun triggerIndicator(args: JsonObject): String {
        val pattern = args.str("pattern") ?: return errorJson("Missing pattern")
        val state = when (pattern) {
            "alert_red" -> NodeIndicatorState(pattern, "#E53935", true)
            "warn_amber" -> NodeIndicatorState(pattern, "#FB8C00", true)
            "ok_green" -> NodeIndicatorState(pattern, "#43A047", false)
            else -> NodeIndicatorState("idle", "#5DAE3A", false)
        }
        onIndicator(state)
        return gson.toJson(mapOf("ok" to true, "pattern" to state.pattern, "simulated" to true))
    }

    private suspend fun queueAlert(args: JsonObject): String {
        var message = args.str("message") ?: return errorJson("Missing message")
        if (SafetyRules.rejectsHarmfulIntent(message)) {
            return errorJson("Blocked: tools must not harm or chase animals")
        }
        if (message.contains("elephant", true) || SafetyRules.isWildlifeAlertType(message)) {
            message = SafetyRules.elephantAlertMessage(message)
        }
        val id = repository.queueAlert(message, level = "wildlife")
        lastStatus = IncidentStatus.QUEUED.name
        QueuedAlertWorker.enqueue(context, delaySeconds = 8)
        return gson.toJson(
            mapOf(
                "ok" to true,
                "queued_id" to id,
                "status" to "queued",
                "retry" to "WorkManager scheduled"
            )
        )
    }

    private fun checkCooldown(args: JsonObject): String {
        val type = args.str("type") ?: return errorJson("Missing type")
        val active = cooldown.isInCooldown(type)
        return gson.toJson(
            mapOf(
                "ok" to true,
                "type" to type,
                "in_cooldown" to active,
                "remaining_ms" to cooldown.remainingMs(type),
                "window_ms" to SafetyRules.COOLDOWN_MS
            )
        )
    }

    private fun suggestNextSteps(args: JsonObject): String {
        val incident = args.str("incident").orEmpty()
        val links = mutableListOf<Map<String, String>>()
        if (incident.contains("crop", true) || incident.contains("damage", true) || incident.contains("leaf", true)) {
            links += mapOf(
                "screen" to "leaf_scanner",
                "label" to "Open Leaf Scanner",
                "route" to "LeafScannerFragment"
            )
        }
        links += mapOf(
            "screen" to "community_feed",
            "label" to "Warn neighbours in Community Network",
            "route" to "SocialMediaPostsFragment"
        )
        return gson.toJson(mapOf("ok" to true, "navigation_only" to true, "links" to links))
    }

    fun currentImageDataUrl(): String? {
        cameraImageDataUrl?.let { return it }
        val sc = scenario ?: return null
        val path = sc.frames.getOrNull(frameIndex) ?: return null
        return assetToDataUrl(path)
    }

    private fun assetToDataUrl(assetPath: String): String? {
        return try {
            context.assets.open(assetPath).use { input ->
                val bytes = input.readBytes()
                val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                "data:image/png;base64,$b64"
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseArgs(raw: String): JsonObject {
        if (raw.isBlank()) return JsonObject()
        return try {
            JsonParser.parseString(raw).asJsonObject
        } catch (_: Exception) {
            JsonObject()
        }
    }

    private fun JsonObject.str(key: String): String? =
        if (has(key) && get(key).isJsonPrimitive) get(key).asString else null

    private fun JsonObject.num(key: String): Double? =
        if (has(key) && get(key).isJsonPrimitive) get(key).asDouble else null

    private fun errorJson(msg: String): String = gson.toJson(mapOf("ok" to false, "error" to msg))
}
