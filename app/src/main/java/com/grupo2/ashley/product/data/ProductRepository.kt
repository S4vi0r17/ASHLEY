package com.grupo2.ashley.product.data

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.grupo2.ashley.product.models.Product
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ProductRepository {
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

    suspend fun updateProduct(product: Product): Result<Unit> {
        return try {
            val updatedProduct = product.copy(
                updatedAt = System.currentTimeMillis()
            )

            firestore.collection("products")
                .document(product.productId)
                .set(updatedProduct)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            firestore.collection("products")
                .document(productId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
