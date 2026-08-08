package com.project.farmingapp.supabase

/**
 * Lightweight stand-in for Firestore DocumentSnapshot so existing adapters/UI
 * can keep calling get()/getString()/id without Firebase types.
 */
class RemoteDocument(
    val id: String,
    private val fields: Map<String, Any?> = emptyMap()
) {
    val data: Map<String, Any?> get() = fields

    fun exists(): Boolean = fields.isNotEmpty() || id.isNotBlank()

    fun get(field: String): Any? = fields[field]

    fun getString(field: String): String? {
        val value = fields[field] ?: return null
        return value.toString()
    }

    fun getLong(field: String): Long? {
        val value = fields[field] ?: return null
        return when (value) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull()
            else -> null
        }
    }

    fun getDouble(field: String): Double? {
        val value = fields[field] ?: return null
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }
    }

    fun getBoolean(field: String): Boolean? {
        val value = fields[field] ?: return null
        return when (value) {
            is Boolean -> value
            is String -> value.equals("true", ignoreCase = true)
            else -> null
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getList(field: String): List<T>? = fields[field] as? List<T>

    companion object {
        fun fromRow(row: Map<String, Any?>, idKey: String = "id"): RemoteDocument {
            val aliased = FieldAlias.withAliases(row)
            val id = aliased[idKey]?.toString()
                ?: aliased["email"]?.toString()
                ?: aliased["remote_id"]?.toString()
                ?: aliased["remoteId"]?.toString()
                ?: ""
            return RemoteDocument(id, aliased)
        }
    }
}
