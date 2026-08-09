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
 * Emulator (local) → http://10.0.2.2:8000/
 * Physical phone (same Wi‑Fi) → http://<PC_LAN_IP>:8000/
 * Production (Render) → https://farming-app-backend-xyw1.onrender.com/
 */
object AgriAiConfig {
    const val BASE_URL = "https://farming-app-backend-xyw1.onrender.com/"
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

data class CropSuitabilityRequest(
    val crop: String,
    val soil: String? = null,
    val irrigation: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val top_k: Int = 5
)

data class SuitabilityLocation(
    val label: String? = null,
    val district: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val score: Double? = null,
    val zone: String? = null,
    val sample_count: Int? = null,
    val avg_yield_kg: Double? = null,
    val common_soil: String? = null,
    val common_irrigation: String? = null,
    val reasons: List<String>? = null
)

data class SuitabilityZone(
    val label: String? = null,
    val district: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val score: Double? = null,
    val zone: String? = null
)

data class CropSuitabilityResponse(
    val crop: String? = null,
    val crop_english: String? = null,
    val engine: String? = null,
    val survey_count: Int? = null,
    val matched_survey_count: Int? = null,
    val best_locations: List<SuitabilityLocation>? = null,
    val map_zones: List<SuitabilityZone>? = null,
    val advice_sinhala: String? = null,
    val advice_english: String? = null,
    val supabase_configured: Boolean? = null,
    val warning: String? = null
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

    @POST("crop-suitability")
    fun cropSuitability(@Body body: CropSuitabilityRequest): Call<CropSuitabilityResponse>
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
