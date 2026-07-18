package com.project.farmingapp.utilities

import android.content.Context
import org.json.JSONObject

object LocalCartStore {
    private const val PREFS = "local_cart_store"
    private const val KEY_CART = "cart_items_json"

    fun addOrUpdate(context: Context, itemId: String, quantity: Int, time: String) {
        if (itemId.isBlank()) return
        val root = readRoot(context)
        val item = JSONObject()
        item.put("quantity", quantity.coerceAtLeast(1))
        item.put("time", time)
        root.put(itemId, item)
        writeRoot(context, root)
    }

    fun remove(context: Context, itemId: String) {
        val root = readRoot(context)
        root.remove(itemId)
        writeRoot(context, root)
    }

    fun updateQuantity(context: Context, itemId: String, quantity: Int) {
        val root = readRoot(context)
        val existing = root.optJSONObject(itemId) ?: JSONObject()
        existing.put("quantity", quantity.coerceAtLeast(1))
        if (!existing.has("time")) {
            existing.put("time", System.currentTimeMillis().toString())
        }
        root.put(itemId, existing)
        writeRoot(context, root)
    }

    fun getAll(context: Context): HashMap<String, Any> {
        val root = readRoot(context)
        val cart = hashMapOf<String, Any>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val entry = root.optJSONObject(key) ?: continue
            val itemMap = hashMapOf<String, Any>()
            itemMap["quantity"] = entry.optInt("quantity", 1)
            itemMap["time"] = entry.optString("time", "")
            cart[key] = itemMap
        }
        return cart
    }

    private fun readRoot(context: Context): JSONObject {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_CART, "{}")
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
            .putString(KEY_CART, root.toString())
            .apply()
    }
}
