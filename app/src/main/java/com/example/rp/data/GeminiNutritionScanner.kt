package com.example.rp.data

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
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Escáner de etiquetas nutricionales con Gemini API + compresión de imágenes.
 *
 * En Python esto equivaldría a usar `PIL.Image.thumbnail((768, 768))` + `requests.post(...)`
 * dentro de una función `async def`. En Kotlin usamos `suspend fun` con `withContext(Dispatchers.IO)`.
 */
object GeminiNutritionScanner {

    /**
     * Reduce la resolución de un Bitmap para que su lado más largo mida como máximo `maxDimensionPx`
     * (por defecto 768px). Esto reduce drásticamente el consumo de tokens/créditos en Gemini
     * manteniendo perfectamente legibles los números de la tabla nutricional.
     */
    fun downscaleBitmap(original: Bitmap, maxDimensionPx: Int = 768): Bitmap {
        val width = original.width
        val height = original.height
        val longestSide = max(width, height)
        if (longestSide <= maxDimensionPx) return original

        val scale = maxDimensionPx.toFloat() / longestSide.toFloat()
        val newWidth = (width * scale).roundToInt().coerceAtLeast(1)
        val newHeight = (height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true)
    }

    /**
     * Carga un Bitmap desde una URI de la galería del teléfono y le baja la resolución.
     */
    fun loadAndDownscaleFromUri(context: Context, uri: Uri, maxDimensionPx: Int = 768): Bitmap? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val rawBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
                downscaleBitmap(rawBitmap, maxDimensionPx)
            }
        }.getOrNull()
    }

    /**
     * Guarda una copia comprimida de la foto en el almacenamiento interno permanente de la app
     * (`context.filesDir`). No requiere permisos de almacenamiento externo y persiste al apagar el teléfono.
     */
    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String? {
        return runCatching {
            val scaled = downscaleBitmap(bitmap, 768)
            val folder = File(context.filesDir, "ingredient_photos")
            if (!folder.exists()) folder.mkdirs()
            val file = File(folder, "ing_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 78, out)
            }
            file.absolutePath
        }.getOrNull()
    }

    /**
     * Envía la imagen ya reducida de resolución a Gemini API y devuelve `NutritionInfo` por cada 100g/100ml.
     */
    suspend fun scanNutritionLabel(
        bitmap: Bitmap,
        apiKey: String
    ): Result<NutritionInfo> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Configura primero tu API Key de Gemini en la pestaña 'IA / Ajustes'.")
            )
        }

        try {
            // 1. Bajar resolución a máx 768px y comprimir en JPEG al 72% para gastar el mínimo de tokens
            val lowResBitmap = downscaleBitmap(bitmap, 768)
            val outputStream = ByteArrayOutputStream()
            lowResBitmap.compress(Bitmap.CompressFormat.JPEG, 72, outputStream)
            val jpegBytes = outputStream.toByteArray()
            val base64Image = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)

            // 2. Preparar el prompt pidiendo JSON puro con valores estandarizados a 100g / 100ml
            val prompt = """
                Analiza esta etiqueta nutricional. Extrae los valores nutricionales por cada 100g o 100ml
                (si la etiqueta solo muestra por porción, conviértelos proporcionalmente a 100g/100ml).
                Devuelve ÚNICAMENTE un objeto JSON válido con estas claves numéricas exactas:
                {
                  "caloriesKcal": 0.0,
                  "proteinG": 0.0,
                  "carbsG": 0.0,
                  "sugarsG": 0.0,
                  "totalFatG": 0.0,
                  "saturatedFatG": 0.0,
                  "fiberG": 0.0,
                  "sodiumMg": 0.0
                }
            """.trimIndent()

            val requestBody = JSONObject().apply {
                put(
                    "contents",
                    JSONArray().put(
                        JSONObject().apply {
                            put(
                                "parts",
                                JSONArray()
                                    .put(JSONObject().put("text", prompt))
                                    .put(
                                        JSONObject().put(
                                            "inline_data",
                                            JSONObject()
                                                .put("mime_type", "image/jpeg")
                                                .put("data", base64Image)
                                        )
                                    )
                            )
                        }
                    )
                )
                put(
                    "generationConfig",
                    JSONObject().put("response_mime_type", "application/json")
                )
            }

            val endpoint =
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${apiKey.trim()}"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connectTimeout = 20000
                readTimeout = 25000
                doOutput = true
            }

            connection.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            val responseText = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                val err = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Error HTTP $responseCode"
                return@withContext Result.failure( IllegalStateException("Error de Gemini ($responseCode): $err"))
            }

            val rootJson = JSONObject(responseText)
            val textContent = rootJson
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val nutritionJson = JSONObject(textContent)
            val info = NutritionInfo(
                caloriesKcal = nutritionJson.optDouble("caloriesKcal", 0.0),
                proteinG = nutritionJson.optDouble("proteinG", 0.0),
                carbsG = nutritionJson.optDouble("carbsG", 0.0),
                sugarsG = nutritionJson.optDouble("sugarsG", 0.0),
                totalFatG = nutritionJson.optDouble("totalFatG", 0.0),
                saturatedFatG = nutritionJson.optDouble("saturatedFatG", 0.0),
                fiberG = nutritionJson.optDouble("fiberG", 0.0),
                sodiumMg = nutritionJson.optDouble("sodiumMg", 0.0)
            )
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
