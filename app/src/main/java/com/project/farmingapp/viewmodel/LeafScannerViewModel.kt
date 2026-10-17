package com.project.farmingapp.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.project.farmingapp.model.ai.LeafScanError
import com.project.farmingapp.model.ai.LeafScanResult
import com.project.farmingapp.model.ai.RoboflowLeafClient
import com.project.farmingapp.model.ai.RoboflowLeafConfig
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.Executors

/**
 * ViewModel for the Leaf Scanner screen.
 *
 * Encodes the selected image to Base64 on an IO thread, then calls
 * [RoboflowLeafClient.classifyBase64].  The API key is loaded from the
 * ROBOFLOW_API_KEY build config field (set in local.properties → build.gradle).
 *
 * LiveData exposed:
 *  - [result]  — the parsed [LeafScanResult] or null
 *  - [error]   — a user-visible error string or null
 *  - [loading] — true while the request is in flight
 */
class LeafScannerViewModel : ViewModel() {

    private val TAG = "LeafScannerVM"

    private val _result  = MutableLiveData<LeafScanResult?>()
    val result: LiveData<LeafScanResult?> = _result

    private val _error   = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    // Dedicated single-thread executor — no coroutines needed; the project
    // already uses executor threads in other ViewModels.
    private val executor = Executors.newSingleThreadExecutor()

    /**
     * Compress [imageFile] to a 640×640 JPEG, Base64-encode it, and call the
     * Roboflow workflow endpoint.
     *
     * @param imageFile   Captured/selected image file on disk.
     * @param apiKey      Roboflow API key — passed in from the Fragment which
     *                    reads it from BuildConfig or a local fallback.
     */
    fun analyze(imageFile: File, apiKey: String) {
        if (!imageFile.exists() || imageFile.length() == 0L) {
            _error.value = "Image file is missing or empty."
            return
        }
        if (apiKey.isBlank()) {
            _error.value = "Roboflow API key not configured. Add ROBOFLOW_API_KEY to local.properties."
            return
        }

        _loading.value = true
        _error.value   = null
        _result.value  = null

        executor.execute {
            try {
                val base64 = com.project.farmingapp.model.ai.LeafImageEncoder.encodeBase64(imageFile)
                Log.d(TAG, "Encoded ${base64.length} chars, calling Roboflow…")

                val scanResult = RoboflowLeafClient.classifyBase64(
                    base64Image = base64,
                    apiKey      = apiKey
                )
                Log.d(TAG, "Result: top=${scanResult.top} conf=${scanResult.confidence}")

                if (scanResult.top.isBlank() && scanResult.allPredictions.isEmpty()) {
                    // Roboflow answered but recognised nothing (e.g. photo contains no leaf).
                    postError("No leaf could be recognised in this photo. Take a clear, close photo of a single leaf.")
                    return@execute
                }
                _loading.postValue(false)
                _result.postValue(scanResult)
            } catch (e: LeafScanError.MissingApiKey) {
                postError("Roboflow API key not configured.")
            } catch (e: LeafScanError.EmptyImage) {
                postError("Image could not be read.")
            } catch (e: LeafScanError.NetworkError) {
                Log.e(TAG, "Network error", e.cause)
                postError("Network error — check internet connection and try again.")
            } catch (e: LeafScanError.HttpError) {
                Log.e(TAG, "HTTP ${e.code}: ${e.body}")
                postError(when (e.code) {
                    401 -> "Invalid Roboflow API key (401). Check ROBOFLOW_API_KEY in local.properties."
                    403 -> "Access denied (403). Ensure the API key has access to workspace '${RoboflowLeafConfig.WORKSPACE}'."
                    413 -> "Image too large. Try again with a smaller photo."
                    429 -> "Roboflow rate limit reached. Wait a moment and try again."
                    503 -> "Roboflow service unavailable. Try again shortly."
                    else -> "Roboflow returned HTTP ${e.code}. Try again."
                })
            } catch (e: LeafScanError.ParseError) {
                Log.e(TAG, "Parse error", e.cause)
                postError("Unexpected response from Roboflow. Please try again.")
            } catch (e: OutOfMemoryError) {
                Log.e(TAG, "Out of memory while preparing image", e)
                postError("The photo is too large to process. Please take a smaller photo.")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error", e)
                postError("Unexpected error: ${e.message}")
            }
        }
    }

    fun clearResult() {
        _result.value = null
        _error.value  = null
    }

    private fun postError(msg: String) {
        _loading.postValue(false)
        _result.postValue(null)
        _error.postValue(msg)
    }

    override fun onCleared() {
        super.onCleared()
        executor.shutdown()
    }
}
