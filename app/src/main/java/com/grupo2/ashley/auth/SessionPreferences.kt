package com.grupo2.ashley.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.grupo2.ashley.auth.models.SessionToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Gestiona el almacenamiento persistente del token de sesión usando DataStore
 */
object SessionPreferences {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session_data")

    // Claves para almacenar los datos del token
    private val SESSION_TOKEN = stringPreferencesKey("session_token")
    private val SESSION_USER_ID = stringPreferencesKey("session_user_id")
    private val SESSION_CREATED_AT = longPreferencesKey("session_created_at")
    private val SESSION_LAST_ACTIVITY = longPreferencesKey("session_last_activity")
    private val SESSION_EXPIRES_AT = longPreferencesKey("session_expires_at")

    /**
     * Guarda un token de sesión en DataStore
     */
    suspend fun saveSessionToken(context: Context, sessionToken: SessionToken) {
        context.dataStore.edit { preferences ->
            preferences[SESSION_TOKEN] = sessionToken.token
            preferences[SESSION_USER_ID] = sessionToken.userId
            preferences[SESSION_CREATED_AT] = sessionToken.createdAt
            preferences[SESSION_LAST_ACTIVITY] = sessionToken.lastActivityAt
            preferences[SESSION_EXPIRES_AT] = sessionToken.expiresAt
        }
    }

    /**
     * Obtiene el token de sesión almacenado
     * @return SessionToken si existe, null si no hay token guardado
     */
    suspend fun getSessionToken(context: Context): SessionToken? {
        val preferences = context.dataStore.data.first()

        val token = preferences[SESSION_TOKEN] ?: return null
        val userId = preferences[SESSION_USER_ID] ?: return null
        val createdAt = preferences[SESSION_CREATED_AT] ?: return null
        val lastActivityAt = preferences[SESSION_LAST_ACTIVITY] ?: return null
        val expiresAt = preferences[SESSION_EXPIRES_AT] ?: return null

        return SessionToken(
            token = token,
            userId = userId,
            createdAt = createdAt,
            lastActivityAt = lastActivityAt,
            expiresAt = expiresAt
        )
    }

    /**
     * Obtiene un Flow del token de sesión para observar cambios
     */
    fun sessionTokenFlow(context: Context): Flow<SessionToken?> {
        return context.dataStore.data.map { preferences ->
            val token = preferences[SESSION_TOKEN] ?: return@map null
            val userId = preferences[SESSION_USER_ID] ?: return@map null
            val createdAt = preferences[SESSION_CREATED_AT] ?: return@map null
            val lastActivityAt = preferences[SESSION_LAST_ACTIVITY] ?: return@map null
            val expiresAt = preferences[SESSION_EXPIRES_AT] ?: return@map null

            SessionToken(
                token = token,
                userId = userId,
                createdAt = createdAt,
                lastActivityAt = lastActivityAt,
                expiresAt = expiresAt
            )
        }
    }

    /**
     * Actualiza el timestamp de última actividad
     */
    suspend fun updateLastActivity(context: Context, timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[SESSION_LAST_ACTIVITY] = timestamp

            // Recalcular expiración basado en la nueva actividad
            val newExpiresAt = timestamp + SESSION_TIMEOUT_MILLIS
            preferences[SESSION_EXPIRES_AT] = newExpiresAt
        }
    }

    /**
     * Limpia el token de sesión de DataStore
     */
    suspend fun clearSessionToken(context: Context) {
        context.dataStore.edit { preferences ->
            preferences.remove(SESSION_TOKEN)
            preferences.remove(SESSION_USER_ID)
            preferences.remove(SESSION_CREATED_AT)
            preferences.remove(SESSION_LAST_ACTIVITY)
            preferences.remove(SESSION_EXPIRES_AT)
        }
    }

    /**
     * Verifica si existe un token de sesión almacenado
     */
    suspend fun hasSessionToken(context: Context): Boolean {
        val preferences = context.dataStore.data.first()
        return preferences[SESSION_TOKEN] != null
    }

    // Timeout de 2 minutos en milisegundos (para testing)
    private const val SESSION_TIMEOUT_MILLIS = 2 * 60 * 1000L
}
