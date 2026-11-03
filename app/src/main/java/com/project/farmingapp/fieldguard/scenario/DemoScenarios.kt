package com.project.farmingapp.fieldguard.scenario

data class DemoScenario(
    val id: String,
    val titleRes: String,
    val descriptionRes: String,
    /** Asset paths relative to assets/ */
    val frames: List<String>,
    val expectedOutcome: String,
    /** When true, send_alert is forced to fail once so queue_alert can be demoed. */
    val forceNetworkDown: Boolean = false,
    /** Pre-seed cooldown for duplicate demo. */
    val seedCooldownType: String? = null,
    val night: Boolean = false,
    val sensorOverrides: Map<String, Any> = emptyMap()
)

object DemoScenarios {
    const val CAMERA = "camera"

    val ALL: List<DemoScenario> = listOf(
        DemoScenario(
            id = "elephant_night",
            titleRes = "fieldguard_scenario_elephant_title",
            descriptionRes = "fieldguard_scenario_elephant_desc",
            frames = listOf(
                "fieldguard/scenarios/elephant_night/frame1.png",
                "fieldguard/scenarios/elephant_night/frame2.png"
            ),
            expectedOutcome = "alert + indicator + record",
            night = true,
            sensorOverrides = mapOf(
                "pir_motion" to true,
                "temperature_c" to 24.5,
                "humidity_pct" to 88.0
            )
        ),
        DemoScenario(
            id = "ambiguous_movement",
            titleRes = "fieldguard_scenario_ambiguous_title",
            descriptionRes = "fieldguard_scenario_ambiguous_desc",
            frames = listOf(
                "fieldguard/scenarios/ambiguous_movement/frame1.png",
                "fieldguard/scenarios/ambiguous_movement/frame2.png",
                "fieldguard/scenarios/ambiguous_movement/frame3.png"
            ),
            expectedOutcome = "more frames → record uncertain, no alert",
            night = true,
            sensorOverrides = mapOf("pir_motion" to true)
        ),
        DemoScenario(
            id = "farmer_daylight",
            titleRes = "fieldguard_scenario_farmer_title",
            descriptionRes = "fieldguard_scenario_farmer_desc",
            frames = listOf(
                "fieldguard/scenarios/farmer_daylight/frame1.png"
            ),
            expectedOutcome = "log only, no wildlife alert",
            night = false,
            sensorOverrides = mapOf("pir_motion" to true, "temperature_c" to 31.0)
        ),
        DemoScenario(
            id = "network_down",
            titleRes = "fieldguard_scenario_network_title",
            descriptionRes = "fieldguard_scenario_network_desc",
            frames = listOf(
                "fieldguard/scenarios/network_down/frame1.png",
                "fieldguard/scenarios/network_down/frame2.png"
            ),
            expectedOutcome = "send_alert fails → queue_alert → retry",
            forceNetworkDown = true,
            night = true,
            sensorOverrides = mapOf("pir_motion" to true)
        ),
        DemoScenario(
            id = "duplicate_cooldown",
            titleRes = "fieldguard_scenario_duplicate_title",
            descriptionRes = "fieldguard_scenario_duplicate_desc",
            frames = listOf(
                "fieldguard/scenarios/duplicate_cooldown/frame1.png"
            ),
            expectedOutcome = "check_cooldown → no second alert",
            seedCooldownType = "elephant",
            night = true,
            sensorOverrides = mapOf("pir_motion" to true)
        )
    )

    fun byId(id: String): DemoScenario? = ALL.find { it.id == id }
}
