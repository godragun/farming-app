package com.project.farmingapp.utilities

import android.content.Context
import com.project.farmingapp.supabase.SupabaseSession

/**
 * Offline / demo mode: guest mode supplies a stable id/email for UI when no Supabase session.
 * Real sessions come from [SupabaseSession].
 */
object AuthHelper {

    private const val PREFS = "auth_helper_prefs"
    private const val KEY_GUEST = "guest_mode"

    const val GUEST_EMAIL = "harendrathennakoon@gmail.com"
    const val GUEST_UID = "guest_offline_uid"
    const val GUEST_NAME = "Senidu"

    @Volatile
    var guestMode: Boolean = false
        private set

    fun syncFromPrefs(context: Context) {
        guestMode = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_GUEST, false)
        SupabaseSession.syncFromPrefs(context)
    }

    fun enterGuestMode(context: Context) {
        guestMode = true
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_GUEST, true)
            .apply()
    }

    fun exitGuestMode(context: Context) {
        guestMode = false
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_GUEST, false)
            .apply()
    }

    fun isSignedIn(): Boolean =
        guestMode || SupabaseSession.isLoggedIn()

    fun requireEmail(): String =
        if (guestMode) GUEST_EMAIL
        else SupabaseSession.email ?: error("Not signed in")

    fun requireUid(): String =
        if (guestMode) GUEST_UID
        else SupabaseSession.userId ?: error("Not signed in")

    fun currentDisplayName(): String? =
        if (guestMode) GUEST_NAME
        else SupabaseSession.displayName
}
