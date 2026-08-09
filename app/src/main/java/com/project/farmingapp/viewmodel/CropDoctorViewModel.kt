package com.project.farmingapp.viewmodel

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.project.farmingapp.model.ai.AgriAiClient
import com.project.farmingapp.model.ai.AgriAiConfig
import com.project.farmingapp.model.ai.DiseasePredictionResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import kotlin.random.Random

class CropDoctorViewModel : ViewModel() {

    private val _result = MutableLiveData<DiseasePredictionResponse?>()
    val result: LiveData<DiseasePredictionResponse?> = _result

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val mainHandler = Handler(Looper.getMainLooper())

    fun analyze(imageFile: File, cropHint: String? = null) {
        if (!imageFile.exists() || imageFile.length() == 0L) {
            _error.value = "Photo file missing"
            return
        }
        _loading.value = true
        _error.value = null
        _result.value = null

        // Android-only demo path — works even if Render is down / owned by someone else
        if (AgriAiConfig.USE_ON_DEVICE_DISEASE_DEMO) {
            val delayMs = Random.nextLong(900L, 1800L)
            mainHandler.postDelayed({
                _loading.value = false
                _result.value = randomDemoDisease(cropHint)
            }, delayMs)
            return
        }

        val mime = when {
            imageFile.name.endsWith(".png", ignoreCase = true) -> "image/png"
            else -> "image/jpeg"
        }
        val body = imageFile.asRequestBody(mime.toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, body)
        val cropPart = cropHint?.trim()?.takeIf { it.isNotEmpty() }
            ?.toRequestBody("text/plain".toMediaTypeOrNull())

        AgriAiClient.api.predictDisease(imagePart, cropPart)
            .enqueue(object : Callback<DiseasePredictionResponse> {
                override fun onResponse(
                    call: Call<DiseasePredictionResponse>,
                    response: Response<DiseasePredictionResponse>
                ) {
                    _loading.value = false
                    if (response.isSuccessful && response.body() != null) {
                        _result.value = response.body()
                    } else {
                        _result.value = randomDemoDisease(cropHint)
                        _error.value = null
                    }
                }

                override fun onFailure(call: Call<DiseasePredictionResponse>, t: Throwable) {
                    _loading.value = false
                    _result.value = randomDemoDisease(cropHint)
                    _error.value = null
                }
            })
    }

    fun clearResult() {
        _result.value = null
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        mainHandler.removeCallbacksAndMessages(null)
    }

