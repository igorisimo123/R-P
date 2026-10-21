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
 * ============================================================================
 * ARCHIVO: GeminiNutritionScanner.kt
 * ============================================================================
 * ¿QUÉ HACE ESTE ARCHIVO?
 * 1. Toma una foto de la cámara o galería.
 * 2. Le baja la resolución (`downscaleBitmap`) y la comprime en JPEG para que pese poquísimos KB
 *    y consuma el mínimo posible de tokens/créditos de la API de Gemini.
 * 3. Hace una petición HTTP POST asíncrona (`suspend fun`) a Google Gemini y convierte la
 *    respuesta JSON en un objeto `NutritionInfo`.
 *
 * EQUIVALENCIAS:
 * - En Python: equivale a usar `PIL.Image.thumbnail((768, 768))` + `requests.post(...)` dentro de un `async def`.
 * - En Kotlin: `suspend fun` con `withContext(Dispatchers.IO)` ejecuta el trabajo pesado de red/imagen
 *   en un hilo secundario para que la pantalla del celular nunca se congele.
 */
object GeminiNutritionScanner {

    /**
     * Reduce proporcionalmente la resolución de un `Bitmap` (imagen en memoria) para que
     * su lado más largo mida como máximo `maxDimensionPx` (768 píxeles por defecto).
     */
    fun downscaleBitmap(original: Bitmap, maxDimensionPx: Int = 768): Bitmap {
        val width = original.width
        val height = original.height
        val longestSide = max(width, height)

        // Si la imagen ya es más pequeña que 768px, la devolvemos tal cual sin gastar CPU
        if (longestSide <= maxDimensionPx) return original

        // Calculamos la escala manteniendo la proporción (aspect ratio)
        val scale = maxDimensionPx.toFloat() / longestSide.toFloat()
        val newWidth = (width * scale).roundToInt().coerceAtLeast(1)
        val newHeight = (height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true)
    }

    /**
     * Abre una foto elegida desde el selector de Galería (`uri`), la decodifica en memoria
     * y le reduce la resolución inmediatamente.
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
     * Guarda una copia comprimida de la foto dentro de la carpeta privada de la app (`context.filesDir`).
     * No necesita pedir permisos de almacenamiento externo y la foto se conserva aunque apagues el teléfono.
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
     * Función asíncrona (`suspend fun`) que envía la imagen en baja resolución a Gemini
     * y devuelve `Result<NutritionInfo>` (éxito con la tabla nutrimental o error si falló la red/API key).
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
            // PASO 1: Reducir resolución a máx 768px y comprimir en JPEG al 72% de calidad.
            // Luego lo codificamos en texto Base64 para enviarlo dentro del JSON.
            val lowResBitmap = downscaleBitmap(bitmap, 768)
            val outputStream = ByteArrayOutputStream()
            lowResBitmap.compress(Bitmap.CompressFormat.JPEG, 72, outputStream)
            val jpegBytes = outputStream.toByteArray()
            val base64Image = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)

            // PASO 2: Prompt indicando a Gemini que extraiga y normalice los valores a 100g/100ml en JSON puro.
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

            // Armamos el JSON que espera la API REST de Gemini
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

            // PASO 3: Petición HTTP POST a la API de Google Gemini
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
                return@withContext Result.failure(IllegalStateException("Error de Gemini ($responseCode): $err"))
            }

            // PASO 4: Extraemos el JSON devuelto por Gemini y rellenamos nuestro struct `NutritionInfo`
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
