package com.grupo2.ashley.auth

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rastrea la actividad del usuario y actualiza el timestamp de última actividad
 * Esto extiende automáticamente la sesión mientras el usuario esté activo
 */
@Singleton
class ActivityTracker @Inject constructor(
    private val sessionManager: SessionManager
) {
    companion object {
        private const val TAG = "ActivityTracker"

        // Intervalo de actualización: cada 1 minuto de actividad
        private const val UPDATE_INTERVAL_MS = 60_000L
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var updateJob: Job? = null
    private var lastActivityTime: Long = 0
    private var isTracking = false

    /**
     * Registra una actividad del usuario
     * Esto se debe llamar cuando el usuario interactúa con la app
     */
    fun trackActivity() {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastActivity = currentTime - lastActivityTime

        // Solo actualizar si ha pasado más del intervalo mínimo
        if (timeSinceLastActivity >= UPDATE_INTERVAL_MS) {
            lastActivityTime = currentTime

            scope.launch {
                try {
                    sessionManager.updateActivity()
                    Log.d(TAG, "Actividad del usuario registrada")
                } catch (e: Exception) {
                    Log.e(TAG, "Error al registrar actividad", e)
                }
            }
        }
    }

    /**
     * Inicia el rastreo automático de actividad
     */
    fun startTracking() {
        if (isTracking) {
            Log.d(TAG, "Ya se está rastreando la actividad")
            return
        }

        isTracking = true
        lastActivityTime = System.currentTimeMillis()

        updateJob = scope.launch {
            while (isTracking) {
                delay(UPDATE_INTERVAL_MS)

                try {
                    // Verificar si hubo actividad reciente
                    val timeSinceLastActivity = System.currentTimeMillis() - lastActivityTime

                    if (timeSinceLastActivity < UPDATE_INTERVAL_MS * 2) {
                        // Hubo actividad reciente, actualizar
                        sessionManager.updateActivity()
                        Log.d(TAG, "Actividad actualizada automáticamente")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error en actualización automática de actividad", e)
                }
            }
        }

        Log.d(TAG, "Rastreo de actividad iniciado")
    }

    /**
     * Detiene el rastreo de actividad
     */
    fun stopTracking() {
        isTracking = false
        updateJob?.cancel()
        updateJob = null
        Log.d(TAG, "Rastreo de actividad detenido")
    }

    /**
     * Reinicia el contador de actividad
     */
    fun resetActivityTimer() {
        lastActivityTime = System.currentTimeMillis()
    }
}
