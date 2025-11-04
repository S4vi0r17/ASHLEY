package com.grupo2.ashley.profile.data

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ImageStorageRepository {
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        private const val TAG = "ImageStorageRepository"
        private const val PROFILE_IMAGES_PATH = "profile_images"
    }

    /**
     * Sube una imagen de perfil a Firebase Storage
     * @param imageUri URI de la imagen seleccionada
     * @return URL de descarga de la imagen subida
     */
    suspend fun uploadProfileImage(imageUri: Uri): Result<String> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.e(TAG, "No hay usuario autenticado")
                return Result.failure(Exception("Usuario no autenticado"))
            }

            // Generar nombre único para la imagen
            val imageName = "profile_${userId}_${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference
                .child(PROFILE_IMAGES_PATH)
                .child(userId)
                .child(imageName)

            Log.d(TAG, "Subiendo imagen: $imageName")

            // Subir imagen
            val uploadTask = storageRef.putFile(imageUri).await()
            
            // Obtener URL de descarga
            val downloadUrl = storageRef.downloadUrl.await()
            
            Log.d(TAG, "Imagen subida exitosamente: ${downloadUrl}")
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("storage/unauthorized", ignoreCase = true) == true ->
                    "No tienes permisos para subir imágenes"
                e.message?.contains("storage/canceled", ignoreCase = true) == true ->
                    "Subida cancelada"
                e.message?.contains("storage/retry-limit-exceeded", ignoreCase = true) == true ->
                    "Error de conexión. Intenta nuevamente"
                else -> "Error al subir imagen: ${e.message}"
            }
            Log.e(TAG, errorMessage, e)
            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Elimina una imagen de perfil anterior de Firebase Storage
     * @param imageUrl URL de la imagen a eliminar
     */
    suspend fun deleteProfileImage(imageUrl: String): Result<Unit> {
        return try {
            if (imageUrl.isBlank()) {
                return Result.success(Unit)
            }

            val storageRef = storage.getReferenceFromUrl(imageUrl)
            storageRef.delete().await()
            
            Log.d(TAG, "Imagen anterior eliminada exitosamente")
            Result.success(Unit)
        } catch (e: Exception) {
            // No fallar si la imagen no existe
            Log.w(TAG, "No se pudo eliminar la imagen anterior: ${e.message}")
            Result.success(Unit)
        }
    }

    /**
     * Actualiza la imagen de perfil (elimina la anterior y sube la nueva)
     * @param newImageUri URI de la nueva imagen
     * @param oldImageUrl URL de la imagen anterior (opcional)
     * @return URL de descarga de la nueva imagen
     */
    suspend fun updateProfileImage(newImageUri: Uri, oldImageUrl: String? = null): Result<String> {
        return try {
            // Subir nueva imagen
            val uploadResult = uploadProfileImage(newImageUri)
            
            if (uploadResult.isSuccess) {
                // Si hay imagen anterior, eliminarla
                oldImageUrl?.let {
                    if (it.isNotBlank()) {
                        deleteProfileImage(it)
                    }
                }
            }
            
            uploadResult
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar imagen: ${e.message}", e)
            Result.failure(e)
        }
    }
}