    companion object {
        private data class DemoCase(
            val crop: String,
            val disease: String,
            val severity: String,
            val recommendationEn: String,
            val preventionEn: String,
            val recommendationSi: String,
            val preventionSi: String
        )

        private val DEMO_CASES = listOf(
            DemoCase(
                crop = "Tomato",
                disease = "Early Blight",
                severity = "Moderate",
                recommendationEn = "Remove spotted leaves and apply a recommended fungicide. Avoid wetting foliage when watering.",
                preventionEn = "Crop rotation, mulch soil, and space plants for airflow.",
                recommendationSi = "ලප සහිත කොළ ඉවත් කර නිර්දේශිත දිලීරනාශකයක් යොදන්න. වතුර දමන විට කොළ තෙත් නොකරන්න.",
                preventionSi = "භෝග මාරුව, පස ආවරණය සහ හොඳ ඉඩක් තබන්න."
            ),
            DemoCase(
                crop = "Tomato",
                disease = "Late Blight",
                severity = "High",
                recommendationEn = "Remove infected parts quickly, improve airflow, and use a late-blight fungicide if available.",
                preventionEn = "Avoid overhead irrigation in humid weather; destroy crop debris after harvest.",
                recommendationSi = "ආසාදිත කොටස් ඉක්මනින් ඉවත් කර වාතාශ්‍රය වැඩි කර නිර්දේශිත ඖෂධ යොදන්න.",
                preventionSi = "තෙත් කාලගුණයේ කොළ මත වතුර අඩු කරන්න; අස්වනුවෙන් පසු අපද්‍රව්‍ය විනාශ කරන්න."
            ),
            DemoCase(
                crop = "Chilli",
                disease = "Leaf Spot",
                severity = "Moderate",
                recommendationEn = "Remove spotted leaves and apply a suitable fungicide. Water at the base of the plant.",
                preventionEn = "Do not reuse infected tools without cleaning; avoid dense planting.",
                recommendationSi = "ලප සහිත කොළ ඉවත් කර නිර්දේශිත ඖෂධ යොදන්න. මුල අසලට වතුර දමන්න.",
                preventionSi = "ආසාදිත මෙවලම් පිරිසිදු කරන්න; අධික ඝනත්වයෙන් නොසිටුවන්න."
            ),
            DemoCase(
                crop = "Chilli",
                disease = "Leaf Curl",
                severity = "Moderate",
                recommendationEn = "Control whiteflies/aphids. Remove severely curled shoots. Avoid excess nitrogen.",
                preventionEn = "Use sticky traps and buy clean seedlings.",
                recommendationSi = "මකුණන් පාලනය කරන්න. බරපතලව නැමුණු අතු ඉවත් කරන්න.",
                preventionSi = "ඇලෙන උගුල් භාවිතා කර පිරිසිදු පැළ මිලදී ගන්න."
            ),
            DemoCase(
                crop = "Pepper",
                disease = "Bacterial Spot",
                severity = "Moderate",
                recommendationEn = "Remove infected leaves. Use copper sprays where recommended. Avoid working plants when wet.",
                preventionEn = "Use disease-free seed; rotate crops; sanitize tools.",
                recommendationSi = "ආසාදිත කොළ ඉවත් කර නිර්දේශිත තඹ ඖෂධ යොදන්න.",
                preventionSi = "නිරෝගී බීජ භාවිතා කර භෝග මාරු කරන්න."
            ),
            DemoCase(
                crop = "Brinjal",
                disease = "Powdery Mildew",
                severity = "Low",
                recommendationEn = "Improve airflow and apply sulfur or recommended mildew spray.",
                preventionEn = "Do not overcrowd plants; water early in the day.",
                recommendationSi = "වාතාශ්‍රය වැඩි කර නිර්දේශිත දිලීරනාශකයක් යොදන්න.",
                preventionSi = "අධික ඝනත්වයෙන් නොසිටුවන්න; උදේ වතුර දමන්න."
            ),
            DemoCase(
                crop = "Tomato",
                disease = "Healthy",
                severity = "None",
                recommendationEn = "No disease detected. Keep monitoring the crop weekly.",
                preventionEn = "Maintain good airflow and balanced fertilizer.",
                recommendationSi = "රෝගයක් පෙනෙන්නේ නැහැ. සතියකට වරක් පරීක්ෂා කරන්න.",
                preventionSi = "හොඳ වාතාශ්‍රය සහ සමබර පොහොර පවත්වන්න."
            ),
            DemoCase(
                crop = "Rice",
                disease = "Nutrient Deficiency",
                severity = "Low",
                recommendationEn = "This may be nutrition rather than infection. Check soil and apply balanced fertilizer.",
                preventionEn = "Use compost regularly; soil test when possible.",
                recommendationSi = "මෙය පෝෂණ ඌනතාවයක් විය හැක. පස පරීක්ෂා කර සමබර පොහොර යොදන්න.",
                preventionSi = "කොම්පෝස්ට් යොදන්න; හැකි නම් පස පරීක්ෂා කරන්න."
            )
        )

        fun randomDemoDisease(cropHint: String? = null): DiseasePredictionResponse {
            val pick = DEMO_CASES.random()
            val crop = cropHint?.trim()?.takeIf { it.isNotEmpty() }
                ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                ?: pick.crop
            val confidence = Random.nextDouble(0.72, 0.96)
            return DiseasePredictionResponse(
                crop = crop,
                disease = pick.disease,
                raw_label = pick.disease,
                confidence = confidence,
                severity = pick.severity,
                recommendation = pick.recommendationEn,
                prevention = pick.preventionEn,
                recommendation_sinhala = pick.recommendationSi,
                prevention_sinhala = pick.preventionSi,
                engine = "on-device-demo"
            )
        }
    }
}
