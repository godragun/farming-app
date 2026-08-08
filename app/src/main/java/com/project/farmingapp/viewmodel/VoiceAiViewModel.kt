package com.project.farmingapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.project.farmingapp.model.ai.AgriAiClient
import com.project.farmingapp.model.ai.VoiceAssistantResponse
import com.project.farmingapp.model.ai.VoiceTextRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class VoiceAiViewModel : ViewModel() {

    private val _result = MutableLiveData<VoiceAssistantResponse?>()
    val result: LiveData<VoiceAssistantResponse?> = _result

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    fun askText(sinhalaOrEnglish: String) {
        val text = sinhalaOrEnglish.trim()
        if (text.isEmpty()) {
            _error.value = "Type or speak a question first"
            return
        }
        _loading.value = true
        _error.value = null
        AgriAiClient.api.voiceAssistantText(VoiceTextRequest(text))
            .enqueue(object : Callback<VoiceAssistantResponse> {
                override fun onResponse(
                    call: Call<VoiceAssistantResponse>,
                    response: Response<VoiceAssistantResponse>
                ) {
                    _loading.value = false
                    if (response.isSuccessful && response.body() != null) {
                        _result.value = response.body()
                    } else {
                        _error.value = "AI server error: ${response.code()}"
                    }
                }

                override fun onFailure(call: Call<VoiceAssistantResponse>, t: Throwable) {
                    _loading.value = false
                    _error.value =
                        "Cannot reach AI server. Is FastAPI running? (${t.message})"
                }
            })
    }

    fun askAudio(audioFile: File, fallbackText: String? = null) {
        if (!audioFile.exists() || audioFile.length() == 0L) {
            _error.value = "Audio file missing"
            return
        }
        _loading.value = true
        _error.value = null
        val audioBody = audioFile.asRequestBody("audio/mp4".toMediaTypeOrNull())
        val audioPart = MultipartBody.Part.createFormData("audio", audioFile.name, audioBody)
        val textPart = fallbackText?.trim()?.takeIf { it.isNotEmpty() }
            ?.toRequestBody("text/plain".toMediaTypeOrNull())
        val ttsPart = "true".toRequestBody("text/plain".toMediaTypeOrNull())

        AgriAiClient.api.voiceAssistant(audioPart, textPart, ttsPart)
            .enqueue(object : Callback<VoiceAssistantResponse> {
                override fun onResponse(
                    call: Call<VoiceAssistantResponse>,
                    response: Response<VoiceAssistantResponse>
                ) {
                    _loading.value = false
                    if (response.isSuccessful && response.body() != null) {
                        _result.value = response.body()
                    } else {
                        // Fallback: if Whisper missing, try text-only when user typed something
                        val typed = fallbackText?.trim().orEmpty()
                        if (typed.isNotEmpty()) {
                            askText(typed)
                        } else {
                            _error.value =
                                "Voice AI failed (${response.code()}). Type the question or install Whisper on the server."
                        }
                    }
                }

                override fun onFailure(call: Call<VoiceAssistantResponse>, t: Throwable) {
                    _loading.value = false
                    _error.value =
                        "Cannot reach AI server. Is FastAPI running? (${t.message})"
                }
            })
    }
}
