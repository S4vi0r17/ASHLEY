package com.grupo2.ashley.auth.models

/**
 * Representa el estado actual de la sesión del usuario
 */
sealed class SessionState {
    /**
     * La sesión es válida y el usuario puede continuar usando la app
     */
    object Valid : SessionState()

    /**
     * La sesión ha expirado por inactividad (más de 30 minutos)
     */
    object Expired : SessionState()

    /**
     * La sesión es inválida (token corrupto, usuario cambió, etc.)
     */
    object Invalid : SessionState()

    /**
     * Se requiere re-autenticación antes de continuar
     */
    object RequiresReauth : SessionState()
}

/**
 * Datos del token de sesión almacenados en DataStore
 */
data class SessionToken(
    val token: String,
    val userId: String,
    val createdAt: Long,
    val lastActivityAt: Long,
    val expiresAt: Long
) {
    /**
     * Verifica si el token ha expirado
     */
    fun isExpired(): Boolean {
        return System.currentTimeMillis() >= expiresAt
    }

    /**
     * Obtiene el tiempo restante hasta la expiración en milisegundos
     */
    fun getTimeUntilExpiration(): Long {
        return (expiresAt - System.currentTimeMillis()).coerceAtLeast(0)
    }

    /**
     * Obtiene el tiempo de inactividad en milisegundos
     */
    fun getInactivityTime(): Long {
        return System.currentTimeMillis() - lastActivityAt
    }
}

/**
 * Resultado de un intento de re-autenticación
 */
data class ReauthenticationResult(
    val success: Boolean,
    val error: String? = null,
    val newToken: SessionToken? = null
)
