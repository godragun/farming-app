package com.project.farmingapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.project.farmingapp.BuildConfig
import com.project.farmingapp.model.ai.AgriAiClient
import com.project.farmingapp.model.ai.DiseasePredictionResponse
import com.project.farmingapp.model.ai.LeafImageEncoder
import com.project.farmingapp.model.ai.LeafScanError
import com.project.farmingapp.model.ai.LeafScanResult
import com.project.farmingapp.model.ai.RoboflowLeafClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.concurrent.Executors

/**
 * Crop Doctor: send the photo to the FastAPI /predict-disease backend.
 *
 * The backend is a self-hosted server behind a temporary tunnel, so it is often unreachable.
 * When that happens the photo is classified directly with the Roboflow leaf workflow (the same
 * model the Leaf Scanner uses) so the screen still produces a real diagnosis. The app never
 * invents a disease: if neither route works the user gets an error message.
 */
class CropDoctorViewModel : ViewModel() {

    private val _result = MutableLiveData<DiseasePredictionResponse?>()
    val result: LiveData<DiseasePredictionResponse?> = _result

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val executor = Executors.newSingleThreadExecutor()

    fun analyze(imageFile: File, cropHint: String? = null) {
        if (!imageFile.exists() || imageFile.length() == 0L) {
            _error.value = "Photo file missing"
            return
        }
        _loading.value = true
        _error.value = null
        _result.value = null

        try {
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
                        val parsed = try { response.body() } catch (_: Exception) { null }
                        if (response.isSuccessful && parsed != null) {
                            _loading.postValue(false)
                            _result.postValue(parsed)
                        } else {
                            fallbackToRoboflow(imageFile, httpErrorMessage(response.code()))
                        }
                    }

                    override fun onFailure(call: Call<DiseasePredictionResponse>, t: Throwable) {
                        fallbackToRoboflow(imageFile, UNAVAILABLE)
                    }
                })
        } catch (e: Exception) {
            Log.w(TAG, "Backend request could not be started", e)
            fallbackToRoboflow(imageFile, UNAVAILABLE)
        }
    }

    private fun fallbackToRoboflow(imageFile: File, backendMessage: String) {
        val apiKey = try { BuildConfig.ROBOFLOW_API_KEY } catch (_: Exception) { "" }
        if (apiKey.isBlank()) {
            postFailure(backendMessage)
            return
        }
        if (executor.isShutdown) return
        try {
            executor.execute {
                try {
                    val base64 = LeafImageEncoder.encodeBase64(imageFile)
                    val scan = RoboflowLeafClient.classifyBase64(base64, apiKey)
                    if (scan.top.isBlank()) {
                        postFailure("No leaf could be recognised in this photo. Take a clear, close photo of a single leaf.")
                    } else {
                        _loading.postValue(false)
                        _result.postValue(scan.toDiseaseResponse())
                    }
                } catch (e: LeafScanError.NetworkError) {
                    postFailure("No internet connection. Please check your network and try again.")
                } catch (e: LeafScanError.HttpError) {
                    Log.w(TAG, "Roboflow HTTP ${e.code}")
                    postFailure(backendMessage)
                } catch (e: OutOfMemoryError) {
                    postFailure("The photo is too large to process. Please take a smaller photo.")
                } catch (e: Exception) {
                    Log.w(TAG, "Roboflow fallback failed", e)
                    postFailure(backendMessage)
                }
            }
        } catch (e: java.util.concurrent.RejectedExecutionException) {
            // ViewModel was cleared (user left the screen) - nothing to report.
        }
    }

    private fun postFailure(message: String) {
        _loading.postValue(false)
        _result.postValue(null)
        _error.postValue(message)
    }

    private fun httpErrorMessage(code: Int): String = when (code) {
        413 -> "Photo is too large. Retake a closer photo."
        else -> UNAVAILABLE
    }

    fun clearResult() {
        _result.value = null
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        executor.shutdown()
    }

    private companion object {
        const val TAG = "CropDoctorVM"
        const val UNAVAILABLE =
            "AI disease detection is temporarily unavailable. Your offline crop guides are still available."
    }
}

/** Maps a Roboflow classification label such as "Tomato_Early_blight" to the Crop Doctor format. */
internal fun LeafScanResult.toDiseaseResponse(): DiseasePredictionResponse {
    val parts = top.replace("___", "_").split("_").filter { it.isNotBlank() }
    val crop = parts.firstOrNull()?.replaceFirstChar { it.uppercase() }
    val detail = parts.drop(1).joinToString(" ").ifBlank { top }
    val healthy = top.contains("healthy", ignoreCase = true)
    val level = when {
        confidence >= 0.8 -> "high"
        confidence >= 0.5 -> "medium"
        else -> "low"
    }
    val recommendationEn = if (healthy) {
        "The leaf looks healthy. Keep monitoring it weekly and keep good airflow and watering habits."
    } else {
        "Possible $detail detected. Remove and destroy affected leaves, avoid watering the foliage, " +
            "and ask your local agriculture officer which treatment suits your crop."
    }
    val recommendationSi = if (healthy) {
        "කොළය සෞඛ්‍ය සම්පන්නව පෙනේ. සතිපතා නිරීක්ෂණය කරන්න."
    } else {
        "රෝගී කොළ ඉවත් කර විනාශ කරන්න. නිවැරදි ප්‍රතිකාර සඳහා ප්‍රදේශයේ කෘෂිකර්ම නිලධාරියාගෙන් උපදෙස් ලබාගන්න."
    }
    return DiseasePredictionResponse(
        diagnosis_available = diagnosisAvailable,
        crop = crop,
        disease = if (healthy) "Healthy" else detail,
        raw_label = top,
        confidence = confidence,
        confidence_level = level,
        recommendation = recommendationEn,
        recommendation_sinhala = recommendationSi,
        source = "roboflow",
        engine = "roboflow-direct"
    )
}
