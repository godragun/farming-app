package com.project.farmingapp.fieldguard.iot

/**
 * Three fixed Agri Lanka demo IoT nodes (simulated). IDs are stable for Room + tools.
 */
data class DemoNode(
    val nodeId: String,
    val district: String,
    val cropHint: String,
    val titleRes: String,
    val descRes: String,
    val lat: Double,
    val lng: Double,
    val baseTempC: Double,
    val baseHumidityPct: Double,
    val linkTypeDefault: String,
    val nearForest: Boolean = false
)

object DemoNodes {
    val ALL: List<DemoNode> = listOf(
        DemoNode(
            nodeId = "node_polonnaruwa_paddy",
            district = "Polonnaruwa",
            cropHint = "paddy",
            titleRes = "fieldguard_iot_node_polonnaruwa_title",
            descRes = "fieldguard_iot_node_polonnaruwa_desc",
            lat = 7.9397,
            lng = 81.0006,
            baseTempC = 29.0,
            baseHumidityPct = 78.0,
            linkTypeDefault = "Wi-Fi"
        ),
        DemoNode(
            nodeId = "node_badulla_veg",
            district = "Badulla",
            cropHint = "vegetables",
            titleRes = "fieldguard_iot_node_badulla_title",
            descRes = "fieldguard_iot_node_badulla_desc",
            lat = 6.9934,
            lng = 81.0550,
            baseTempC = 24.5,
            baseHumidityPct = 82.0,
            linkTypeDefault = "mesh:2"
        ),
        DemoNode(
            nodeId = "node_anuradhapura_forest",
            district = "Anuradhapura",
            cropHint = "paddy",
            titleRes = "fieldguard_iot_node_anuradhapura_title",
            descRes = "fieldguard_iot_node_anuradhapura_desc",
            lat = 8.3114,
            lng = 80.4037,
            baseTempC = 30.0,
            baseHumidityPct = 72.0,
            linkTypeDefault = "mesh:1",
            nearForest = true
        )
    )

    fun byId(nodeId: String): DemoNode? = ALL.find { it.nodeId == nodeId }
}
