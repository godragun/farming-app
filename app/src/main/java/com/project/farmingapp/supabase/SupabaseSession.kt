package com.project.farmingapp.supabase

import android.content.Context

/**
 * Persists the current Supabase Auth session on device.
 */
object SupabaseSession {

    private const val PREFS = "supabase_session_prefs"
    private const val KEY_ACCESS = "access_token"
    private const val KEY_REFRESH = "refresh_token"
    private const val KEY_UID = "user_id"
    private const val KEY_EMAIL = "email"
    private const val KEY_NAME = "display_name"

    @Volatile
    var accessToken: String? = null
        private set

    @Volatile
    var refreshToken: String? = null
        private set

    @Volatile
    var userId: String? = null
        private set

    @Volatile
    var email: String? = null
        private set

    @Volatile
    var displayName: String? = null
        private set

    fun syncFromPrefs(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        accessToken = prefs.getString(KEY_ACCESS, null)
        refreshToken = prefs.getString(KEY_REFRESH, null)
        userId = prefs.getString(KEY_UID, null)
        email = prefs.getString(KEY_EMAIL, null)
        displayName = prefs.getString(KEY_NAME, null)
    }

    fun save(
        context: Context,
        access: String,
        refresh: String?,
        uid: String,
        userEmail: String?,
        name: String?
    ) {
        accessToken = access
        refreshToken = refresh
        userId = uid
        email = userEmail
        displayName = name
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACCESS, access)
            .putString(KEY_REFRESH, refresh)
            .putString(KEY_UID, uid)
            .putString(KEY_EMAIL, userEmail)
            .putString(KEY_NAME, name)
            .apply()
    }

    fun clear(context: Context) {
        accessToken = null
        refreshToken = null
        userId = null
        email = null
        displayName = null
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    fun isLoggedIn(): Boolean = !accessToken.isNullOrBlank() && !userId.isNullOrBlank()

    fun bearerToken(): String = accessToken ?: SupabaseConfig.ANON_KEY
}
