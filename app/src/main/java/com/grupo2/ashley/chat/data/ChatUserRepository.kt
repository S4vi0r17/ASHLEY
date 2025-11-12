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
     * Gets user profile by userId with caching
     * @param userId The user ID to fetch profile for
     * @return UserProfile or null if not found
     */
    suspend fun getUserProfile(userId: String): UserProfile? {
        // Check cache first
        profileCache[userId]?.let {
            Log.d(TAG, "Profile cache hit for userId: $userId")
            return it
        }

        // Fetch from Firestore
        return try {
            Log.d(TAG, "Fetching profile from Firestore for userId: $userId")
            val document = profilesCollection.document(userId).get().await()

            if (document.exists()) {
                val profile = document.toObject(UserProfile::class.java)
                if (profile != null) {
                    // Cache the profile
                    profileCache[userId] = profile
                    Log.d(TAG, "Profile fetched and cached: ${profile.firstName} ${profile.lastName}")
                    profile
                } else {
                    Log.w(TAG, "Profile document exists but couldn't be parsed for userId: $userId")
                    null
                }
            } else {
                Log.w(TAG, "No profile found for userId: $userId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching profile for userId $userId: ${e.message}", e)
            null
        }
    }

    /**
     * Gets multiple user profiles at once with caching
     * @param userIds List of user IDs to fetch
     * @return Map of userId to UserProfile (only includes found profiles)
     */
    suspend fun getUserProfiles(userIds: List<String>): Map<String, UserProfile> {
        val result = mutableMapOf<String, UserProfile>()
        val uncachedUserIds = mutableListOf<String>()

        // Check cache for each user
        userIds.forEach { userId ->
            profileCache[userId]?.let {
                result[userId] = it
            } ?: run {
                uncachedUserIds.add(userId)
            }
        }

        // Fetch uncached profiles from Firestore
        if (uncachedUserIds.isNotEmpty()) {
            try {
                Log.d(TAG, "Fetching ${uncachedUserIds.size} profiles from Firestore")

                // Firestore allows querying up to 10 documents with 'in' operator
                // Split into chunks of 10 if needed
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

    /**
     * Clears the profile cache
     */
    fun clearCache() {
        profileCache.clear()
        Log.d(TAG, "Profile cache cleared")
    }

    /**
     * Removes a specific profile from cache
     * @param userId The user ID to remove from cache
     */
    fun removeCacheEntry(userId: String) {
        profileCache.remove(userId)
        Log.d(TAG, "Removed cache entry for userId: $userId")
    }

    /**
     * Gets cache size for debugging
     * @return Number of cached profiles
     */
    fun getCacheSize(): Int = profileCache.size
}
