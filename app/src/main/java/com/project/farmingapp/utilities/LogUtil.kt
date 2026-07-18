package com.project.farmingapp.utilities

import android.util.Log

/**
 * Centralized logging utility for the app.
 * Use tags: SURVEY, CAMERA, LOCATION, FIREBASE, SYNC, etc.
 */
object LogUtil {
    
    private const val APP_TAG = "FarmingApp"
    
    fun debug(tag: String, message: String) {
        Log.d("[$tag]", message)
    }
    
    fun error(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e("[$tag]", message, throwable)
        } else {
            Log.e("[$tag]", message)
        }
    }
    
    fun info(tag: String, message: String) {
        Log.i("[$tag]", message)
    }
    
    fun warning(tag: String, message: String) {
        Log.w("[$tag]", message)
    }
    
    // Convenience methods for common tags
    fun surveyDebug(message: String) = debug("SURVEY", message)
    fun surveyError(message: String, throwable: Throwable? = null) = error("SURVEY", message, throwable)
    
    fun cameraDebug(message: String) = debug("CAMERA", message)
    fun cameraError(message: String, throwable: Throwable? = null) = error("CAMERA", message, throwable)
    
    fun locationDebug(message: String) = debug("LOCATION", message)
    fun locationError(message: String, throwable: Throwable? = null) = error("LOCATION", message, throwable)
    
    fun firebaseDebug(message: String) = debug("FIREBASE", message)
    fun firebaseError(message: String, throwable: Throwable? = null) = error("FIREBASE", message, throwable)
    
    fun syncDebug(message: String) = debug("SYNC", message)
    fun syncError(message: String, throwable: Throwable? = null) = error("SYNC", message, throwable)
}
