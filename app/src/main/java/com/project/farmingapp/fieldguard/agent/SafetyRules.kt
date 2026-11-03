package com.project.farmingapp.fieldguard.agent

/**
 * Safety rules enforced in code (not only in the system prompt).
 */
object SafetyRules {
    const val COOLDOWN_MS = 10 * 60 * 1000L

    const val ELEPHANT_SAFETY_TEXT =
        "Do not approach. Stay indoors or at a safe distance, warn neighbours, contact your local wildlife office."

    private val HARMFUL_ACTION_KEYWORDS = listOf(
        "chase", "harm", "shoot", "hit", "scare with fire", "poison", "trap animal", "drive away aggressively"
    )

    fun elephantAlertMessage(baseMessage: String): String {
        val trimmed = baseMessage.trim()
        return if (trimmed.contains(ELEPHANT_SAFETY_TEXT, ignoreCase = true)) {
            trimmed
        } else {
            listOf(trimmed, ELEPHANT_SAFETY_TEXT).filter { it.isNotBlank() }.joinToString(" ")
        }
    }

    fun isWildlifeAlertType(type: String): Boolean {
        val t = type.lowercase()
        return t.contains("elephant") || t.contains("wildlife") || t.contains("animal")
    }

    fun rejectsHarmfulIntent(message: String): Boolean {
        val lower = message.lowercase()
        return HARMFUL_ACTION_KEYWORDS.any { lower.contains(it) }
    }

    /** Confidence must be labelled as the model's estimate — never as accuracy. */
    fun confidenceLabel(estimate: Double): String {
        val pct = (estimate.coerceIn(0.0, 1.0) * 100).toInt()
        return "model's estimate: $pct%"
    }
}
