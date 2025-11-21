package com.grupo2.ashley.dashboard.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.grupo2.ashley.dashboard.models.CategoryStats
import com.grupo2.ashley.dashboard.models.DailyStats
import com.grupo2.ashley.dashboard.models.ProductSummary
import com.grupo2.ashley.dashboard.models.UserStats
import com.grupo2.ashley.product.models.Product
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StatsRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        private const val TAG = "StatsRepository"
        private const val PRODUCTS_COLLECTION = "products"
    }
    
    suspend fun getUserStats(): Result<UserStats> {
        return try {
            val userId = auth.currentUser?.uid 
                ?: return Result.failure(Exception("Usuario no autenticado"))
            
            // Obtener todos los productos del usuario
            val productsSnapshot = firestore.collection(PRODUCTS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val products = productsSnapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Product::class.java)?.copy(productId = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error al parsear producto: ${e.message}")
                    null
                }
            }
            
            Log.d(TAG, "Productos encontrados: ${products.size}")
            
            // Calcular estadísticas
            val activeProducts = products.count { it.isActive }
            val inactiveProducts = products.count { !it.isActive }
            val totalViews = products.sumOf { it.views }
            val totalFavorites = products.sumOf { it.favorites }
            
            // Productos por categoría
            val productsByCategory = products.groupBy { it.category }
                .mapValues { it.value.size }
            
            // Productos por condición
            val productsByCondition = products.groupBy { it.condition.displayName }
                .mapValues { it.value.size }
            
            // Precio promedio
            val averagePrice = if (products.isNotEmpty()) {
                products.map { it.price }.average()
            } else 0.0
            
            // Producto más visto
            val mostViewedProduct = products
                .maxByOrNull { it.views }
                ?.let { toProductSummary(it) }
            
            // Producto más favorito
            val mostFavoritedProduct = products
                .maxByOrNull { it.favorites }
                ?.let { toProductSummary(it) }
            
            // Productos recientes (últimos 5)
            val recentProducts = products
                .sortedByDescending { it.createdAt }
                .take(5)
                .map { toProductSummary(it) }
            
            // Estadísticas de los últimos 7 días - DATOS REALES
            val viewsLast7Days = getUserDailyStats(userId, 7).getOrElse { emptyList() }
            
            // Obtener fecha de registro del usuario
            val userDoc = firestore.collection("users")
                .document(userId)
                .get()
                .await()
            
            val memberSince = userDoc.getLong("createdAt") ?: System.currentTimeMillis()
            
            // Calcular total de mensajes recibidos
            val totalMessages = products.sumOf { it.messagesReceived }

            val stats = UserStats(
                totalProductsPublished = products.size,
                activeProducts = activeProducts,
                inactiveProducts = inactiveProducts,
                totalViews = totalViews,
                totalFavorites = totalFavorites,
                totalMessages = totalMessages,
                categoriesUsed = productsByCategory.keys.size,
                averagePrice = averagePrice,
                mostViewedProduct = mostViewedProduct,
                mostFavoritedProduct = mostFavoritedProduct,
                recentProducts = recentProducts,
                productsByCategory = productsByCategory,
                productsByCondition = productsByCondition,
                viewsLast7Days = viewsLast7Days,
                memberSince = memberSince
            )
            
            Log.d(TAG, "Estadísticas calculadas: $stats")
            Result.success(stats)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener estadísticas: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun getCategoryStats(): Result<List<CategoryStats>> {
        return try {
            val userId = auth.currentUser?.uid 
                ?: return Result.failure(Exception("Usuario no autenticado"))
            
            val productsSnapshot = firestore.collection(PRODUCTS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val products = productsSnapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Product::class.java)
                } catch (e: Exception) {
                    null
                }
            }
            
            val categoryStats = products
                .groupBy { it.category }
                .map { (category, categoryProducts) ->
                    CategoryStats(
                        categoryName = category,
                        productCount = categoryProducts.size,
                        totalViews = categoryProducts.sumOf { it.views },
                        totalFavorites = categoryProducts.sumOf { it.favorites },
                        averagePrice = categoryProducts.map { it.price }.average()
                    )
                }
                .sortedByDescending { it.productCount }
            
            Result.success(categoryStats)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener estadísticas por categoría: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private fun toProductSummary(product: Product): ProductSummary {
        return ProductSummary(
            productId = product.productId,
            title = product.title,
            imageUrl = product.images.firstOrNull() ?: "",
            price = product.price,
            views = product.views,
            favorites = product.favorites,
            category = product.category,
            createdAt = product.createdAt,
            isActive = product.isActive
        )
    }

    /**
     * Obtiene estadísticas diarias reales de TODOS los productos del usuario
     */
    suspend fun getUserDailyStats(userId: String, days: Int = 7): Result<List<DailyStats>> {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            val statsMap = mutableMapOf<String, DailyStats>()
            
            // Inicializar mapa con últimos 7 días
            for (i in (days - 1) downTo 0) {
                calendar.timeInMillis = System.currentTimeMillis()
                calendar.add(Calendar.DAY_OF_YEAR, -i)
                val date = dateFormat.format(calendar.time)
                statsMap[date] = DailyStats(date = date, views = 0, favorites = 0, messages = 0)
            }
            
            // Obtener todos los productos del usuario
            val productsSnapshot = firestore.collection(PRODUCTS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            // Para cada producto, obtener sus estadísticas diarias
            for (productDoc in productsSnapshot.documents) {
                val productId = productDoc.id
                
                // Obtener subcollection de estadísticas
                val statsSnapshot = firestore.collection(PRODUCTS_COLLECTION)
                    .document(productId)
                    .collection("product_stats")
                    .get()
                    .await()
                
                // Sumar estadísticas de cada día
                for (statDoc in statsSnapshot.documents) {
                    val date = statDoc.getString("date") ?: continue
                    if (statsMap.containsKey(date)) {
                        val views = statDoc.getLong("views")?.toInt() ?: 0
                        val favorites = statDoc.getLong("favorites")?.toInt() ?: 0
                        val messages = statDoc.getLong("messagesReceived")?.toInt() ?: 0
                        
                        val currentStats = statsMap[date]!!
                        statsMap[date] = DailyStats(
                            date = date,
                            views = currentStats.views + views,
                            favorites = currentStats.favorites + favorites,
                            messages = currentStats.messages + messages
                        )
                    }
                }
            }
            
            // Convertir a lista ordenada por fecha
            val statsList = statsMap.values.sortedBy { it.date }
            Result.success(statsList)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener estadísticas diarias: ${e.message}", e)
            Result.failure(e)
        }
    }
}
