package com.project.farmingapp.model

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.LiveData
import com.project.farmingapp.model.data.FieldSurvey
import com.project.farmingapp.model.db.FieldSurveyDatabase
import com.project.farmingapp.supabase.FieldAlias
import com.project.farmingapp.supabase.SupabaseDb
import com.project.farmingapp.utilities.AuthHelper
import com.project.farmingapp.utilities.ImageCompressor
import com.project.farmingapp.utilities.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.UUID

class FieldSurveyRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dao = FieldSurveyDatabase.getInstance(context).fieldSurveyDao()

    val allSurveys: LiveData<List<FieldSurvey>> = dao.getAllSurveys()
    val unsyncedCount: LiveData<Int> = dao.getUnsyncedCount()

    suspend fun saveSurvey(survey: FieldSurvey): Long = dao.insert(survey)

    suspend fun updateSurvey(survey: FieldSurvey) = dao.update(survey)

    suspend fun deleteSurvey(survey: FieldSurvey) = dao.delete(survey)

    suspend fun getSurveyById(id: Int): FieldSurvey? = dao.getSurveyById(id)

    /**
     * Sync all pending surveys to Supabase (Storage + Database).
     * Media goes to Storage bucket `media`; rows go to table `surveys`.
     */
    suspend fun syncAll(context: Context): SyncResult = withContext(Dispatchers.IO) {
        try {
            LogUtil.syncDebug("Starting survey sync...")
            
            if (!isOnline(context)) {
                LogUtil.syncDebug("No internet connection. Surveys saved locally, will sync later.")
                return@withContext SyncResult.NoInternet
            }
            
            val pending = dao.getUnsyncedSurveys()
            if (pending.isEmpty()) {
                LogUtil.syncDebug("No pending surveys to sync.")
                return@withContext SyncResult.NothingToSync
            }
            
            LogUtil.syncDebug("Found ${pending.size} pending survey(ies) to sync")
            
            val uid = runCatching { AuthHelper.requireUid() }.getOrNull()
                ?: return@withContext SyncResult.Error("Sign in with an account to sync surveys. They stay saved on this device.")
            var syncedCount = 0
            
            pending.forEach { original ->
                try {
                    LogUtil.syncDebug("Syncing survey: ${original.farmerName} (ID: ${original.id})")
                    
                    // Ensure we have a stable, unique ID for remote document
                    val survey = if (original.remoteId.isBlank()) {
                        original.copy(remoteId = UUID.randomUUID().toString())
                    } else {
                        original
                    }
                    
                    // Update local copy with remote ID if needed
                    if (survey.remoteId != original.remoteId) {
                        dao.update(survey)
                    }
                    
                    // 1. Upload image to Supabase Storage (NO Firebase Storage)
                    var imageUrl = survey.imageUrl
                    if (imageUrl.isEmpty() && survey.imagePath.isNotEmpty()) {
                        imageUrl = uploadImageToSupabase(uid, survey)
                    }
                    
                    // 2. Upload audio to Supabase Storage (NO Firebase Storage)
                    var audioUrl = survey.audioUrl
                    if (audioUrl.isEmpty() && survey.audioPath.isNotEmpty()) {
                        audioUrl = uploadAudioToSupabase(uid, survey)
                    }
                    
                    val doc = mapOf(
                        "uid" to uid,
                        "remoteId" to survey.remoteId,
                        "localId" to survey.id,
                        "farmerName" to survey.farmerName,
                        "locationText" to survey.locationText,
                        "latitude" to survey.latitude,
                        "longitude" to survey.longitude,
                        "cropType" to survey.cropType,
                        "soilCondition" to survey.soilCondition,
                        "irrigationType" to survey.irrigationType,
                        "pestNotes" to survey.pestNotes,
                        "estimatedYieldKg" to survey.estimatedYieldKg,
                        "imageUrl" to imageUrl,
                        "audioUrl" to audioUrl,
                        "savedAt" to survey.savedAt,
                        "syncedAt" to System.currentTimeMillis()
                    )
                    val snake = FieldAlias.toSnakeRow(doc)
                    val row = JSONObject()
                    snake.forEach { (k, v) -> row.put(k, v ?: JSONObject.NULL) }
                    SupabaseDb.upsert("surveys", row, onConflict = "remote_id")
                    LogUtil.syncDebug("Successfully uploaded survey to Supabase: ${survey.farmerName}")
                    
                    // 4. Update local record: mark synced, store media URLs
                    dao.update(
                        survey.copy(
                            imageUrl = imageUrl,
                            audioUrl = audioUrl,
                            isSynced = true
                        )
                    )
                    
                    syncedCount++
                } catch (e: Exception) {
                    LogUtil.syncError("Failed to sync survey ${original.farmerName}", e)
                    // Continue with next survey instead of stopping
                }
            }
            
            LogUtil.syncDebug("Sync completed: $syncedCount/${pending.size} surveys synced successfully")
            
            return@withContext if (syncedCount > 0) {
                SyncResult.Success(syncedCount)
            } else {
                SyncResult.Error("Failed to sync any surveys. Check logs.")
            }
        } catch (e: Exception) {
            LogUtil.syncError("Unexpected error during sync", e)
            SyncResult.Error(e.message ?: "Unknown error occurred during sync")
        }
    }
    
    /**
     * Upload image to Supabase Storage with UNIQUE object path.
     * Format: uploads/{userId}/{timestamp}_{random}_{filename}
     */
    private suspend fun uploadImageToSupabase(uid: String, survey: FieldSurvey): String {
        val imageFile = File(survey.imagePath)
        if (!imageFile.exists() || imageFile.length() == 0L) {
            LogUtil.syncDebug("Image file not found or empty: ${survey.imagePath}")
            return ""
        }
        
        LogUtil.syncDebug("Starting image upload to Supabase: ${survey.imagePath} (size: ${imageFile.length()} bytes)")

        // Compress to avoid huge uploads + memory pressure (camera ANR/crash source).
        val compressedFile = File(appContext.cacheDir, "survey_${survey.remoteId}_${System.currentTimeMillis()}.jpg")
        val compressedOk = ImageCompressor.compressImage(
            inputPath = imageFile.absolutePath,
            outputPath = compressedFile.absolutePath,
            maxWidth = 1280,
            maxHeight = 720,
            quality = 82
        )
        val fileToUpload = if (compressedOk && compressedFile.exists() && compressedFile.length() > 0L) {
            LogUtil.syncDebug("Using compressed image: ${compressedFile.length()} bytes")
            compressedFile
        } else {
            compressedFile.delete()
            imageFile
        }

        val mimeType = guessMimeType(fileToUpload)
        val url = SupabaseUploader.uploadFile(
            file = fileToUpload,
            mimeType = mimeType,
            subfolder = "uploads/$uid",
            originalFileName = imageFile.name
        )
        if (fileToUpload == compressedFile) compressedFile.delete()
        LogUtil.syncDebug("Supabase image uploaded successfully: $url")
        return url
    }
    
    /**
     * Upload audio to Supabase Storage with UNIQUE object path.
     * Format: uploads/{userId}/{timestamp}_{random}_{filename}
     */
    private suspend fun uploadAudioToSupabase(uid: String, survey: FieldSurvey): String {
        val audioFile = File(survey.audioPath)
        if (!audioFile.exists() || audioFile.length() == 0L) {
            LogUtil.syncDebug("Audio file not found or empty: ${survey.audioPath}")
            return ""
        }
        
        LogUtil.syncDebug("Starting audio upload to Supabase: ${survey.audioPath} (size: ${audioFile.length()} bytes)")
        val mimeType = guessMimeType(audioFile)
        val url = SupabaseUploader.uploadFile(
            file = audioFile,
            mimeType = mimeType,
            subfolder = "uploads/$uid",
            originalFileName = audioFile.name
        )
        LogUtil.syncDebug("Supabase audio uploaded successfully: $url")
        return url
    }

    private fun guessMimeType(file: File): String {
        val ext = file.extension.lowercase()
        return when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "m4a" -> "audio/mp4"
            "mp4" -> "video/mp4"
            "aac" -> "audio/aac"
            else -> "application/octet-stream"
        }
    }
    
    /**
     * Check if device is connected to internet
     */
    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    sealed class SyncResult {
        data class Success(val count: Int) : SyncResult()
        object NoInternet : SyncResult()
        object NothingToSync : SyncResult()
        data class Error(val message: String) : SyncResult()
    }
}

