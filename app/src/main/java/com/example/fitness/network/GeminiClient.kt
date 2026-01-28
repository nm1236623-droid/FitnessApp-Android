package com.example.fitness.network

import android.util.Log
import com.example.fitness.BuildConfig
import com.example.fitness.running.CardioType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

object GeminiClient {

    private val JSON = "application/json; charset=utf-8".toMediaType()

    // Keep one client; configure timeouts.
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(75, TimeUnit.SECONDS)   // ⭐ 核心
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()


    private fun classifyHttpError(code: Int, responseBody: String): String {
        // Map common Generative Language API errors to human-readable messages.
        return when (code) {
            400 -> "Bad Request (400): request payload invalid"
            401 -> "Unauthorized (401): API key invalid or missing"
            403 -> "Forbidden (403): API key doesn't have access / billing not enabled / API disabled"
            404 -> "Not Found (404): model/endpoint not found"
            429 -> "Too Many Requests (429): quota/rate limit exceeded"
            in 500..599 -> "Server Error ($code): Gemini service problem"
            else -> "HTTP $code"
        } + if (responseBody.isNotBlank()) "\nResponse: $responseBody" else ""
    }

    // Backwards-compatible overload; some callers pass timeoutMs.
    @Suppress("UNUSED_PARAMETER")
    suspend fun ask(prompt: String, timeoutMs: Long): Result<String> = ask(prompt)

    suspend fun ask(prompt: String): Result<String> =
        withContext(Dispatchers.IO) {

            var url = BuildConfig.GEMINI_API_URL.trim()
            if (url.isBlank()) {
                return@withContext Result.failure<String>(IllegalStateException("GEMINI_API_URL not configured"))
            }

            val apiKey = BuildConfig.GEMINI_API_KEY.trim()
            if (apiKey.isBlank()) {
                // This is the most common reason in debug builds.
                return@withContext Result.failure<String>(IllegalStateException("GEMINI_API_KEY is blank. Set geminiApiKey in gradle.properties or input it in Settings."))
            }

            try {
                val isGoogleKey = apiKey.startsWith("AIza")

                if (isGoogleKey && !url.contains("key=")) {
                    url += if (url.contains("?")) "&key=$apiKey" else "?key=$apiKey"
                }

                // ========== Correct Google Gemini payload ==========
                val parts = JSONArray().put(
                    JSONObject().put("text", prompt)
                )

                val contents = JSONArray().put(
                    JSONObject()
                        .put("role", "user")   // Optional but recommended
                        .put("parts", parts)
                )

                val payload = JSONObject().put("contents", contents)

                val body = payload.toString().toRequestBody(JSON)

                val reqBuilder = Request.Builder()
                    .url(url)
                    .post(body)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Android-GeminiSDK/1.0")   // prevent rare Google 403 issue

                if (!isGoogleKey && apiKey.isNotBlank()) {
                    reqBuilder.header("Authorization", "Bearer $apiKey")
                }

                val request = reqBuilder.build()

                client.newCall(request).execute().use { resp ->
                    val code = resp.code
                    val text = resp.body?.string() ?: ""

                    if (!resp.isSuccessful) {
                        // Log a concise message; include body for debugging.
                        val msg = classifyHttpError(code, text)
                        Log.e("GeminiClient", "ask failed: $msg")
                        return@withContext Result.failure(IOException(msg))
                    }

                    // ========== Parse Google Gemini Response ==========
                    try {
                        val root = JSONObject(text)
                        val candidates = root.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val content = candidates.getJSONObject(0).optJSONObject("content")
                            val partsArr = content?.optJSONArray("parts")
                            if (partsArr != null && partsArr.length() > 0) {
                                val resultText = partsArr.getJSONObject(0).optString("text", "")
                                return@withContext Result.success(resultText)
                            }
                        }

                        return@withContext Result.success(text)
                    } catch (_: Exception) {
                        return@withContext Result.success(text)
                    }
                }

            } catch (e: Exception) {
                Log.e("GeminiClient", "ask failed", e)
                return@withContext Result.failure(e)
            }
        }

    suspend fun generateText(prompt: String): String? {
        val res = ask(prompt)
        return res.getOrNull()
    }

    /**
     * Simple MET-based cardio calorie estimator.
     * `avgSpeedKph` is optional and used as a rough intensity hint.
     */
    fun estimateCardioCaloriesSec(durationSec: Int, avgSpeedKph: Float?, weightKg: Float = 70f): Float {
        val met = when {
            avgSpeedKph == null -> 7f
            avgSpeedKph < 6f -> 6f
            avgSpeedKph < 9f -> 8f
            avgSpeedKph < 12f -> 10f
            else -> 12f
        }
        val hours = durationSec / 3600f
        return met * weightKg * hours
    }

    @Deprecated(
        message = "Use estimateCardioCaloriesSec",
        replaceWith = ReplaceWith("estimateCardioCaloriesSec(durationSec, avgSpeedKph, weightKg)")
    )
    fun estimateRunningCaloriesSec(durationSec: Int, avgSpeedKph: Float?, weightKg: Float = 70f): Float =
        estimateCardioCaloriesSec(durationSec, avgSpeedKph, weightKg)

    /**
     * Ask Gemini to estimate calories burned for a cardio session.
     * Falls back to [estimateCardioCaloriesSec] if Gemini is unavailable or output can't be parsed.
     */
    suspend fun estimateCardioCaloriesWithGemini(
        cardioType: CardioType,
        durationSec: Int,
        weightKg: Float,
        avgSpeedKph: Float? = null,
    ): Float {
        if (durationSec <= 0) return 0f

        val minutes = durationSec / 60
        val seconds = durationSec % 60
        val durationText = if (minutes > 0) {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        } else {
            String.format(Locale.US, "%d sec", seconds)
        }

        val speedHint = avgSpeedKph?.let { "平均速度約 ${String.format(Locale.US, "%.1f", it)} km/h" } ?: ""

        val prompt = buildString {
            append("你是一位運動教練與運動生理學助理。\n")
            append("請估算使用者做有氧運動的消耗熱量（kcal）。\n")
            append("運動：${cardioType.displayName}\n")
            append("時間：$durationText\n")
            append("體重：${String.format(Locale.US, "%.1f", weightKg)} kg\n")
            if (speedHint.isNotBlank()) append("強度資訊：$speedHint\n")
            append("請只回覆一個數字（kcal），不要附加文字。例如：245\n")
        }

        val res = ask(prompt)
        val text = res.getOrNull()?.trim().orEmpty()
        val number = text
            .replace("kcal", "", ignoreCase = true)
            .replace("cal", "", ignoreCase = true)
            .replace(Regex("[^0-9.+-]"), "")
            .toFloatOrNull()

        if (number != null && number.isFinite() && number >= 0f) return number

        // fallback
        return estimateCardioCaloriesSec(durationSec, avgSpeedKph, weightKg)
    }
}
