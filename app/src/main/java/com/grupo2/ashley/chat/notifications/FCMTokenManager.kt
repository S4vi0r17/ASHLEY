package com.grupo2.ashley.chat.notifications

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FCMTokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseDb: DatabaseReference,
    private val auth: FirebaseAuth
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "FCMTokenManager"
        private const val PREFS_NAME = "fcm_prefs"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_TOKEN_SYNCED = "token_synced"
    }

    /**
     * Get the current FCM token, request a new one if not available
     */
    suspend fun getToken(): String? {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "FCM Token retrieved: $token")
            saveToken(token)
            token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get FCM token", e)
            null
        }
    }

    /**
     * Save token locally
     */
    fun saveToken(token: String) {
        prefs.edit()
            .putString(KEY_FCM_TOKEN, token)
            .putBoolean(KEY_TOKEN_SYNCED, false)
            .apply()

        Log.d(TAG, "Token saved locally: $token")

        // Sync with Firebase if user is logged in
        val userId = auth.currentUser?.uid
        if (userId != null) {
            syncTokenToFirebase(userId, token)
        }
    }

    /**
     * Get the locally saved token
     */
    fun getSavedToken(): String? {
        return prefs.getString(KEY_FCM_TOKEN, null)
    }

    /**
     * Sync token to Firebase for the current user
     */
    fun syncTokenToFirebase(userId: String, token: String? = null) {
        val fcmToken = token ?: getSavedToken() ?: return

        val tokenData = mapOf(
            "token" to fcmToken,
            "timestamp" to System.currentTimeMillis(),
            "platform" to "android"
        )

        firebaseDb.child("users")
            .child(userId)
            .child("fcmTokens")
            .child(fcmToken.hashCode().toString())
            .setValue(tokenData)
            .addOnSuccessListener {
                prefs.edit().putBoolean(KEY_TOKEN_SYNCED, true).apply()
                Log.d(TAG, "Token synced to Firebase for user: $userId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to sync token to Firebase", e)
            }
    }

    /**
     * Remove token from Firebase when user logs out
     */
    fun removeTokenFromFirebase(userId: String) {
        val token = getSavedToken() ?: return

        firebaseDb.child("users")
            .child(userId)
            .child("fcmTokens")
            .child(token.hashCode().toString())
            .removeValue()
            .addOnSuccessListener {
                Log.d(TAG, "Token removed from Firebase for user: $userId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to remove token from Firebase", e)
            }
    }

    /**
     * Clear local token data
     */
    fun clearLocalToken() {
        prefs.edit()
            .remove(KEY_FCM_TOKEN)
            .remove(KEY_TOKEN_SYNCED)
            .apply()
        Log.d(TAG, "Local token cleared")
    }

    /**
     * Check if token is synced with Firebase
     */
    fun isTokenSynced(): Boolean {
        return prefs.getBoolean(KEY_TOKEN_SYNCED, false)
    }

    /**
     * Subscribe to a topic (useful for broadcast notifications)
     */
    suspend fun subscribeToTopic(topic: String): Boolean {
        return try {
            FirebaseMessaging.getInstance().subscribeToTopic(topic).await()
            Log.d(TAG, "Subscribed to topic: $topic")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to subscribe to topic: $topic", e)
            false
        }
    }

    /**
     * Unsubscribe from a topic
     */
    suspend fun unsubscribeFromTopic(topic: String): Boolean {
        return try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).await()
            Log.d(TAG, "Unsubscribed from topic: $topic")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unsubscribe from topic: $topic", e)
            false
        }
    }
}
