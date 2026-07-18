package com.project.farmingapp

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.project.farmingapp.utilities.LogUtil

/**
 * Application entry-point.
 * Dark / light mode must be set here — before ANY Activity calls super.onCreate()
 * which locks in the theme configuration. Setting it anywhere inside onCreate() is too late.
 *
 * Also initializes Firebase offline persistence for reliable data sync.
 */
class FarmingApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Set dark/light mode
        val darkEnabled = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            .getBoolean("darkModeEnabled", false)
        AppCompatDelegate.setDefaultNightMode(
            if (darkEnabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        
        // Enable Firebase Firestore offline persistence
        // This allows data to be saved locally and synced when network is available
        try {
            val db = FirebaseFirestore.getInstance()
            db.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            LogUtil.firebaseDebug("Firebase Firestore offline persistence enabled")
        } catch (e: Exception) {
            LogUtil.firebaseError("Failed to enable Firestore offline persistence", e)
        }
    }
}
