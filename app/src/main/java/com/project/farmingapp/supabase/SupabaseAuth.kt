package com.project.farmingapp.supabase

import android.content.Context
import org.json.JSONObject

data class SupabaseAuthResult(
    val success: Boolean,
    val message: String,
    val uid: String? = null,
    val email: String? = null,
    val displayName: String? = null
)

object SupabaseAuth {

    fun signUpWithEmail(
        context: Context,
        email: String,
        password: String,
        profile: Map<String, Any?>
    ): SupabaseAuthResult {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
            .put("data", JSONObject(profile.filterValues { it != null }))

        SupabaseHttp.postJson(
            url = "${SupabaseConfig.authUrl}/signup",
            body = body,
            headers = SupabaseHttp.authHeaders(includeBearer = false)
        ).use { response ->
            val text = SupabaseHttp.responseText(response)
            if (!response.isSuccessful) {
                return SupabaseAuthResult(false, extractError(text))
            }
            val json = SupabaseHttp.parseObject(text)
                ?: return SupabaseAuthResult(false, "Invalid signup response")

            val access = json.optString("access_token", "")
            val refresh = json.optString("refresh_token", null)
            val user = json.optJSONObject("user")
            val uid = user?.optString("id").orEmpty()
            val userEmail = user?.optString("email") ?: email
            val name = profile["name"]?.toString()
                ?: user?.optJSONObject("user_metadata")?.optString("name")

            if (access.isBlank() || uid.isBlank()) {
                // Email confirmation may be required — still create profile row if possible.
                upsertUserProfile(email, profile, uid.ifBlank { email })
                return SupabaseAuthResult(
                    true,
                    "Success",
                    uid = uid.ifBlank { null },
                    email = userEmail,
                    displayName = name
                )
            }

            SupabaseSession.save(context, access, refresh, uid, userEmail, name)
            upsertUserProfile(email, profile, uid)
            return SupabaseAuthResult(true, "Success", uid, userEmail, name)
        }
    }

    fun loginWithEmail(context: Context, email: String, password: String): SupabaseAuthResult {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)

        SupabaseHttp.postJson(
            url = "${SupabaseConfig.authUrl}/token?grant_type=password",
            body = body,
            headers = SupabaseHttp.authHeaders(includeBearer = false)
        ).use { response ->
            val text = SupabaseHttp.responseText(response)
            if (!response.isSuccessful) {
                return SupabaseAuthResult(false, extractError(text))
            }
            val json = SupabaseHttp.parseObject(text)
                ?: return SupabaseAuthResult(false, "Invalid login response")

            val access = json.optString("access_token")
            val refresh = json.optString("refresh_token", null)
            val user = json.optJSONObject("user")
            val uid = user?.optString("id").orEmpty()
            val userEmail = user?.optString("email") ?: email
            val name = user?.optJSONObject("user_metadata")?.optString("name")
                ?: user?.optJSONObject("user_metadata")?.optString("full_name")

            if (access.isBlank() || uid.isBlank()) {
                return SupabaseAuthResult(false, "Login failed: missing session")
            }
            SupabaseSession.save(context, access, refresh, uid, userEmail, name)
            return SupabaseAuthResult(true, "Success", uid, userEmail, name)
        }
    }

    /**
     * Exchange a Google ID token for a Supabase session.
     * Requires Google provider enabled in Supabase Auth settings.
     */
    fun signInWithGoogleIdToken(
        context: Context,
        idToken: String,
        email: String,
        profile: Map<String, Any?>
    ): SupabaseAuthResult {
        val body = JSONObject()
            .put("provider", "google")
            .put("id_token", idToken)

        SupabaseHttp.postJson(
            url = "${SupabaseConfig.authUrl}/token?grant_type=id_token",
            body = body,
            headers = SupabaseHttp.authHeaders(includeBearer = false)
        ).use { response ->
            val text = SupabaseHttp.responseText(response)
            if (!response.isSuccessful) {
                return SupabaseAuthResult(false, extractError(text))
            }
            val json = SupabaseHttp.parseObject(text)
                ?: return SupabaseAuthResult(false, "Invalid Google sign-in response")

            val access = json.optString("access_token")
            val refresh = json.optString("refresh_token", null)
            val user = json.optJSONObject("user")
            val uid = user?.optString("id").orEmpty()
            val userEmail = user?.optString("email") ?: email
            val name = profile["name"]?.toString()
                ?: user?.optJSONObject("user_metadata")?.optString("name")
                ?: user?.optJSONObject("user_metadata")?.optString("full_name")

            if (access.isBlank() || uid.isBlank()) {
                return SupabaseAuthResult(false, "Google sign-in failed: missing session")
            }
            SupabaseSession.save(context, access, refresh, uid, userEmail, name)
            upsertUserProfile(userEmail, profile, uid)
            return SupabaseAuthResult(true, "Success", uid, userEmail, name)
        }
    }

    fun sendPasswordReset(email: String): SupabaseAuthResult {
        val body = JSONObject().put("email", email)
        SupabaseHttp.postJson(
            url = "${SupabaseConfig.authUrl}/recover",
            body = body,
            headers = SupabaseHttp.authHeaders(includeBearer = false)
        ).use { response ->
            val text = SupabaseHttp.responseText(response)
            return if (response.isSuccessful) {
                SupabaseAuthResult(true, "Email Sent")
            } else {
                SupabaseAuthResult(false, extractError(text))
            }
        }
    }

    fun signOut(context: Context) {
        try {
            val token = SupabaseSession.accessToken
            if (!token.isNullOrBlank()) {
                SupabaseHttp.postJson(
                    url = "${SupabaseConfig.authUrl}/logout",
                    body = JSONObject(),
                    headers = mapOf(
                        "apikey" to SupabaseConfig.ANON_KEY,
                        "Authorization" to "Bearer $token",
                        "Content-Type" to "application/json"
                    )
                ).close()
            }
        } catch (_: Exception) {
        } finally {
            SupabaseSession.clear(context)
        }
    }

    private fun upsertUserProfile(email: String, profile: Map<String, Any?>, uid: String) {
        try {
            val row = JSONObject()
                .put("id", uid)
                .put("email", email)
            profile.forEach { (key, value) ->
                when (value) {
                    null -> Unit
                    is List<*> -> row.put(key, org.json.JSONArray(value))
                    is Map<*, *> -> row.put(key, JSONObject(value as Map<*, *>))
                    else -> row.put(key, value)
                }
            }
            // Normalize common column names used by the Android app.
            if (!row.has("profile_image") && row.has("profileImage")) {
                row.put("profile_image", row.get("profileImage"))
            }
            if (!row.has("user_type") && row.has("userType")) {
                row.put("user_type", row.get("userType"))
            }
            if (!row.has("mob_no") && row.has("mobNo")) {
                row.put("mob_no", row.get("mobNo"))
            }
            SupabaseDb.upsert("users", row, onConflict = "email")
        } catch (_: Exception) {
            // Profile upsert is best-effort; auth session may still be valid.
        }
    }

    private fun extractError(text: String): String {
        val json = SupabaseHttp.parseObject(text) ?: return text.ifBlank { "Auth request failed" }
        return json.optString("msg")
            .ifBlank { json.optString("error_description") }
            .ifBlank { json.optString("message") }
            .ifBlank { json.optString("error") }
            .ifBlank { "Auth request failed" }
    }
}
