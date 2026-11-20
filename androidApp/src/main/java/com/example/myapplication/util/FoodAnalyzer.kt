package com.example.myapplication.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max

/**
 * Analisa fotos de comida usando a API gratuita do Google Gemini (AI Studio).
 * O utilizador obtém uma chave gratuita em https://aistudio.google.com/app/apikey
 * (sem cartão de crédito necessário no free tier).
 */
object FoodAnalyzer {

    // Modelo multimodal gratuito com boa capacidade de visão
    private const val MODEL = "gemini-2.0-flash"
    private const val API_BASE = "https://generativelanguage.googleapis.com/v1beta/models"

    data class FoodItem(
        val name: String,
        val calories: Int,
        val proteinG: Float? = null,
        val carbsG: Float? = null,
        val fatG: Float? = null
    )

    class Result(val items: List<FoodItem>, val jpeg: ByteArray)

    class AnalysisException(val isParse: Boolean, message: String) : Exception(message)

    suspend fun analyze(context: Context, uri: Uri, apiKey: String, languageCode: String): Result =
        withContext(Dispatchers.IO) {
            val jpeg = loadScaledJpeg(context, uri)
            val base64 = Base64.encodeToString(jpeg, Base64.NO_WRAP)
            val responseText = callApi(apiKey, base64, languageName(languageCode))
            Result(parseItems(responseText), jpeg)
        }

    private fun languageName(code: String): String = when (code) {
        "pt" -> "European Portuguese"
        "fr" -> "French"
        else -> "English"
    }

    private fun loadScaledJpeg(context: Context, uri: Uri, maxSide: Int = 1280): ByteArray {
        val bounds = BitmapFactory.Options()
        bounds.inJustDecodeBounds = true
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > maxSide * 2) sample *= 2

        val opts = BitmapFactory.Options()
        opts.inSampleSize = sample
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: throw AnalysisException(false, "Could not read the image")

        val scale = maxSide.toFloat() / max(decoded.width, decoded.height)
        val bitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else decoded

        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        if (bitmap != decoded) bitmap.recycle()
        decoded.recycle()
        return out.toByteArray()
    }

    private fun callApi(apiKey: String, imageBase64: String, language: String): String {
        val prompt = "Identify every distinct food and drink in this photo. For each one, estimate the " +
            "portion size and its nutrition. Reply with ONLY a JSON object, no other text, in this exact shape: " +
            "{\"items\":[{\"name\":\"...\",\"calories\":0,\"protein_g\":0,\"carbs_g\":0,\"fat_g\":0}]}. " +
            "Write each name in $language and include the approximate portion, for example \"Grilled chicken breast (150 g)\". " +
            "calories is an integer in kcal for that portion. If there is no food in the photo, reply {\"items\":[]}."

        // Gemini generateContent body
        val inlineData = JSONObject()
            .put("mime_type", "image/jpeg")
            .put("data", imageBase64)

        val imagePart = JSONObject()
            .put("inline_data", inlineData)

        val textPart = JSONObject()
            .put("text", prompt)

        val parts = JSONArray().put(imagePart).put(textPart)
        val content = JSONObject().put("parts", parts)
        val body = JSONObject()
            .put("contents", JSONArray().put(content))
            .put("generationConfig", JSONObject()
                .put("temperature", 0.2)
                .put("maxOutputTokens", 1024)
            )

        val url = "$API_BASE/$MODEL:generateContent?key=${apiKey.trim()}"
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 20000
            conn.readTimeout = 60000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                val detail = try {
                    val err = JSONObject(text)
                    err.optJSONObject("error")?.optString("message")
                        ?: err.optString("message", text.take(200))
                } catch (e: Exception) {
                    text.take(200)
                }
                throw AnalysisException(false, "HTTP $code: $detail")
            }

            // Extract text from Gemini response
            val candidates = JSONObject(text).optJSONArray("candidates")
                ?: throw AnalysisException(true, "No candidates in response")
            if (candidates.length() == 0) throw AnalysisException(true, "Empty candidates")
            val contentObj = candidates.getJSONObject(0).optJSONObject("content")
                ?: throw AnalysisException(true, "No content")
            val partsArr = contentObj.optJSONArray("parts")
                ?: throw AnalysisException(true, "No parts")
            val sb = StringBuilder()
            for (i in 0 until partsArr.length()) {
                val p = partsArr.getJSONObject(i)
                if (p.has("text")) sb.append(p.getString("text"))
            }
            sb.toString()
        } finally {
            conn.disconnect()
        }
    }

    private fun parseItems(raw: String): List<FoodItem> {
        // Gemini sometimes wraps JSON in markdown code fences
        var cleaned = raw.trim()
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```json").removePrefix("```").trim()
            if (cleaned.endsWith("```")) cleaned = cleaned.removeSuffix("```").trim()
        }
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        if (start < 0 || end <= start) throw AnalysisException(true, "No JSON")
        val items = try {
            JSONObject(cleaned.substring(start, end + 1)).getJSONArray("items")
        } catch (e: Exception) {
            throw AnalysisException(true, "Invalid JSON")
        }
        val list = ArrayList<FoodItem>()
        for (i in 0 until items.length()) {
            val o = items.getJSONObject(i)
            val name = o.optString("name").trim()
            if (name.isEmpty()) continue
            list.add(
                FoodItem(
                    name = name,
                    calories = o.optInt("calories", 0).coerceAtLeast(0),
                    proteinG = o.optDouble("protein_g", Double.NaN).let { if (it.isNaN()) null else it.toFloat() },
                    carbsG = o.optDouble("carbs_g", Double.NaN).let { if (it.isNaN()) null else it.toFloat() },
                    fatG = o.optDouble("fat_g", Double.NaN).let { if (it.isNaN()) null else it.toFloat() }
                )
            )
        }
        return list
    }
}
