package com.project.farmingapp.model.ai

import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

// ---------------------------------------------------------------------------
// Config — API key is injected at runtime from BuildConfig or a local file.
// NEVER hardcode the key here.
// ---------------------------------------------------------------------------

/**
 * Roboflow Serverless Workflow — Leaf & Deceased Leaves Classification.
 *
 * Workflow: leaves-and-deceased-leaves-vleaves-and-deceased-leaves-z3m7h-1-
 *           vit-base-patch16-224-in21k-t1-logic
 * Workspace: garbagedetectionws
 *
 * Input  : image  (base64 or URL)
 * Outputs: predictions.top         — top class label  (String)
 *          predictions.confidence  — top confidence   (Double 0-1)
 *          predictions.predictions — per-class array  (List<LeafClassPrediction>)
 */
object RoboflowLeafConfig {
    const val WORKSPACE      = "garbagedetectionws"
    const val WORKFLOW_ID    = "leaves-and-deceased-leaves-vleaves-and-deceased-leaves-z3m7h-1-vit-base-patch16-224-in21k-t1-logic"
    const val ENDPOINT       = "https://serverless.roboflow.com/$WORKSPACE/workflows/$WORKFLOW_ID"

    // Minimum confidence (0–1) below which we treat the result as uncertain.
    const val MIN_CONFIDENCE = 0.30
}

// ---------------------------------------------------------------------------
// Response models — parsed from the real workflow output structure.
// Each run returns a List (one entry per input image).
// Entry structure:
//   predictions.top         : String
//   predictions.confidence  : Double
//   predictions.predictions : List<{class, class_id, confidence}>
// ---------------------------------------------------------------------------

data class LeafClassPrediction(
    val `class`: String,
    val class_id: Int,
    val confidence: Double
)

data class LeafScanResult(
    /** Top-ranked label (e.g. "Pepper_bell_healthy"). */
    val top: String,
    /** Confidence for the top class (0–1). */
    val confidence: Double,
    /** Full ranked list of classes. */
    val allPredictions: List<LeafClassPrediction>,
    /** Whether the confidence meets the minimum threshold. */
    val diagnosisAvailable: Boolean
)

// ---------------------------------------------------------------------------
// Typed errors
// ---------------------------------------------------------------------------

sealed class LeafScanError(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    data class NetworkError(override val cause: Throwable) : LeafScanError(cause.message, cause)
    data class HttpError(val code: Int, val body: String?) : LeafScanError("HTTP $code: $body")
    data class ParseError(override val cause: Throwable) : LeafScanError(cause.message, cause)
    object MissingApiKey : LeafScanError("Missing Roboflow API key")
    object EmptyImage : LeafScanError("Empty image provided")
}

// ---------------------------------------------------------------------------
// Client
// ---------------------------------------------------------------------------

object RoboflowLeafClient {

