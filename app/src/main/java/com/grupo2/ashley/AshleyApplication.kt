package com.grupo2.ashley

import android.app.Application
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.grupo2.ashley.chat.notifications.FCMTokenManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AshleyApplication : Application() {

    @Inject
    lateinit var fcmTokenManager: FCMTokenManager

    @Inject
    lateinit var auth: FirebaseAuth

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        // Initialize FCM token
        applicationScope.launch {
            try {
                val token = fcmTokenManager.getToken()
                if (token != null) {
                    Log.d("AshleyApp", "FCM Token initialized: $token")

                    // Sync token if user is logged in
                    auth.currentUser?.uid?.let { userId ->
                        fcmTokenManager.syncTokenToFirebase(userId, token)
                    }
                }
            } catch (e: Exception) {
                Log.e("AshleyApp", "Failed to initialize FCM token", e)
            }
        }

        // Listen for auth state changes to sync FCM token
        auth.addAuthStateListener { firebaseAuth ->
            val userId = firebaseAuth.currentUser?.uid
            if (userId != null) {
                // User logged in - sync token
                applicationScope.launch {
                    val token = fcmTokenManager.getToken()
                    if (token != null) {
                        fcmTokenManager.syncTokenToFirebase(userId, token)
                    }
                }
            } else {
                // User logged out - optionally clear token
                // Uncomment if you want to clear token on logout
                // fcmTokenManager.clearLocalToken()
            }
        }
    }
}
