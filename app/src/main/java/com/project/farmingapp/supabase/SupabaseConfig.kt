package com.project.farmingapp.supabase

import com.project.farmingapp.BuildConfig

/**
 * Client-safe Supabase project settings from BuildConfig / local.properties.
 * Never put a service_role key in the APK — anon only.
 */
object SupabaseConfig {
    @JvmField
    val URL: String = BuildConfig.SUPABASE_URL

    /** Public anon key only. Empty when not configured in local.properties. */
    @JvmField
    val ANON_KEY: String = BuildConfig.SUPABASE_ANON_KEY

    const val MEDIA_BUCKET = "media"

    fun isConfigured(): Boolean = URL.isNotBlank() && ANON_KEY.isNotBlank()

    val restUrl: String get() = "$URL/rest/v1"
    val authUrl: String get() = "$URL/auth/v1"
    val storageUrl: String get() = "$URL/storage/v1"
}
