package com.project.farmingapp.model.ai

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.util.concurrent.TimeUnit

/**
 * FastAPI Agri Lanka AI backend.
 *
 * Emulator → http://10.0.2.2:8000/
 * Physical phone (same Wi‑Fi) → http://<PC_LAN_IP>:8000/
 * Production → https://YOUR_DEPLOYED_API/
 */
object AgriAiConfig {
    // Change this when testing on a real phone or after Render deploy.
    const val BASE_URL = "http://10.0.2.2:8000/"
}

data class VoiceTextRequest(val text: String)

data class VoiceAssistantResponse(
    val transcription_sinhala: String? = null,
    val transcription_english: String? = null,
    val intent: String? = null,
    val crop: String? = null,
    val crop_sinhala: String? = null,
    val symptom: String? = null,
    val symptom_sinhala: String? = null,
    val needs_photo: Boolean? = false,
    val response_sinhala: String? = null,
    val response_english: String? = null,
    val next_action: String? = null,
    val stt_engine: String? = null,
    val tts_audio_base64: String? = null,
    val tts_mime: String? = null,
    val tts_error: String? = null
)

data class DiseasePredictionResponse(
    val crop: String? = null,
    val disease: String? = null,
    val raw_label: String? = null,
    val confidence: Double? = null,
    val severity: String? = null,
    val recommendation: String? = null,
    val prevention: String? = null,
    val recommendation_sinhala: String? = null,
    val prevention_sinhala: String? = null,
    val engine: String? = null
)

interface AgriAiApi {
    @POST("voice-assistant/text")
    fun voiceAssistantText(@Body body: VoiceTextRequest): Call<VoiceAssistantResponse>

    @Multipart
    @POST("voice-assistant")
    fun voiceAssistant(
        @Part audio: MultipartBody.Part?,
        @Part("text") text: RequestBody?,
        @Part("include_tts") includeTts: RequestBody
    ): Call<VoiceAssistantResponse>

    @Multipart
    @POST("predict-disease")
    fun predictDisease(
        @Part image: MultipartBody.Part,
        @Part("crop") crop: RequestBody?
    ): Call<DiseasePredictionResponse>
}

object AgriAiClient {
    val api: AgriAiApi by lazy {
        val http = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
        Retrofit.Builder()
            .baseUrl(AgriAiConfig.BASE_URL)
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AgriAiApi::class.java)
    }
}
