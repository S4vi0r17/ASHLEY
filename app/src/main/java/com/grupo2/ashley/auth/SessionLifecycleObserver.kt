package com.grupo2.ashley.auth

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.auth.FirebaseAuth
import com.grupo2.ashley.auth.models.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observador del ciclo de vida de la aplicación para monitorear sesiones
 * Detecta cuando la app va al background y vuelve al foreground
 */
@Singleton
class SessionLifecycleObserver @Inject constructor(
    private val sessionManager: SessionManager,
    private val auth: FirebaseAuth
) : DefaultLifecycleObserver {

    companion object {
        private const val TAG = "SessionLifecycle"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastBackgroundTime: Long = 0

    // Callback para notificar cuando la sesión expira
    var onSessionExpired: (() -> Unit)? = null

    /**
     * Se llama cuando la app viene del background a foreground
     */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        // Verificar si el usuario está autenticado
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.d(TAG, "No hay usuario autenticado, saltando validación de sesión")
            return
        }

        Log.d(TAG, "App volvió a foreground")

        // Calcular tiempo en background
        if (lastBackgroundTime > 0) {
            val timeInBackground = System.currentTimeMillis() - lastBackgroundTime
            val minutesInBackground = timeInBackground / 60000
            Log.d(TAG, "Tiempo en background: $minutesInBackground minutos")
        }

        // Validar la sesión
        scope.launch {
            try {
                val sessionState = sessionManager.validateSession()
                Log.d(TAG, "Estado de sesión al volver: $sessionState")

                when (sessionState) {
                    is SessionState.Expired -> {
                        Log.w(TAG, "Sesión expirada al volver del background")
                        onSessionExpired?.invoke()
                    }
                    is SessionState.Invalid -> {
                        Log.w(TAG, "Sesión inválida al volver del background")
                        onSessionExpired?.invoke()
                    }
                    is SessionState.Valid -> {
                        Log.d(TAG, "Sesión válida, actualizando actividad")
                        sessionManager.updateActivity()
                    }
                    is SessionState.RequiresReauth -> {
                        Log.w(TAG, "Se requiere re-autenticación")
                        onSessionExpired?.invoke()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al validar sesión en onStart", e)
            }
        }
    }

    /**
     * Se llama cuando la app va al background
     */
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)

        // Verificar si el usuario está autenticado
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.d(TAG, "No hay usuario autenticado, saltando guardado de timestamp")
            return
        }

        Log.d(TAG, "App fue al background")
        lastBackgroundTime = System.currentTimeMillis()
    }

    /**
     * Registra este observer en el ProcessLifecycle
     */
    fun register() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        Log.d(TAG, "SessionLifecycleObserver registrado")
    }

    /**
     * Desregistra este observer del ProcessLifecycle
     */
    fun unregister() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        Log.d(TAG, "SessionLifecycleObserver desregistrado")
    }
}
