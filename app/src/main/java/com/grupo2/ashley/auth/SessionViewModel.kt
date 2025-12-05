package com.grupo2.ashley.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.grupo2.ashley.auth.models.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para manejar el estado de la sesión en la UI
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val auth: FirebaseAuth
) : ViewModel() {

    companion object {
        private const val TAG = "SessionViewModel"
    }

    // Estado de la sesión
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Valid)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    // Control del dialog de re-autenticación
    private val _showReauthDialog = MutableStateFlow(false)
    val showReauthDialog: StateFlow<Boolean> = _showReauthDialog.asStateFlow()

    // Error en la re-autenticación
    private val _reauthError = MutableStateFlow<String?>(null)
    val reauthError: StateFlow<String?> = _reauthError.asStateFlow()

    // Estado de carga durante la validación
    private val _isValidating = MutableStateFlow(false)
    val isValidating: StateFlow<Boolean> = _isValidating.asStateFlow()

    // Indica si la sesión necesita ser validada al abrir la app
    private val _needsValidation = MutableStateFlow(true)
    val needsValidation: StateFlow<Boolean> = _needsValidation.asStateFlow()

    /**
     * Verifica la validez de la sesión actual
     */
    fun checkSessionValidity() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Verificando validez de la sesión...")
                val state = sessionManager.validateSession()
                _sessionState.value = state

                when (state) {
                    is SessionState.Valid -> {
                        Log.d(TAG, "Sesión válida")
                        _showReauthDialog.value = false
                        _needsValidation.value = false
                    }
                    is SessionState.Expired -> {
                        Log.d(TAG, "Sesión expirada, mostrando dialog")
                        _showReauthDialog.value = true
                    }
                    is SessionState.Invalid -> {
                        Log.w(TAG, "Sesión inválida")
                        // No mostrar dialog, se manejará en MainActivity
                    }
                    is SessionState.RequiresReauth -> {
                        Log.d(TAG, "Se requiere re-autenticación")
                        _showReauthDialog.value = true
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al verificar sesión", e)
                _sessionState.value = SessionState.Invalid
            }
        }
    }

    /**
     * Re-autentica al usuario con su contraseña
     * @param password Contraseña del usuario
     */
    fun reauthenticateWithPassword(password: String) {
        viewModelScope.launch {
            try {
                _isValidating.value = true
                _reauthError.value = null

                Log.d(TAG, "Intentando re-autenticar usuario...")
                val result = sessionManager.renewSession(password)

                if (result.success) {
                    Log.d(TAG, "Re-autenticación exitosa")
                    _sessionState.value = SessionState.Valid
                    _showReauthDialog.value = false
                    _reauthError.value = null
                    _needsValidation.value = false
                } else {
                    Log.w(TAG, "Re-autenticación fallida: ${result.error}")
                    _reauthError.value = result.error ?: "Error desconocido"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al re-autenticar", e)
                _reauthError.value = e.message ?: "Error inesperado"
            } finally {
                _isValidating.value = false
            }
        }
    }

    /**
     * Actualiza el timestamp de última actividad
     * Esto extiende la sesión por otros 30 minutos
     */
    fun updateActivity() {
        viewModelScope.launch {
            try {
                sessionManager.updateActivity()
                Log.d(TAG, "Actividad actualizada")
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar actividad", e)
            }
        }
    }

    /**
     * Solicita mostrar el dialog de re-autenticación
     */
    fun requestReauthentication() {
        _showReauthDialog.value = true
        _reauthError.value = null
    }

    /**
     * Cancela la re-autenticación y cierra sesión
     */
    fun cancelReauthentication() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Usuario canceló la re-autenticación, cerrando sesión")
                logout()
            } catch (e: Exception) {
                Log.e(TAG, "Error al cancelar re-autenticación", e)
            }
        }
    }

    /**
     * Cierra la sesión del usuario
     */
    fun logout() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Cerrando sesión...")
                sessionManager.invalidateSession()
                auth.signOut()
                _sessionState.value = SessionState.Invalid
                _showReauthDialog.value = false
                _needsValidation.value = true
            } catch (e: Exception) {
                Log.e(TAG, "Error al cerrar sesión", e)
            }
        }
    }

    /**
     * Limpia el error de re-autenticación
     */
    fun clearReauthError() {
        _reauthError.value = null
    }

    /**
     * Obtiene el tiempo restante hasta la expiración de la sesión
     */
    suspend fun getTimeUntilExpiration(): Long {
        return try {
            sessionManager.getTimeUntilExpiration()
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener tiempo de expiración", e)
            0L
        }
    }

    /**
     * Resetea el estado de necesidad de validación
     */
    fun resetNeedsValidation() {
        _needsValidation.value = false
    }
}
