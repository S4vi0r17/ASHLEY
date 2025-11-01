package com.grupo2.ashley.profile.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.grupo2.ashley.profile.models.UserProfile
import kotlinx.coroutines.tasks.await

class ProfileRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val profilesCollection = firestore.collection("users")

    companion object {
        private const val TAG = "ProfileRepository"
    }

    /**
     * Obtiene el perfil del usuario actual desde Firestore
     */
    suspend fun getUserProfile(): Result<UserProfile?> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.e(TAG, "No hay usuario autenticado")
                return Result.failure(Exception("Usuario no autenticado"))
            }

            val document = profilesCollection.document(userId).get().await()
            
            if (document.exists()) {
                val profile = document.toObject(UserProfile::class.java)
                Log.d(TAG, "Perfil obtenido exitosamente para userId: $userId")
                Result.success(profile)
            } else {
                Log.d(TAG, "No existe perfil para userId: $userId")
                Result.success(null)
            }
        } catch (e: Exception) {
            // Mejorar manejo de errores offline
            val errorMessage = when {
                e.message?.contains("offline", ignoreCase = true) == true -> 
                    "Sin conexión. Los cambios se sincronizarán cuando tengas internet"
                e.message?.contains("permission", ignoreCase = true) == true -> 
                    "No tienes permisos para acceder al perfil"
                else -> "Error al cargar perfil: ${e.message}"
            }
            Log.e(TAG, errorMessage, e)
            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Verifica si el usuario tiene su perfil completo
     */
    suspend fun isProfileComplete(): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            val document = profilesCollection.document(userId).get().await()
            
            if (document.exists()) {
                val isComplete = document.getBoolean("isProfileComplete") ?: false
                Log.d(TAG, "Perfil completo: $isComplete")
                isComplete
            } else {
                Log.d(TAG, "No existe documento de perfil")
                false
            }
        } catch (e: Exception) {
            // Si está offline, asumir que el perfil puede no estar completo
            Log.w(TAG, "Error al verificar perfil (posiblemente offline): ${e.message}")
            false
        }
    }

    /**
     * Guarda o actualiza el perfil del usuario en Firestore
     */
    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.e(TAG, "No hay usuario autenticado")
                return Result.failure(Exception("Usuario no autenticado"))
            }

            val email = auth.currentUser?.email ?: ""
            
            // Actualizar el perfil con datos del auth
            val updatedProfile = profile.copy(
                userId = userId,
                email = email,
                updatedAt = System.currentTimeMillis(),
                isProfileComplete = isProfileDataComplete(profile)
            )

            profilesCollection.document(userId).set(updatedProfile).await()
            Log.d(TAG, "Perfil guardado exitosamente para userId: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar perfil: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Actualiza campos específicos del perfil
     */
    suspend fun updateProfileFields(fields: Map<String, Any>): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.e(TAG, "No hay usuario autenticado")
                return Result.failure(Exception("Usuario no autenticado"))
            }

            val updatedFields = fields.toMutableMap()
            updatedFields["updatedAt"] = System.currentTimeMillis()

            profilesCollection.document(userId).update(updatedFields).await()
            Log.d(TAG, "Campos actualizados exitosamente")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar campos: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Elimina el perfil del usuario
     */
    suspend fun deleteUserProfile(): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                return Result.failure(Exception("Usuario no autenticado"))
            }

            profilesCollection.document(userId).delete().await()
            Log.d(TAG, "Perfil eliminado exitosamente")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar perfil: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Verifica si los datos del perfil están completos
     */
    private fun isProfileDataComplete(profile: UserProfile): Boolean {
        return profile.firstName.isNotBlank() &&
                profile.lastName.isNotBlank() &&
                profile.phoneNumber.isNotBlank() &&
                profile.address.isNotBlank() &&
                profile.city.isNotBlank()
    }
}
