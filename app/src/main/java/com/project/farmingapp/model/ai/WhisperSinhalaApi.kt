package com.project.farmingapp.model.ai

import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Hugging Face Whisper Small Sinhala v3 integration.
 *
 * Model: https://huggingface.co/Lingalingeswaran/whisper-small-sinhala_v3
 * Task: Automatic Speech Recognition (ASR) for Sinhala language.
 *
 * Direct mobile client: Sends audio recordings (.m4a, .mp3, .wav) to the
 * Hugging Face Inference API / Serverless Endpoint and parses the transcribed
 * Sinhala text.
 */
object WhisperSinhalaConfig {
    const val MODEL_ID = "Lingalingeswaran/whisper-small-sinhala_v3"
    const val HF_API_URL = "https://api-inference.huggingface.co/models/$MODEL_ID"
    const val HF_ROUTER_URL = "https://router.huggingface.co/hf-inference/models/$MODEL_ID"

    // Default sample rate & language tag
    const val LANGUAGE_SI = "si"
}

sealed class WhisperTranscriptionError(message: String? = null, override val cause: Throwable? = null) : Exception(message, cause) {
    data class NetworkError(override val cause: Throwable) : WhisperTranscriptionError(cause.message, cause)
    data class HttpError(val code: Int, val body: String?) : WhisperTranscriptionError("HTTP $code: $body")
    data class ParseError(override val cause: Throwable) : WhisperTranscriptionError(cause.message, cause)
    data class ModelLoading(val estimatedTime: Double) : WhisperTranscriptionError("Model is currently loading on Hugging Face (est. ${estimatedTime}s)")
    object EmptyAudio : WhisperTranscriptionError("Audio file is empty or missing")
}

object WhisperSinhalaClient {

    private const val TAG = "WhisperSinhalaClient"
    private const val TIMEOUT_SEC = 60L

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Transcribe audio to Sinhala text via Hugging Face Inference API.
     *
     * @param audioFile Recorded voice file (.m4a / .wav / .mp3)
     * @param hfApiKey Optional Hugging Face User Access Token (Bearer hf_...)
     * @return Transcribed Sinhala text
     */
    @Throws(WhisperTranscriptionError::class)
    fun transcribeAudio(audioFile: File, hfApiKey: String = ""): String {
        if (!audioFile.exists() || audioFile.length() == 0L) {
            throw WhisperTranscriptionError.EmptyAudio
        }

        val mimeType = when (audioFile.extension.lowercase()) {
            "wav" -> "audio/wav"
            "mp3" -> "audio/mpeg"
            else -> "audio/m4a"
        }

        val requestBody = audioFile.asRequestBody(mimeType.toMediaTypeOrNull())

        // Try standard inference endpoint, then router fallback if needed
        val endpoints = listOf(WhisperSinhalaConfig.HF_API_URL, WhisperSinhalaConfig.HF_ROUTER_URL)
        var lastException: WhisperTranscriptionError? = null

        for (url in endpoints) {
            val reqBuilder = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Content-Type", mimeType)

            if (hfApiKey.isNotBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $hfApiKey")
            }

            try {
                val response = httpClient.newCall(reqBuilder.build()).execute()
                val code = response.code
                val rawBody = response.body?.string().orEmpty()

                if (response.isSuccessful) {
                    val text = parseTranscriptionResponse(rawBody)
                    if (text.isNotBlank()) {
                        return text
                    }
                } else if (code == 503 && rawBody.contains("loading", ignoreCase = true)) {
                    val est = try {
                        JSONObject(rawBody).optDouble("estimated_time", 20.0)
                    } catch (_: Exception) {
                        20.0
                    }
                    throw WhisperTranscriptionError.ModelLoading(est)
                } else {
                    Log.w(TAG, "Endpoint $url returned HTTP $code: $rawBody")
                    lastException = WhisperTranscriptionError.HttpError(code, rawBody)
                }
            } catch (e: IOException) {
                Log.w(TAG, "Network error contacting $url", e)
                lastException = WhisperTranscriptionError.NetworkError(e)
            } catch (e: WhisperTranscriptionError) {
                throw e
            }
        }

        throw lastException ?: WhisperTranscriptionError.HttpError(500, "Failed to transcribe audio")
    }

    /**
     * Parses Hugging Face ASR response:
     * Either `{"text": "..."}` or `[{"text": "..."}]`
     */
    internal fun parseTranscriptionResponse(jsonString: String): String {
        val trimmed = jsonString.trim()
        if (trimmed.startsWith("{")) {
            val obj = JSONObject(trimmed)
            if (obj.has("text")) return obj.getString("text").trim()
            if (obj.has("transcription")) return obj.getString("transcription").trim()
        } else if (trimmed.startsWith("[")) {
            val array = JSONArray(trimmed)
            if (array.length() > 0) {
                val first = array.getJSONObject(0)
                if (first.has("text")) return first.getString("text").trim()
            }
        }
        return ""
    }
}
