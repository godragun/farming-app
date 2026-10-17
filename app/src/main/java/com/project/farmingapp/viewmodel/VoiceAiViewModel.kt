package com.project.farmingapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.project.farmingapp.model.ai.AgriAiClient
import com.project.farmingapp.model.ai.VoiceAssistantResponse
import com.project.farmingapp.model.ai.VoiceTextRequest
import com.project.farmingapp.view.chatbot.ChatbotKnowledgeEngine
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

    /**
     * Safe to call from any thread: every LiveData write below uses postValue, because
     * [tryDirectWhisperTranscription] calls this from a background executor and
     * setValue() on a background thread throws IllegalStateException (app crash).
     */
    fun askText(sinhalaOrEnglish: String) {
        val text = sinhalaOrEnglish.trim()
        if (text.isEmpty()) {
            _error.postValue("Type or speak a question first")
            return
        }
        _loading.postValue(true)
        _error.postValue(null)
        try {
            AgriAiClient.api.voiceAssistantText(VoiceTextRequest(text))
                .enqueue(object : Callback<VoiceAssistantResponse> {
                    override fun onResponse(
                        call: Call<VoiceAssistantResponse>,
                        response: Response<VoiceAssistantResponse>
                    ) {
                        val body = try { response.body() } catch (_: Exception) { null }
                        if (response.isSuccessful && body != null) {
                            _loading.postValue(false)
                            _result.postValue(body)
                        } else {
                            answerLocally(text)
                        }
                    }

                    override fun onFailure(call: Call<VoiceAssistantResponse>, t: Throwable) {
                        answerLocally(text)
                    }
                })
        } catch (e: Exception) {
            answerLocally(text)
        }
    }

    private val _transcribedText = MutableLiveData<String?>()
    val transcribedText: LiveData<String?> = _transcribedText

    fun askAudio(audioFile: File, fallbackText: String? = null) {
        if (!audioFile.exists() || audioFile.length() == 0L) {
            _error.postValue("Audio file missing")
            return
        }
        _loading.postValue(true)
        _error.postValue(null)
        try {
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
                        val body = try { response.body() } catch (_: Exception) { null }
                        if (response.isSuccessful && body != null) {
                            _loading.postValue(false)
                            val transcribed = body.transcription_sinhala ?: body.transcription_english
                            if (!transcribed.isNullOrBlank()) {
                                _transcribedText.postValue(transcribed)
                            }
                            _result.postValue(body)
                        } else {
                            // Fallback: Transcribe directly via Hugging Face Whisper Sinhala v3
                            tryDirectWhisperTranscription(audioFile, fallbackText)
                        }
                    }

                    override fun onFailure(call: Call<VoiceAssistantResponse>, t: Throwable) {
                        // Fallback: Transcribe directly via Hugging Face Whisper Sinhala v3
                        tryDirectWhisperTranscription(audioFile, fallbackText)
                    }
                })
        } catch (e: Exception) {
            tryDirectWhisperTranscription(audioFile, fallbackText)
        }
    }

    private fun tryDirectWhisperTranscription(audioFile: File, fallbackText: String?) {
        val hfApiKey = try {
            com.project.farmingapp.BuildConfig.HUGGINGFACE_API_KEY
        } catch (_: Exception) {
            ""
        }
        val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
        executor.execute {
            try {
                var transcribed = ""
                try {
                    transcribed = com.project.farmingapp.model.ai.WhisperSinhalaClient.transcribeAudio(
                        audioFile = audioFile,
                        hfApiKey = hfApiKey
                    )
                } catch (e: Exception) {
                    android.util.Log.w("VoiceAiVM", "Direct Whisper transcription failed: ${e.message}")
                }

                val typed = fallbackText?.trim().orEmpty()
                when {
                    transcribed.isNotBlank() -> {
                        _transcribedText.postValue(transcribed)
                        askText(transcribed)
                    }
                    typed.isNotEmpty() -> askText(typed)
                    else -> {
                        _loading.postValue(false)
                        _error.postValue("Voice recognition unavailable. Please type your farming question.")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("VoiceAiVM", "Voice fallback failed", e)
                _loading.postValue(false)
                _error.postValue("Voice recognition failed. Please type your farming question.")
            } finally {
                executor.shutdown()
            }
        }
    }
    /** Offline farming answer when the FastAPI voice server is unreachable. */
    private fun answerLocally(text: String) {
        val reply = ChatbotKnowledgeEngine.generateResponse(text)
        val needsPhoto = text.contains("කොළ") ||
            text.contains("leaf", ignoreCase = true) ||
            text.contains("disease", ignoreCase = true) ||
            text.contains("රෝග")
        _loading.postValue(false)
        _error.postValue(null)
        _result.postValue(
            VoiceAssistantResponse(
                transcription_sinhala = text,
                intent = "local_knowledge",
                needs_photo = needsPhoto,
                next_action = if (needsPhoto) "request_leaf_photo" else null,
                response_sinhala = reply,
                response_english = reply,
                stt_engine = "on_device"
            )
        )
    }
}
