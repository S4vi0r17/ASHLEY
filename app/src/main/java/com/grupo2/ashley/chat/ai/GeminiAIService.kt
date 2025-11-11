package com.grupo2.ashley.chat.ai

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiAIService {

    // For security reasons, it's recommended to move the API key to a more secure location,
    // such as a local.properties file and access it via BuildConfig.
    private val apiKey = "AIzaSyA0rD3HaFD4g4Z6ahHQlDge0eTy_zeTCVU"

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 0.7f
            topK = 40
            topP = 0.95f
            maxOutputTokens = 200
        }
    )

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

                val response = generativeModel.generateContent(prompt)

                val improvedText = response.text?.trim()

                if (improvedText.isNullOrBlank() || improvedText.length > originalText.length * 3) {
                    Result.success(originalText)
                } else {
                    Result.success(improvedText)
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