    private const val TAG = "RoboflowLeafClient"
    private const val TIMEOUT_CONNECT_SEC = 30L
    private const val TIMEOUT_READ_SEC    = 90L
    private const val TIMEOUT_WRITE_SEC   = 90L
    private const val MAX_RETRIES         = 2
    private const val RETRY_DELAY_MS      = 1500L

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_CONNECT_SEC, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_READ_SEC, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_WRITE_SEC, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false) // we retry manually below
            .build()
    }

    /**
     * Run the Roboflow leaf-classification workflow on a base64-encoded image.
     *
     * @param base64Image  Raw Base64 string (no data-URI prefix).
     * @param apiKey       Roboflow API key — must NOT be empty.
     * @param confidence   Optional confidence threshold override (default 0.4).
     * @return [LeafScanResult] on success.
     * @throws [LeafScanError] on any failure.
     */
    @Throws(LeafScanError::class)
    fun classifyBase64(
        base64Image: String,
        apiKey: String,
        minConfidence: Double = RoboflowLeafConfig.MIN_CONFIDENCE
    ): LeafScanResult {
        if (apiKey.isBlank()) throw LeafScanError.MissingApiKey
        if (base64Image.isBlank()) throw LeafScanError.EmptyImage

        val payload = JSONObject().apply {
            put("inputs", JSONObject().apply {
                put("image", JSONObject().apply {
                    put("type", "base64")
                    put("value", base64Image)
                })
            })
        }.toString()

        return executeWithRetry(payload, apiKey, minConfidence)
    }

    /**
     * Run the workflow with a publicly accessible HTTPS image URL.
     *
     * @param imageUrl HTTPS URL (plain http:// is rejected by the API).
     * @param apiKey   Roboflow API key.
     */
    @Throws(LeafScanError::class)
    fun classifyUrl(
        imageUrl: String,
        apiKey: String,
        minConfidence: Double = RoboflowLeafConfig.MIN_CONFIDENCE
    ): LeafScanResult {
        if (apiKey.isBlank()) throw LeafScanError.MissingApiKey

        val payload = JSONObject().apply {
            put("inputs", JSONObject().apply {
                put("image", JSONObject().apply {
                    put("type", "url")
                    put("value", imageUrl)
                })
            })
        }.toString()

        return executeWithRetry(payload, apiKey, minConfidence)
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private fun executeWithRetry(
        jsonPayload: String,
        apiKey: String,
        minConfidence: Double = RoboflowLeafConfig.MIN_CONFIDENCE
    ): LeafScanResult {
        var lastError: Exception? = null
        repeat(MAX_RETRIES + 1) { attempt ->
            try {
                return execute(jsonPayload, apiKey, minConfidence)
            } catch (e: LeafScanError.NetworkError) {
                lastError = e
                if (attempt < MAX_RETRIES) {
                    Log.w(TAG, "Attempt ${attempt + 1} failed, retrying…", e.cause)
                    Thread.sleep(RETRY_DELAY_MS * (attempt + 1))
                }
            } catch (e: LeafScanError.HttpError) {
                // Don't retry HTTP errors (4xx/5xx) — they won't change on retry.
                throw e
            }
        }
        throw lastError ?: LeafScanError.NetworkError(IOException("Unknown network error"))
    }

    private fun execute(
        jsonPayload: String,
        apiKey: String,
        minConfidence: Double = RoboflowLeafConfig.MIN_CONFIDENCE
    ): LeafScanResult {
        val requestBody = jsonPayload.toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(RoboflowLeafConfig.ENDPOINT)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        Log.d(TAG, "POST ${RoboflowLeafConfig.ENDPOINT}")

        val response = try {
            http.newCall(request).execute()
        } catch (e: IOException) {
            throw LeafScanError.NetworkError(e)
        }

        val code = response.code
        val rawBody = response.body?.string()

        if (!response.isSuccessful) {
            Log.e(TAG, "HTTP $code — $rawBody")
            throw LeafScanError.HttpError(code, rawBody)
        }

        return try {
            parseResponse(rawBody ?: "", minConfidence)
        } catch (e: Exception) {
            Log.e(TAG, "Parse failed: $rawBody", e)
            throw LeafScanError.ParseError(e)
        }
    }

    /**
     * Parse the Roboflow workflow response.
     *
     * Real response shape (verified by MCP workflow_run call):
     * ```json
     * [
     *   {
     *     "predictions": {
     *       "image": { "width": 640, "height": 480 },
     *       "predictions": [
     *         { "class": "Pepper_bell_healthy", "class_id": 1, "confidence": 0.892 }
     *       ],
     *       "top": "Pepper_bell_healthy",
     *       "confidence": 0.892
     *     },
     *     "output": { ... }
     *   }
     * ]
     * ```
     */
    internal fun parseResponse(
        raw: String,
        minConfidence: Double = RoboflowLeafConfig.MIN_CONFIDENCE
    ): LeafScanResult {
        val rootArray = org.json.JSONArray(raw)
        // Workflow always returns one entry per input image. We send one image.
        val entry = rootArray.getJSONObject(0)

        val predictionsObj = entry.getJSONObject("predictions")
        val top = predictionsObj.getString("top")
        val confidence = predictionsObj.getDouble("confidence")

        val predictionsArray = predictionsObj.optJSONArray("predictions")
        val allPredictions = mutableListOf<LeafClassPrediction>()
        if (predictionsArray != null) {
            for (i in 0 until predictionsArray.length()) {
                val p = predictionsArray.getJSONObject(i)
                allPredictions.add(
                    LeafClassPrediction(
                        `class` = p.getString("class"),
                        class_id = p.optInt("class_id", i),
                        confidence = p.getDouble("confidence")
                    )
                )
            }
        }

        return LeafScanResult(
            top = top,
            confidence = confidence,
            allPredictions = allPredictions,
            diagnosisAvailable = confidence >= minConfidence
        )
    }
}
