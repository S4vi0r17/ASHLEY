package com.grupo2.ashley.chat.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.grupo2.ashley.profile.models.UserProfile
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for fetching user profiles in chat context
 * Includes in-memory cache to improve performance and reduce Firestore reads
 */
@Singleton
class ChatUserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val profilesCollection = firestore.collection("users")
    private val profileCache = mutableMapOf<String, UserProfile>()

    companion object {
        private const val TAG = "ChatUserRepository"
    }

    /**
     * Obtiene múltiples perfiles de usuario con caché en memoria
     * Retorna un mapa de ID de usuario a perfil de usuario
     */
    suspend fun getUserProfiles(userIds: List<String>): Map<String, UserProfile> {
        val result = mutableMapOf<String, UserProfile>()
        val uncachedUserIds = mutableListOf<String>()

        // Checking de memoria caché :v
        userIds.forEach { userId ->
            profileCache[userId]?.let {
                result[userId] = it
            } ?: run {
                uncachedUserIds.add(userId)
            }
        }

        // Atrae los usuarios que no están en cacheados y en bloques :v
        if (uncachedUserIds.isNotEmpty()) {
            try {
                Log.d(TAG, "Fetching ${uncachedUserIds.size} profiles from Firestore")

                uncachedUserIds.chunked(10).forEach { chunk ->
                    val documents = profilesCollection
                        .whereIn("userId", chunk)
                        .get()
                        .await()

                    documents.forEach { doc ->
                        val profile = doc.toObject(UserProfile::class.java)
                        if (profile.userId.isNotEmpty()) {
                            profileCache[profile.userId] = profile
                            result[profile.userId] = profile
                        }
                    }
                }

                Log.d(TAG, "Fetched and cached ${uncachedUserIds.size} profiles")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching multiple profiles: ${e.message}", e)
            }
        }

        return result
    }
}
