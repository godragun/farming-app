package com.project.farmingapp.supabase

/**
 * Maps between Android camelCase field names (legacy Firestore) and
 * Supabase snake_case columns so UI adapters keep working.
 */
object FieldAlias {

    private val pairs = listOf(
        "profile_image" to "profileImage",
        "back_image" to "backImage",
        "user_type" to "userType",
        "mob_no" to "mobNo",
        "user_id" to "userID",
        "time_stamp" to "timeStamp",
        "upload_type" to "uploadType",
        "image_url" to "imageUrl",
        "image_id" to "imageID",
        "short_desc" to "shortDesc",
        "long_desc" to "longDesc",
        "del_charge" to "delCharge",
        "market_prices" to "marketPrices",
        "remote_id" to "remoteId",
        "local_id" to "localId",
        "farmer_name" to "farmerName",
        "location_text" to "locationText",
        "crop_type" to "cropType",
        "soil_condition" to "soilCondition",
        "irrigation_type" to "irrigationType",
        "pest_notes" to "pestNotes",
        "estimated_yield_kg" to "estimatedYieldKg",
        "audio_url" to "audioUrl",
        "saved_at" to "savedAt",
        "synced_at" to "syncedAt",
        "product_id" to "productId",
        "time_ms" to "time"
    )

    fun withAliases(row: Map<String, Any?>): Map<String, Any?> {
        val out = row.toMutableMap()
        pairs.forEach { (snake, camel) ->
            if (out.containsKey(snake) && !out.containsKey(camel)) {
                out[camel] = out[snake]
            }
            if (out.containsKey(camel) && !out.containsKey(snake)) {
                out[snake] = out[camel]
            }
        }
        return out
    }

    fun toSnakeRow(fields: Map<String, Any?>): Map<String, Any?> {
        val out = linkedMapOf<String, Any?>()
        fields.forEach { (key, value) ->
            val snake = pairs.firstOrNull { it.second == key }?.first ?: key
            out[snake] = value
        }
        return out
    }
}
