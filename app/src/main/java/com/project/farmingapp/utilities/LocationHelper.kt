package com.project.farmingapp.utilities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Location utility using FusedLocationProviderClient (recommended by Google).
 * Provides efficient, low-battery location fetching with timeout support.
 */
class LocationHelper(private val context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    /**
     * Fetch current location asynchronously with timeout.
     *
     * @param timeoutMs Timeout in milliseconds (default 15 seconds)
     * @return LocationData or null if permission denied or timeout
     */
    suspend fun getCurrentLocation(timeoutMs: Long = 15000): LocationData? {
        if (!hasLocationPermission()) {
            LogUtil.locationDebug("Location permission not granted")
            return null
        }

        return withContext(Dispatchers.IO) {
            val cancellationTokenSource = CancellationTokenSource()
            try {
                LogUtil.locationDebug("Requesting current location (timeout: ${timeoutMs}ms)...")
                withTimeoutOrNull(timeoutMs) {
                    val location = fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cancellationTokenSource.token
                    ).await()
                    location?.let {
                        LogUtil.locationDebug(
                            "Location found: ${String.format("%.4f° N, %.4f° E", it.latitude, it.longitude)}"
                        )
                        LocationData(it.latitude, it.longitude)
                    }
                }.also { result ->
                    if (result == null) {
                        LogUtil.locationDebug("Location request timed out after ${timeoutMs}ms")
                    }
                }
            } catch (e: SecurityException) {
                LogUtil.locationError("Security exception during location fetch", e)
                null
            } catch (e: Exception) {
                LogUtil.locationError("Unexpected error during location fetch", e)
                null
            } finally {
                cancellationTokenSource.cancel()
            }
        }
    }
    
    /**
     * Check if location permission is granted
     */
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Data class for location result
     */
    data class LocationData(
        val latitude: Double,
        val longitude: Double
    )
}
