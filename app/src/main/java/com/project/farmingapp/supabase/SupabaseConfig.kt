package com.project.farmingapp.supabase

/**
 * Client-safe Supabase project settings.
 * Anon key is intended for mobile clients; never put the service-role key here.
 */
object SupabaseConfig {
    const val URL = "https://nkjzjbjkcpcbefkypvdl.supabase.co"
    const val ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5ranpqYmprY3BjYmVma3lwdmRsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzY3ODMzMDQsImV4cCI6MjA5MjM1OTMwNH0.yI46mLkjTM9Qs3etBoJSCqCDjPRMuibUlvDSabFiG5w"
    const val MEDIA_BUCKET = "media"

    val restUrl: String get() = "$URL/rest/v1"
    val authUrl: String get() = "$URL/auth/v1"
    val storageUrl: String get() = "$URL/storage/v1"
}
