package com.project.farmingapp.supabase

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Thin PostgREST helper for Supabase Database tables.
 */
object SupabaseDb {

    fun select(
        table: String,
        query: String = "",
        idKey: String = "id"
    ): List<RemoteDocument> {
        val url = buildString {
            append("${SupabaseConfig.restUrl}/$table")
            if (query.isNotBlank()) {
                append(if (query.startsWith("?")) query else "?$query")
            }
        }
        SupabaseHttp.get(url).use { response ->
            val text = SupabaseHttp.responseText(response)
            if (!response.isSuccessful) {
                throw IllegalStateException("Supabase select failed (${response.code}): $text")
            }
            val array = SupabaseHttp.parseArray(text) ?: JSONArray()
            return SupabaseHttp.jsonArrayToMaps(array).map { RemoteDocument.fromRow(it, idKey) }
        }
    }

    fun selectOne(
        table: String,
        query: String,
        idKey: String = "id"
    ): RemoteDocument? = select(table, query, idKey).firstOrNull()

    fun insert(table: String, row: JSONObject): RemoteDocument? {
        SupabaseHttp.postJson(
            url = "${SupabaseConfig.restUrl}/$table",
            body = row,
            headers = SupabaseHttp.restHeaders("return=representation")
        ).use { response ->
            val text = SupabaseHttp.responseText(response)
            if (!response.isSuccessful) {
                throw IllegalStateException("Supabase insert failed (${response.code}): $text")
            }
            val array = SupabaseHttp.parseArray(text)
            val obj = array?.optJSONObject(0) ?: SupabaseHttp.parseObject(text)
            return obj?.let { RemoteDocument.fromRow(SupabaseHttp.jsonObjectToMap(it)) }
        }
    }

    fun upsert(table: String, row: JSONObject, onConflict: String): RemoteDocument? {
        val headers = SupabaseHttp.restHeaders("resolution=merge-duplicates,return=representation")
            .toMutableMap()
        headers["Prefer"] = "resolution=merge-duplicates,return=representation"
        SupabaseHttp.postJson(
            url = "${SupabaseConfig.restUrl}/$table?on_conflict=$onConflict",
            body = row,
            headers = headers
        ).use { response ->
            val text = SupabaseHttp.responseText(response)
            if (!response.isSuccessful) {
                throw IllegalStateException("Supabase upsert failed (${response.code}): $text")
            }
            val array = SupabaseHttp.parseArray(text)
            val obj = array?.optJSONObject(0) ?: SupabaseHttp.parseObject(text)
            return obj?.let { RemoteDocument.fromRow(SupabaseHttp.jsonObjectToMap(it)) }
        }
    }

    fun update(table: String, query: String, patch: JSONObject): Boolean {
        val url = "${SupabaseConfig.restUrl}/$table?$query"
        SupabaseHttp.patchJson(url, patch).use { response ->
            if (!response.isSuccessful) {
                val text = SupabaseHttp.responseText(response)
                throw IllegalStateException("Supabase update failed (${response.code}): $text")
            }
            return true
        }
    }

    fun delete(table: String, query: String): Boolean {
        val url = "${SupabaseConfig.restUrl}/$table?$query"
        SupabaseHttp.delete(url).use { response ->
            if (!response.isSuccessful) {
                val text = SupabaseHttp.responseText(response)
                throw IllegalStateException("Supabase delete failed (${response.code}): $text")
            }
            return true
        }
    }

    fun eq(column: String, value: String): String =
        "${enc(column)}=eq.${enc(value)}"

    fun order(column: String, ascending: Boolean = true): String =
        "order=${enc(column)}.${if (ascending) "asc" else "desc"}"

    private fun enc(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())
}
