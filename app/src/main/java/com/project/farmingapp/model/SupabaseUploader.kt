package com.project.farmingapp.model

import com.project.farmingapp.supabase.SupabaseConfig
import com.project.farmingapp.supabase.SupabaseSession
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Uploads files to Supabase Storage and returns a public URL.
 * Bucket must exist and allow uploads with the anon key (or be public).
 */
object SupabaseUploader {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Uploads [file] to Supabase Storage under [subfolder].
     * Returns the public URL on success, throws on failure.
     */
    suspend fun uploadFile(
        file: File,
        mimeType: String,
        subfolder: String,
        originalFileName: String? = null
    ): String = suspendCancellableCoroutine { cont ->
        if (!SupabaseConfig.isConfigured()) {
            cont.resumeWithException(
                IOException("Supabase is not configured. Add SUPABASE_URL and SUPABASE_ANON_KEY to local.properties.")
            )
            return@suspendCancellableCoroutine
        }
        if (!file.exists() || file.length() == 0L) {
            cont.resumeWithException(IOException("File missing or empty: ${file.absolutePath}"))
            return@suspendCancellableCoroutine
        }

        val ext = file.extension.ifEmpty { guessExt(mimeType) }
        val safeOriginal = (originalFileName ?: file.name)
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .take(80)
        val objectPath = String.format(
            Locale.US,
            "%s/%d_%s_%s",
            subfolder.trim('/'),
            System.currentTimeMillis(),
            UUID.randomUUID().toString().replace("-", ""),
            if (safeOriginal.contains('.')) safeOriginal else "$safeOriginal.$ext"
        )
        val uploadUrl = "${SupabaseConfig.URL}/storage/v1/object/${SupabaseConfig.MEDIA_BUCKET}/$objectPath"
        val publicUrl = "${SupabaseConfig.URL}/storage/v1/object/public/${SupabaseConfig.MEDIA_BUCKET}/$objectPath"
        val bearer = SupabaseSession.bearerToken()

        val mediaType = mimeType.toMediaTypeOrNull()
        val body = file.asRequestBody(mediaType)

        val request = Request.Builder()
            .url(uploadUrl)
            .post(body)
            .addHeader("apikey", SupabaseConfig.ANON_KEY)
            .addHeader("Authorization", "Bearer $bearer")
            .addHeader("x-upsert", "true")
            .addHeader("Content-Type", mimeType)
            .build()

        val call = client.newCall(request)
        cont.invokeOnCancellation { call.cancel() }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (cont.isActive) cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        val errBody = try { resp.body?.string().orEmpty() } catch (_: Exception) { "" }
                        if (cont.isActive) {
                            cont.resumeWithException(
                                IOException("Supabase upload failed (${resp.code}): $errBody")
                            )
                        }
                        return
                    }
                    if (cont.isActive) cont.resume(publicUrl)
                }
            }
        })
    }

    private fun guessExt(mimeType: String): String = when {
        mimeType.contains("jpeg") -> "jpg"
        mimeType.contains("png") -> "png"
        mimeType.contains("webp") -> "webp"
        mimeType.contains("mp4") -> "mp4"
        mimeType.contains("mpeg") -> "mp3"
        mimeType.contains("aac") -> "aac"
        mimeType.contains("wav") -> "wav"
        else -> "bin"
    }
}
