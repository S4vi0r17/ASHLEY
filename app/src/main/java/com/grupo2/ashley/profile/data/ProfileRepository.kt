package com.grupo2.ashley.profile.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.grupo2.ashley.profile.models.UserProfile
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    // Constructor sin parámetros para compatibilidad con código existente
    constructor() : this(
        FirebaseFirestore.getInstance(),
        FirebaseAuth.getInstance()
    )

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

            Log.d(TAG, "Obteniendo perfil para userId: $userId")
            val document = profilesCollection.document(userId).get().await()
            
            if (document.exists()) {
                val profile = document.toObject(UserProfile::class.java)
                Log.d(TAG, "✓ Perfil obtenido exitosamente")
                Log.d(TAG, "  - firstName: ${profile?.firstName}")
                Log.d(TAG, "  - isProfileComplete: ${profile?.isProfileComplete}")
                Result.success(profile)
            } else {
                Log.d(TAG, "✗ No existe perfil en Firestore para userId: $userId")
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
     * Obtiene el perfil de cualquier usuario por su ID
     */
    suspend fun getUserProfile(userId: String): Result<UserProfile?> {
        return try {
            Log.d(TAG, "Obteniendo perfil para userId: $userId")
            val document = profilesCollection.document(userId).get().await()
            
            if (document.exists()) {
                val profile = document.toObject(UserProfile::class.java)
                Log.d(TAG, "✓ Perfil obtenido exitosamente para $userId")
                Log.d(TAG, "  - firstName: ${profile?.firstName}")
                Result.success(profile)
            } else {
                Log.d(TAG, "✗ No existe perfil en Firestore para userId: $userId")
                Result.success(null)
            }
        } catch (e: Exception) {
            val errorMessage = "Error al cargar perfil del usuario: ${e.message}"
            Log.e(TAG, errorMessage, e)
            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Verifica si el usuario tiene su perfil completo
     */
    suspend fun isProfileComplete(): Boolean {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.w(TAG, "Usuario no autenticado al verificar perfil")
                return false
            }
            
            Log.d(TAG, "============================================")
            Log.d(TAG, "Verificando perfil completo para userId: $userId")
            Log.d(TAG, "Leyendo desde: SERVIDOR")
            
            // Forzar lectura desde el servidor en lugar de caché
            val document = profilesCollection.document(userId)
                .get(com.google.firebase.firestore.Source.SERVER)
                .await()
            
            Log.d(TAG, "Documento existe: ${document.exists()}")
            
            if (document.exists()) {
                // Intentar leer ambos campos por compatibilidad
                var isComplete = document.getBoolean("isProfileComplete") ?: false
                val profileComplete = document.getBoolean("profileComplete")
                
                // Si existe el campo viejo "profileComplete", migrar al nuevo
                if (profileComplete != null && !document.contains("isProfileComplete")) {
                    Log.d(TAG, "⚠️ Migrando campo 'profileComplete' a 'isProfileComplete'")
                    isComplete = profileComplete
                    // Actualizar el documento con el nuevo nombre
                    profilesCollection.document(userId).update(
                        mapOf(
                            "isProfileComplete" to profileComplete
                        )
                    ).await()
                }
                
                val firstName = document.getString("firstName") ?: ""
                val lastName = document.getString("lastName") ?: ""
                val phoneNumber = document.getString("phoneNumber") ?: ""
                
                Log.d(TAG, "✓ Documento encontrado:")
                Log.d(TAG, "  - isProfileComplete: $isComplete")
                Log.d(TAG, "  - firstName: '$firstName'")
                Log.d(TAG, "  - lastName: '$lastName'")
                Log.d(TAG, "  - phoneNumber: '$phoneNumber'")
                Log.d(TAG, "  - Todos los datos: ${document.data}")
                Log.d(TAG, "============================================")
                
                isComplete
            } else {
                Log.d(TAG, "✗ No existe documento de perfil")
                Log.d(TAG, "============================================")
                false
            }
        } catch (e: Exception) {
            // Si hay error (offline, etc), intentar leer de caché
            Log.w(TAG, "Error al verificar perfil desde servidor: ${e.message}, intentando caché")
            try {
                val userId = auth.currentUser?.uid ?: return false
                val document = profilesCollection.document(userId)
                    .get(com.google.firebase.firestore.Source.CACHE)
                    .await()
                
                if (document.exists()) {
                    val isComplete = document.getBoolean("isProfileComplete") 
                        ?: document.getBoolean("profileComplete") 
                        ?: false
                    Log.d(TAG, "Perfil obtenido de caché - isProfileComplete: $isComplete")
                    return isComplete
                }
                false
            } catch (cacheError: Exception) {
                Log.e(TAG, "Error al leer de caché: ${cacheError.message}")
                false
            }
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

            Log.d(TAG, "Guardando perfil con isProfileComplete: ${updatedProfile.isProfileComplete}")
            profilesCollection.document(userId).set(updatedProfile).await()
            Log.d(TAG, "Perfil guardado exitosamente para userId: $userId")
            
            // Verificar que se guardó correctamente leyendo desde el servidor
            val verification = profilesCollection.document(userId)
                .get(com.google.firebase.firestore.Source.SERVER)
                .await()
            val savedIsComplete = verification.getBoolean("isProfileComplete") ?: false
            Log.d(TAG, "Verificación post-guardado - isProfileComplete: $savedIsComplete")
            
            if (!savedIsComplete) {
                Log.w(TAG, "Advertencia: El perfil se guardó pero isProfileComplete es false")
            }
            
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
                profile.fullAddress.isNotBlank()
    }
}
