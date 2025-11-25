package com.grupo2.ashley.tracking

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class ProductTrackingRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        private const val TAG = "ProductTracking"
        private const val PRODUCTS_COLLECTION = "products"
        private const val PRODUCT_STATS_COLLECTION = "product_stats"
        private const val USER_VIEWS_COLLECTION = "user_views"
    }
    
    /**
     * Registra una vista de producto
     * - Incrementa el contador global de vistas
     * - Guarda estadística diaria
     * - Registra la vista del usuario para evitar duplicados en corto tiempo
     */
    suspend fun trackProductView(productId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid
            val today = getCurrentDate()
            
            // Verificar si el usuario ya vio este producto hoy
            if (currentUserId != null && hasUserViewedToday(productId, currentUserId)) {
                Log.d(TAG, "Usuario ya vio este producto hoy, no incrementar contador")
                return Result.success(Unit)
            }
            
            // Incrementar contador global de vistas
            firestore.collection(PRODUCTS_COLLECTION)
                .document(productId)
                .update("views", FieldValue.increment(1))
                .await()
            
            // Guardar estadística diaria
            val statsRef = firestore.collection(PRODUCTS_COLLECTION)
                .document(productId)
                .collection(PRODUCT_STATS_COLLECTION)
                .document(today)

            Log.d(TAG, "Guardando vista para fecha: $today en producto: $productId")

            statsRef.get().await().let { doc ->
                if (doc.exists()) {
                    // Incrementar vista del día
                    statsRef.update("views", FieldValue.increment(1))
                        .await()
                    Log.d(TAG, "Incrementada vista para $today. Valor anterior: ${doc.getLong("views")}")
                } else {
                    // Crear nuevo registro del día
                    statsRef.set(
                        mapOf(
                            "date" to today,
                            "views" to 1,
                            "favorites" to 0,
                            "messagesReceived" to 0,
                            "timestamp" to System.currentTimeMillis()
                        )
                    ).await()
                    Log.d(TAG, "Creado nuevo documento de estadísticas para $today con 1 vista")
                }
            }
            
            // Registrar vista del usuario
            if (currentUserId != null) {
                firestore.collection(PRODUCTS_COLLECTION)
                    .document(productId)
                    .collection(USER_VIEWS_COLLECTION)
                    .document(currentUserId)
                    .set(
                        mapOf(
                            "userId" to currentUserId,
                            "lastViewed" to System.currentTimeMillis(),
                            "viewCount" to FieldValue.increment(1)
                        )
                    )
                    .await()
            }
            
            Log.d(TAG, "Vista registrada exitosamente para producto: $productId")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al registrar vista: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Verifica si el usuario ya vio este producto hoy
     */
    private suspend fun hasUserViewedToday(productId: String, userId: String): Boolean {
        return try {
            val doc = firestore.collection(PRODUCTS_COLLECTION)
                .document(productId)
                .collection(USER_VIEWS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (doc.exists()) {
                val lastViewed = doc.getLong("lastViewed") ?: 0
                val now = System.currentTimeMillis()
                val hoursSinceLastView = (now - lastViewed) / (1000 * 60 * 60)
                
                // Solo contar como nueva vista si pasaron más de 1 hora
                hoursSinceLastView < 1
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar vista del usuario: ${e.message}", e)
            false
        }
    }
    
    /**
     * Obtiene las estadísticas de los últimos N días
     */
    suspend fun getProductStatsLastDays(productId: String, days: Int = 7): Result<List<DailyProductStats>> {
        return try {
            val stats = mutableListOf<DailyProductStats>()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val calendar = Calendar.getInstance()
            
            for (i in (days - 1) downTo 0) {
                calendar.timeInMillis = System.currentTimeMillis()
                calendar.add(Calendar.DAY_OF_YEAR, -i)
                val date = dateFormat.format(calendar.time)
                
                val doc = firestore.collection(PRODUCTS_COLLECTION)
                    .document(productId)
                    .collection(PRODUCT_STATS_COLLECTION)
                    .document(date)
                    .get()
                    .await()
                
                if (doc.exists()) {
                    stats.add(
                        DailyProductStats(
                            date = date,
                            views = doc.getLong("views")?.toInt() ?: 0,
                            favorites = doc.getLong("favorites")?.toInt() ?: 0,
                            messages = doc.getLong("messagesReceived")?.toInt() ?: 0
                        )
                    )
                } else {
                    stats.add(
                        DailyProductStats(
                            date = date,
                            views = 0,
                            favorites = 0,
                            messages = 0
                        )
                    )
                }
            }
            
            Result.success(stats)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener estadísticas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = dateFormat.format(Date())
        Log.d(TAG, "Fecha actual generada: $date")
        return date
    }
}

data class DailyProductStats(
    val date: String,
    val views: Int,
    val favorites: Int,
    val messages: Int
)
