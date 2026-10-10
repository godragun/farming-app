package com.project.farmingapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.project.farmingapp.model.ai.AgriAiClient
import com.project.farmingapp.model.ai.DiseasePredictionResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

/**
 * Crop Doctor: send the photo to FastAPI /predict-disease.
 * Network, model, or HTTP failure is an error. The app never invents a disease.
 */
class CropDoctorViewModel : ViewModel() {

    private val _result = MutableLiveData<DiseasePredictionResponse?>()
    val result: LiveData<DiseasePredictionResponse?> = _result

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    fun analyze(imageFile: File, cropHint: String? = null) {
        if (!imageFile.exists() || imageFile.length() == 0L) {
            _error.value = "Photo file missing"
            return
        }
        _loading.value = true
        _error.value = null
        _result.value = null

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
                        _result.value = null
                        _error.value = when (response.code()) {
                            413 -> "Photo is too large. Retake a closer photo."
                            503 -> "AI disease detection is temporarily unavailable. Your offline crop guides are still available."
                            else -> "AI disease detection is temporarily unavailable. Your offline crop guides are still available."
                        }
                    }
                }

                override fun onFailure(call: Call<DiseasePredictionResponse>, t: Throwable) {
                    _loading.value = false
                    _result.value = null
                    _error.value = "AI disease detection is temporarily unavailable. Your offline crop guides are still available."
                }
            })
    }

    fun clearResult() {
        _result.value = null
        _error.value = null
    }
}
