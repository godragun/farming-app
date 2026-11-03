package com.project.farmingapp.fieldguard

import com.project.farmingapp.fieldguard.agent.CooldownManager
import com.project.farmingapp.fieldguard.agent.SafetyRules
import com.project.farmingapp.fieldguard.agent.ToolSchemas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldGuardSafetyTest {

    @Test
    fun cooldown_blocks_duplicate_within_10_minutes() {
        var now = 1_000_000L
        val cooldown = CooldownManager(clock = { now })
        assertFalse(cooldown.isInCooldown("elephant"))
        cooldown.recordAlert("elephant")
        assertTrue(cooldown.isInCooldown("elephant"))
        now += SafetyRules.COOLDOWN_MS - 1
        assertTrue(cooldown.isInCooldown("elephant"))
        now += 2
        assertFalse(cooldown.isInCooldown("elephant"))
    }

    @Test
    fun unknown_tool_is_rejected() {
        assertNotNull(ToolSchemas.rejectUnknown("explode_drone"))
        assertNull(ToolSchemas.rejectUnknown("get_next_frame"))
        assertNull(ToolSchemas.rejectUnknown("send_alert"))
    }

    @Test
    fun elephant_alert_includes_mandatory_safety_text() {
        val msg = SafetyRules.elephantAlertMessage("Elephant near field boundary.")
        assertTrue(msg.contains(SafetyRules.ELEPHANT_SAFETY_TEXT))
        assertTrue(msg.contains("Do not approach"))
        // Idempotent if already present
        val again = SafetyRules.elephantAlertMessage(msg)
        assertEquals(1, again.split(SafetyRules.ELEPHANT_SAFETY_TEXT).size - 1)
    }

    @Test
    fun confidence_label_never_says_accuracy() {
        val label = SafetyRules.confidenceLabel(0.82)
        assertTrue(label.contains("model's estimate"))
        assertFalse(label.contains("accuracy", ignoreCase = true))
    }
}
