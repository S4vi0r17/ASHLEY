package com.grupo2.ashley.auth

import android.content.Context
import android.util.Log
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.grupo2.ashley.auth.models.ReauthenticationResult
import com.grupo2.ashley.auth.models.SessionState
import com.grupo2.ashley.auth.models.SessionToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestor central del sistema de tokens de sesión
 * Maneja creación, validación, renovación e invalidación de sesiones
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val TAG = "SessionManager"
        private const val TOKEN_LENGTH = 64
        // Timeout de 30 minutos en milisegundos
        const val SESSION_TIMEOUT_MILLIS = 2 * 60 * 1000L

        // Caracteres válidos para el token
        private const val TOKEN_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    }

    /**
     * Crea una nueva sesión para el usuario actual
     * @param userId ID del usuario de Firebase
     * @return SessionToken creado
     */
    suspend fun createSession(userId: String): SessionToken {
        val currentTime = System.currentTimeMillis()
        val token = generateSecureToken()

        val sessionToken = SessionToken(
            token = token,
            userId = userId,
            createdAt = currentTime,
            lastActivityAt = currentTime,
            expiresAt = currentTime + SESSION_TIMEOUT_MILLIS
        )

        SessionPreferences.saveSessionToken(context, sessionToken)
        Log.d(TAG, "Sesión creada para usuario: $userId")

        return sessionToken
    }

    /**
     * Valida el estado actual de la sesión
     * @return SessionState indicando el estado de la sesión
     */
    suspend fun validateSession(): SessionState {
        try {
            // Verificar que el usuario esté autenticado en Firebase
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.w(TAG, "No hay usuario autenticado en Firebase")
                return SessionState.Invalid
            }

            // Obtener token almacenado
            val sessionToken = SessionPreferences.getSessionToken(context)
            if (sessionToken == null) {
                Log.w(TAG, "No se encontró token de sesión almacenado")
                return SessionState.Invalid
            }

            // Verificar que el userId coincida
            if (sessionToken.userId != currentUser.uid) {
                Log.w(TAG, "El userId del token no coincide con el usuario actual")
                return SessionState.Invalid
            }

            // Verificar si el token ha expirado
            if (sessionToken.isExpired()) {
                Log.d(TAG, "Token de sesión expirado")
                return SessionState.Expired
            }

            // Token válido
            Log.d(TAG, "Sesión válida. Tiempo hasta expiración: ${sessionToken.getTimeUntilExpiration() / 1000}s")
            return SessionState.Valid

        } catch (e: Exception) {
            Log.e(TAG, "Error al validar sesión", e)
            return SessionState.Invalid
        }
    }

    /**
     * Actualiza el timestamp de última actividad
     * Esto extiende la sesión por otros 30 minutos
     */
    suspend fun updateActivity() {
        try {
            val sessionToken = SessionPreferences.getSessionToken(context)
            if (sessionToken != null) {
                val currentTime = System.currentTimeMillis()
                SessionPreferences.updateLastActivity(context, currentTime)
                Log.d(TAG, "Actividad actualizada. Nueva expiración: ${currentTime + SESSION_TIMEOUT_MILLIS}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar actividad", e)
        }
    }

    /**
     * Renueva la sesión mediante re-autenticación con contraseña
     * @param password Contraseña del usuario para re-autenticar
     * @return ReauthenticationResult con el resultado de la operación
     */
    suspend fun renewSession(password: String): ReauthenticationResult {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.w(TAG, "No hay usuario autenticado para renovar sesión")
                return ReauthenticationResult(
                    success = false,
                    error = "No hay usuario autenticado"
                )
            }

            val email = currentUser.email
            if (email == null) {
                Log.w(TAG, "El usuario no tiene email")
                return ReauthenticationResult(
                    success = false,
                    error = "Usuario sin email registrado"
                )
            }

            // Re-autenticar con Firebase
            val credential = EmailAuthProvider.getCredential(email, password)
            currentUser.reauthenticate(credential).await()

            // Crear nuevo token de sesión
            val newToken = createSession(currentUser.uid)

            Log.d(TAG, "Sesión renovada exitosamente para usuario: ${currentUser.uid}")
            return ReauthenticationResult(
                success = true,
                newToken = newToken
            )

        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Log.w(TAG, "Contraseña incorrecta al renovar sesión")
            return ReauthenticationResult(
                success = false,
                error = "Contraseña incorrecta"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error al renovar sesión", e)
            return ReauthenticationResult(
                success = false,
                error = e.message ?: "Error desconocido"
            )
        }
    }

    /**
     * Invalida la sesión actual y limpia los datos almacenados
     */
    suspend fun invalidateSession() {
        try {
            SessionPreferences.clearSessionToken(context)
            Log.d(TAG, "Sesión invalidada")
        } catch (e: Exception) {
            Log.e(TAG, "Error al invalidar sesión", e)
        }
    }

    /**
     * Obtiene el tiempo restante hasta la expiración de la sesión
     * @return Tiempo en milisegundos, o 0 si no hay sesión o está expirada
     */
    suspend fun getTimeUntilExpiration(): Long {
        return try {
            val sessionToken = SessionPreferences.getSessionToken(context)
            sessionToken?.getTimeUntilExpiration() ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener tiempo de expiración", e)
            0L
        }
    }

    /**
     * Verifica si existe una sesión almacenada
     */
    suspend fun hasSession(): Boolean {
        return SessionPreferences.hasSessionToken(context)
    }

    /**
     * Genera un token seguro aleatorio
     * @return String de TOKEN_LENGTH caracteres
     */
    private fun generateSecureToken(): String {
        val random = SecureRandom()
        val token = StringBuilder(TOKEN_LENGTH)

        repeat(TOKEN_LENGTH) {
            val randomIndex = random.nextInt(TOKEN_CHARS.length)
            token.append(TOKEN_CHARS[randomIndex])
        }

        // Agregar timestamp para mayor unicidad
        val timestamp = System.currentTimeMillis()
        return "${token}_$timestamp"
    }
}
