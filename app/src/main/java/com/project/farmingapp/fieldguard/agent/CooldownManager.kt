package com.project.farmingapp.fieldguard.agent

/**
 * In-memory + optional SharedPreferences-backed cooldown: one alert per type within 10 minutes.
 */
class CooldownManager(
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val cooldownMs: Long = SafetyRules.COOLDOWN_MS
) {
    private val lastAlertAt = mutableMapOf<String, Long>()

    fun recordAlert(type: String, at: Long = clock()) {
        lastAlertAt[normalize(type)] = at
    }

    fun isInCooldown(type: String, now: Long = clock()): Boolean {
        val last = lastAlertAt[normalize(type)] ?: return false
        return now - last < cooldownMs
    }

    fun remainingMs(type: String, now: Long = clock()): Long {
        val last = lastAlertAt[normalize(type)] ?: return 0L
        return (cooldownMs - (now - last)).coerceAtLeast(0L)
    }

    fun clear() {
        lastAlertAt.clear()
    }

    fun hydrate(from: Map<String, Long>) {
        lastAlertAt.clear()
        lastAlertAt.putAll(from.mapKeys { normalize(it.key) })
    }

    fun snapshot(): Map<String, Long> = lastAlertAt.toMap()

    private fun normalize(type: String) = type.trim().lowercase()
}
