package com.project.farmingapp.fieldguard.iot

import com.project.farmingapp.fieldguard.data.IotReading
import java.util.Calendar
import java.util.Random
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Generates repeatable simulated IoT readings every [INTERVAL_MS] (15 simulated minutes).
 * Daily cycle: cooler / more humid nights. Patterns reshape climate for demos.
 */
class NodeSimulator(
    private val timeZone: TimeZone = TimeZone.getTimeZone("Asia/Colombo")
) {
    fun generateHistory(
        endMs: Long = System.currentTimeMillis(),
        days: Int = RETENTION_DAYS,
        pattern: IotDemoPattern = IotDemoPattern.NORMAL,
        offlineNodeId: String? = DemoNodes.ALL.firstOrNull()?.nodeId
    ): List<IotReading> {
        val startMs = alignToInterval(endMs - days * DAY_MS)
        val endAligned = alignToInterval(endMs)
        val out = ArrayList<IotReading>((days * 24 * 4) * DemoNodes.ALL.size)
        var t = startMs
        while (t <= endAligned) {
            DemoNodes.ALL.forEach { node ->
                out += readingAt(node, t, pattern, offlineNodeId)
            }
            t += INTERVAL_MS
        }
        return out
    }

    fun readingAt(
        node: DemoNode,
        timestampMs: Long,
        pattern: IotDemoPattern = IotDemoPattern.NORMAL,
        offlineNodeId: String? = null
    ): IotReading {
        val rnd = seededRandom(node.nodeId, timestampMs)
        val hour = hourOfDay(timestampMs)
        val night = hour < 6 || hour >= 19

        // Base diurnal cycle: cooler + more humid at night
        val tempSwing = 5.5 * cos((hour - 14) / 24.0 * 2.0 * PI)
        var temp = node.baseTempC + tempSwing + (rnd.nextDouble() - 0.5) * 1.2
        var humidity = node.baseHumidityPct - tempSwing * 2.2 + (rnd.nextDouble() - 0.5) * 4.0

        when (pattern) {
            IotDemoPattern.NORMAL -> Unit
            IotDemoPattern.HUMID_COOL_NIGHTS -> {
                if (night) {
                    temp = 17.0 + rnd.nextDouble() * 2.8 // ~17–20 °C blast-favourable
                    humidity = 92.0 + rnd.nextDouble() * 6.0
                } else {
                    temp = node.baseTempC + 1.0 + rnd.nextDouble()
                    humidity = (humidity + 8.0).coerceAtMost(96.0)
                }
            }
            IotDemoPattern.DRY_HOT_WEEK -> {
                temp = node.baseTempC + 4.0 + sin(hour / 24.0 * 2.0 * PI) * 3.0 + rnd.nextDouble()
                humidity = (45.0 + rnd.nextDouble() * 12.0).coerceIn(35.0, 62.0)
            }
            IotDemoPattern.NODE_OFFLINE -> Unit
            IotDemoPattern.MOTION_AT_NIGHT -> {
                if (night) humidity = (humidity + 5.0).coerceAtMost(98.0)
            }
        }

        temp = temp.coerceIn(12.0, 42.0)
        humidity = humidity.coerceIn(20.0, 100.0)

        val motion = when {
            pattern == IotDemoPattern.MOTION_AT_NIGHT && night && node.nearForest ->
                rnd.nextDouble() < 0.35
            pattern == IotDemoPattern.MOTION_AT_NIGHT && night ->
                rnd.nextDouble() < 0.18
            night && node.nearForest ->
                rnd.nextDouble() < 0.08
            else ->
                rnd.nextDouble() < 0.03
        }

        val offline = pattern == IotDemoPattern.NODE_OFFLINE &&
            offlineNodeId != null &&
            node.nodeId == offlineNodeId &&
            timestampMs >= (System.currentTimeMillis() - 6 * HOUR_MS)

        val ageHours = ((System.currentTimeMillis() - timestampMs).coerceAtLeast(0) / HOUR_MS).toInt()
        val battery = if (offline) {
            (12 + rnd.nextInt(8))
        } else {
            (94 - (ageHours / 18) - rnd.nextInt(4)).coerceIn(15, 100)
        }

        val link = when {
            offline -> "offline"
            node.linkTypeDefault.startsWith("mesh") -> node.linkTypeDefault
            else -> "Wi-Fi"
        }

        return IotReading(
            nodeId = node.nodeId,
            timestampMs = timestampMs,
            temperatureC = (temp * 10).roundToInt() / 10.0,
            humidityPct = (humidity * 10).roundToInt() / 10.0,
            pirMotion = !offline && motion,
            lat = node.lat + (rnd.nextDouble() - 0.5) * 0.002,
            lng = node.lng + (rnd.nextDouble() - 0.5) * 0.002,
            batteryPct = battery,
            linkType = link,
            online = !offline,
            simulated = true
        )
    }

    private fun hourOfDay(timestampMs: Long): Int {
        val cal = Calendar.getInstance(timeZone)
        cal.timeInMillis = timestampMs
        return cal.get(Calendar.HOUR_OF_DAY)
    }

    private fun seededRandom(nodeId: String, timestampMs: Long): Random {
        val seed = nodeId.hashCode().toLong() * 31L + alignToInterval(timestampMs)
        return Random(seed)
    }

    companion object {
        const val INTERVAL_MS = 15 * 60 * 1000L
        const val RETENTION_DAYS = 7
        const val DAY_MS = 24 * 60 * 60 * 1000L
        const val HOUR_MS = 60 * 60 * 1000L

        fun alignToInterval(ms: Long): Long = ms - (ms % INTERVAL_MS)
    }
}
