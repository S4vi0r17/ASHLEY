package com.grupo2.ashley.chat.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class GeminiAIService {

    // For security reasons, it's recommended to move the API key to a more secure location,
    // such as a local.properties file and access it via BuildConfig.
    // API key de Gemini AI
    private val apiKey = "AIzaSyA0rD3HaFD4g4Z6ahHQlDge0eTy_zeTCVU"

    // URL de la API REST de Gemini
    private val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=$apiKey"

    /**
     * Mejora el texto del usuario usando IA de Gemini
     * - Corrige ortografía
     * - Mejora gramática
     * - Hace el mensaje más amigable y claro
     * - Mantiene el tono casual apropiado para chat
     */
    suspend fun improveMessage(originalText: String): Result<String> {
        if (originalText.isBlank()) {
            return Result.failure(Exception("El texto no puede estar vacío"))
        }

        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    Eres un asistente que ayuda a mejorar mensajes de chat para una aplicación de marketplace.

                    Tu tarea es:
                    1. Corregir la ortografía y gramática
                    2. Hacer el mensaje más claro y amigable
                    3. Mantener el tono casual apropiado para chat
                    4. Ser breve y conciso
                    5. NO cambiar completamente el mensaje, solo mejorarlo
                    6. Si el mensaje ya está bien, devuélvelo tal cual o con mejoras mínimas

                    Mensaje original: "$originalText"

                    Devuelve SOLO el mensaje mejorado, sin explicaciones adicionales, sin comillas, sin formato especial.
                """.trimIndent()

                // Crear la conexión HTTP
                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                // Crear el JSON request
                val requestBody = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.7)
                        put("topK", 40)
                        put("topP", 0.95)
                        put("maxOutputTokens", 200)
                    })
                }

                // Enviar request
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody.toString())
                    writer.flush()
                }

                // Leer respuesta
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }

                    // Parsear respuesta JSON
                    val jsonResponse = JSONObject(response)
                    val candidates = jsonResponse.getJSONArray("candidates")
                    if (candidates.length() > 0) {
                        val content = candidates.getJSONObject(0).getJSONObject("content")
                        val parts = content.getJSONArray("parts")
                        if (parts.length() > 0) {
                            val improvedText = parts.getJSONObject(0).getString("text").trim()

                            // Validar que no esté vacío y tenga sentido
                            if (improvedText.isBlank() || improvedText.length > originalText.length * 3) {
                                Result.success(originalText)
                            } else {
                                Result.success(improvedText)
                            }
                        } else {
                            Result.success(originalText)
                        }
                    } else {
                        Result.success(originalText)
                    }
                } else {
                    val errorStream = BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
                    Log.e("GeminiAI", "Error HTTP $responseCode: $errorStream")
                    Result.failure(Exception("Error HTTP $responseCode"))
                }
            } catch (e: Exception) {
                Log.e("GeminiAI", "Error mejorando mensaje: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Verifica si el servicio está configurado correctamente
     */
    fun isConfigured(): Boolean {
        return apiKey.isNotBlank()
    }
}
