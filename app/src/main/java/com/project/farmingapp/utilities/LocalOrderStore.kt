package com.project.farmingapp.utilities

import android.content.Context
import org.json.JSONObject

/**
 * Guest / offline fallback for orders (mirrors LocalCartStore).
 */
object LocalOrderStore {
    private const val PREFS = "local_order_store"
    private const val KEY_ORDERS = "orders_json"

    fun add(context: Context, orderId: String, fields: Map<String, Any?>) {
        if (orderId.isBlank()) return
        val root = readRoot(context)
        val item = JSONObject()
        fields.forEach { (k, v) ->
            item.put(k, v ?: JSONObject.NULL)
        }
        root.put(orderId, item)
        writeRoot(context, root)
    }

    fun getAll(context: Context): HashMap<String, Any> {
        val root = readRoot(context)
        val orders = hashMapOf<String, Any>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val entry = root.optJSONObject(key) ?: continue
            val itemMap = hashMapOf<String, Any>()
            val entryKeys = entry.keys()
            while (entryKeys.hasNext()) {
                val ek = entryKeys.next()
                itemMap[ek] = entry.get(ek)
            }
            orders[key] = itemMap
        }
        return orders
    }

    private fun readRoot(context: Context): JSONObject {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ORDERS, "{}")
            .orEmpty()
        return try {
            JSONObject(raw)
        } catch (_: Exception) {
            JSONObject()
        }
    }

    private fun writeRoot(context: Context, root: JSONObject) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ORDERS, root.toString())
            .apply()
    }
}
