package com.project.farmingapp

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.project.farmingapp.supabase.SupabaseSession
import com.project.farmingapp.utilities.AuthHelper
import com.project.farmingapp.utilities.LogUtil

/**
 * Application entry-point.
 * Dark / light mode must be set here — before ANY Activity calls super.onCreate().
 * Supabase session is restored from SharedPreferences for Auth/DB.
 */
class FarmingApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val darkEnabled = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            .getBoolean("darkModeEnabled", false)
        AppCompatDelegate.setDefaultNightMode(
            if (darkEnabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        try {
            AuthHelper.syncFromPrefs(this)
            SupabaseSession.syncFromPrefs(this)
            LogUtil.debug("FarmingApp", "Supabase session restored (loggedIn=${SupabaseSession.isLoggedIn()})")
        } catch (e: Exception) {
            LogUtil.error("FarmingApp", "Failed to restore Supabase session", e)
        }
    }
}
