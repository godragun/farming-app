package com.project.farmingapp.supabase

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SupabaseHttp {

    private val jsonMedia = "application/json".toMediaType()

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    fun authHeaders(includeBearer: Boolean = true): Map<String, String> {
        val headers = linkedMapOf(
            "apikey" to SupabaseConfig.ANON_KEY,
            "Content-Type" to "application/json"
        )
        if (includeBearer) {
            headers["Authorization"] = "Bearer ${SupabaseSession.bearerToken()}"
        }
        return headers
    }

    fun restHeaders(prefer: String? = null): Map<String, String> {
        val headers = authHeaders().toMutableMap()
        prefer?.let { headers["Prefer"] = it }
        return headers
    }

    fun get(url: String, headers: Map<String, String> = restHeaders()): Response {
        val builder = Request.Builder().url(url).get()
        headers.forEach { (k, v) -> builder.addHeader(k, v) }
        return client.newCall(builder.build()).execute()
    }

    fun postJson(
        url: String,
        body: JSONObject,
        headers: Map<String, String> = restHeaders("return=representation")
    ): Response {
        val builder = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody(jsonMedia))
        headers.forEach { (k, v) -> builder.addHeader(k, v) }
        return client.newCall(builder.build()).execute()
    }

    fun postJsonArray(
        url: String,
        body: JSONArray,
        headers: Map<String, String> = restHeaders("return=representation")
    ): Response {
        val builder = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody(jsonMedia))
        headers.forEach { (k, v) -> builder.addHeader(k, v) }
        return client.newCall(builder.build()).execute()
    }

    fun patchJson(
        url: String,
        body: JSONObject,
        headers: Map<String, String> = restHeaders("return=representation")
    ): Response {
        val builder = Request.Builder()
            .url(url)
            .patch(body.toString().toRequestBody(jsonMedia))
        headers.forEach { (k, v) -> builder.addHeader(k, v) }
        return client.newCall(builder.build()).execute()
    }

    fun delete(url: String, headers: Map<String, String> = restHeaders()): Response {
        val builder = Request.Builder().url(url).delete()
        headers.forEach { (k, v) -> builder.addHeader(k, v) }
        return client.newCall(builder.build()).execute()
    }

    fun responseText(response: Response): String =
        response.body?.string().orEmpty()

    fun parseObject(text: String): JSONObject? = try {
        if (text.isBlank()) null else JSONObject(text)
    } catch (_: Exception) {
        null
    }

    fun parseArray(text: String): JSONArray? = try {
        if (text.isBlank()) null else JSONArray(text)
    } catch (_: Exception) {
        null
    }

    fun jsonObjectToMap(obj: JSONObject): Map<String, Any?> {
        val map = linkedMapOf<String, Any?>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = unwrap(obj.get(key))
        }
        return map
    }

    fun jsonArrayToMaps(array: JSONArray): List<Map<String, Any?>> {
        val list = ArrayList<Map<String, Any?>>(array.length())
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            list.add(jsonObjectToMap(item))
        }
        return list
    }

    private fun unwrap(value: Any?): Any? = when (value) {
        null, JSONObject.NULL -> null
        is JSONObject -> jsonObjectToMap(value)
        is JSONArray -> {
            val list = ArrayList<Any?>(value.length())
            for (i in 0 until value.length()) {
                list.add(unwrap(value.get(i)))
            }
            list
        }
        else -> value
    }
}
