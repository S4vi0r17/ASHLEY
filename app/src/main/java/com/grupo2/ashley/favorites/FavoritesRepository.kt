package com.grupo2.ashley.favorites

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.util.*

class FavoritesRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        private const val TAG = "FavoritesRepository"
        private const val FAVORITES_COLLECTION = "favorites"
        private const val PRODUCTS_COLLECTION = "products"
        private const val PRODUCT_STATS_COLLECTION = "product_stats"
    }
    
    /**
     * Agrega un producto a favoritos
     */
    suspend fun addToFavorites(productId: String, productTitle: String, productImage: String, productPrice: Double): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid 
                ?: return Result.failure(Exception("Usuario no autenticado"))
            val today = getCurrentDate()
            // Agregar a la colección de favoritos del usuario
            firestore.collection("users")
                .document(userId)
                .collection(FAVORITES_COLLECTION)
                .document(productId)
                .set(
                    mapOf(
                        "productId" to productId,
                        "userId" to userId,
                        "productTitle" to productTitle,
                        "productImage" to productImage,
                        "productPrice" to productPrice,
                        "addedAt" to System.currentTimeMillis()
                    )
                )
                .await()
            // Incrementar contador de favoritos del producto
            firestore.collection(PRODUCTS_COLLECTION)
                .document(productId)
                .update("favorites", FieldValue.increment(1))
                .await()
            // Actualizar estadística diaria
            val statsRef = firestore.collection(PRODUCTS_COLLECTION)
                .document(productId)
                .collection(PRODUCT_STATS_COLLECTION)
                .document(today)

            Log.d(TAG, "Guardando favorito para fecha: $today en producto: $productId")

            val statsDoc = statsRef.get().await()
            if (statsDoc.exists()) {
                // Si ya existe, incrementa el campo 'favorites' SOLO para hoy
                statsRef.update("favorites", FieldValue.increment(1)).await()
                Log.d(TAG, "Incrementado favoritos para $today. Valor anterior: ${statsDoc.getLong("favorites")}")
            } else {
                // Si no existe, crea el documento con 'favorites' en 1
                statsRef.set(
                    mapOf(
                        "date" to today,
                        "views" to 0,
                        "favorites" to 1,
                        "messagesReceived" to 0,
                        "timestamp" to System.currentTimeMillis()
                    )
                ).await()
                Log.d(TAG, "Creado nuevo documento de estadísticas para $today con 1 favorito")
            }
            Log.d(TAG, "Producto agregado a favoritos: $productId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error al agregar a favoritos: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Remueve un producto de favoritos
     */
    suspend fun removeFromFavorites(productId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid 
                ?: return Result.failure(Exception("Usuario no autenticado"))
            val today = getCurrentDate()
            // Remover de la colección de favoritos del usuario
            firestore.collection("users")
                .document(userId)
                .collection(FAVORITES_COLLECTION)
                .document(productId)
                .delete()
                .await()
            // Decrementar contador de favoritos del producto
            firestore.collection(PRODUCTS_COLLECTION)
                .document(productId)
                .update("favorites", FieldValue.increment(-1))
                .await()
            // Actualizar estadística diaria SOLO para hoy
            val statsRef = firestore.collection(PRODUCTS_COLLECTION)
                .document(productId)
                .collection(PRODUCT_STATS_COLLECTION)
                .document(today)
            val statsDoc = statsRef.get().await()
            if (statsDoc.exists()) {
                val currentFavorites = statsDoc.getLong("favorites")?.toInt() ?: 0
                if (currentFavorites > 0) {
                    statsRef.update("favorites", FieldValue.increment(-1)).await()
                }
            }
            Log.d(TAG, "Producto removido de favoritos: $productId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error al remover de favoritos: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Verifica si un producto está en favoritos
     */
    suspend fun isFavorite(productId: String): Result<Boolean> {
        return try {
            val userId = auth.currentUser?.uid 
                ?: return Result.success(false)
            
            val doc = firestore.collection("users")
                .document(userId)
                .collection(FAVORITES_COLLECTION)
                .document(productId)
                .get()
                .await()
            
            Result.success(doc.exists())
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar favorito: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Obtiene todos los favoritos del usuario
     */
    suspend fun getUserFavorites(): Result<List<FavoriteProduct>> {
        return try {
            val userId = auth.currentUser?.uid 
                ?: return Result.failure(Exception("Usuario no autenticado"))
            
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection(FAVORITES_COLLECTION)
                .orderBy("addedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            
            val favorites = snapshot.documents.mapNotNull { doc ->
                try {
                    FavoriteProduct(
                        productId = doc.getString("productId") ?: "",
                        userId = doc.getString("userId") ?: "",
                        productTitle = doc.getString("productTitle") ?: "",
                        productImage = doc.getString("productImage") ?: "",
                        productPrice = doc.getDouble("productPrice") ?: 0.0,
                        addedAt = doc.getLong("addedAt") ?: 0L
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error al parsear favorito: ${e.message}")
                    null
                }
            }
            
            Result.success(favorites)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener favoritos: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Observa en tiempo real los IDs de productos marcados como favoritos por el usuario.
     * Emite un Set de productId cada vez que cambian.
     */
    fun observeUserFavoriteIds(): Flow<Set<String>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(emptySet())
            close()
            return@callbackFlow
        }

        val favoritesRef = firestore.collection("users")
            .document(userId)
            .collection(FAVORITES_COLLECTION)

        val listener = favoritesRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // En caso de error, no cerramos el flow, solo enviamos vacío
                trySend(emptySet())
                return@addSnapshotListener
            }

            val ids = snapshot?.documents?.mapNotNull { it.getString("productId") }?.toSet() ?: emptySet()
            trySend(ids)
        }

        awaitClose { listener.remove() }
    }
    
    /**
     * Alterna el estado de favorito (agregar/remover)
     */
    suspend fun toggleFavorite(
        productId: String,
        productTitle: String,
        productImage: String,
        productPrice: Double
    ): Result<Boolean> {
        return try {
            val isFav = isFavorite(productId).getOrNull() ?: false
            
            if (isFav) {
                removeFromFavorites(productId).getOrThrow()
                Result.success(false)
            } else {
                addToFavorites(productId, productTitle, productImage, productPrice).getOrThrow()
                Result.success(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al alternar favorito: ${e.message}", e)
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

data class FavoriteProduct(
    val productId: String,
    val userId: String,
    val productTitle: String,
    val productImage: String,
    val productPrice: Double,
    val addedAt: Long
)
