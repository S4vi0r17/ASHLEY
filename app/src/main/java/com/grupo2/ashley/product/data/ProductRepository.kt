package com.grupo2.ashley.product.data

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.grupo2.ashley.product.models.Product
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

class
ProductRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun uploadProductImages(imageUris: List<Uri>): Result<List<String>> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("Usuario no autenticado")
            val imageUrls = mutableListOf<String>()

            for (uri in imageUris) {
                val fileName = "products/${userId}/${UUID.randomUUID()}.jpg"
                val storageRef = storage.reference.child(fileName)
                
                storageRef.putFile(uri).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()
                imageUrls.add(downloadUrl)
            }

            Result.success(imageUrls)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProductImage(imageUrls: List<String>): Result<List<String>> {
        return try {
            val deletedImages = mutableListOf<String>()

            for (url in imageUrls) {
                val storageRef = storage.getReferenceFromUrl(url)
                storageRef.delete().await()
                deletedImages.add(url)
            }
            Result.success(deletedImages)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createProduct(product: Product): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("Usuario no autenticado")
            val userEmail = auth.currentUser?.email ?: ""
            
            val productId = firestore.collection("products").document().id
            val productWithId = product.copy(
                productId = productId,
                userId = userId,
                userEmail = userEmail,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            firestore.collection("products")
                .document(productId)
                .set(productWithId)
                .await()

            Result.success(productId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProduct(product: Product): Result<String> {
        return try {
            val updatedProduct = product.copy(
                updatedAt = System.currentTimeMillis()
            )

            val updateMap = mapOf(
                "title" to updatedProduct.title,
                "brand" to updatedProduct.brand,
                "category" to updatedProduct.category,
                "condition" to updatedProduct.condition,
                "price" to updatedProduct.price,
                "description" to updatedProduct.description,
                "images" to updatedProduct.images,
                "deliveryLocationName" to updatedProduct.deliveryLocationName,
                "deliveryAddress" to updatedProduct.deliveryAddress,
                "deliveryLatitude" to updatedProduct.deliveryLatitude,
                "deliveryLongitude" to updatedProduct.deliveryLongitude,
                "updatedAt" to updatedProduct.updatedAt
            )

            firestore.collection("products")
                .document(product.productId)
                .update(updateMap)
                .await()

            Result.success(product.productId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<String> {
        return try {
            firestore.collection("products")
                .document(productId)
                .delete()
                .await()

            Result.success(productId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllProducts(): Result<List<Product>> {
        return try {
            val snapshot = firestore.collection("products")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val products = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Product::class.java)?.let { firebaseProduct ->
                    Product(
                        productId = firebaseProduct.productId,
                        title = firebaseProduct.title,
                        description = firebaseProduct.description,
                        price = firebaseProduct.price,
                        deliveryAddress = firebaseProduct.deliveryAddress,
                        images = firebaseProduct.images,
                        category = firebaseProduct.category,
                        favorites = firebaseProduct.favorites,
                        brand = firebaseProduct.brand,
                        condition = firebaseProduct.condition,
                        userId = firebaseProduct.userId,
                        userEmail = firebaseProduct.userEmail,
                        createdAt = firebaseProduct.createdAt,
                        deliveryLatitude = firebaseProduct.deliveryLatitude,
                        deliveryLongitude = firebaseProduct.deliveryLongitude,
                        deliveryLocationName = firebaseProduct.deliveryLocationName
                    )
                }
            }

            Result.success(products)
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error al cargar productos", e)
            Result.failure(e)
        }
    }

    /**
     * Observa en tiempo real todos los productos.
     */
    fun observeAllProducts(): Flow<List<Product>> = callbackFlow {
        val query = firestore.collection("products").orderBy("createdAt", Query.Direction.DESCENDING)
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val products = snapshot?.documents?.mapNotNull { doc ->
                try {
                    doc.toObject(Product::class.java)?.copy(productId = doc.id)
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()

            trySend(products)
        }

        awaitClose { listener.remove() }
    }

    /**
     * Observa en tiempo real los productos de un usuario (owner).
     */
    fun observeProductsByUser(userId: String): Flow<List<Product>> = callbackFlow {
        val query = firestore.collection("products").whereEqualTo("userId", userId)
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val products = snapshot?.documents?.mapNotNull { doc ->
                try {
                    doc.toObject(Product::class.java)?.copy(productId = doc.id)
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()

            trySend(products)
        }

        awaitClose { listener.remove() }
    }

    fun filterProducts(
        products: List<Product>,
        categoryId: String,
        searchQuery: String
    ): List<Product> {
        var filtered = products

        // Filtrar por categoría
        if (categoryId != "all") {
            filtered = filtered.filter { it.category == categoryId }
        }

        // Filtrar por búsqueda
        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true) ||
                        it.brand.contains(searchQuery, ignoreCase = true)
            }
        }

        return filtered
    }

    fun toggleFavorite(productId: String, currentProducts: List<Product>): List<Product> {
        return currentProducts.map { product ->
            if (product.productId == productId) {
                product.copy(isFavorite = !product.isFavorite)
            } else {
                product
            }
        }
    }
}
