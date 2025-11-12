package com.grupo2.ashley.chat.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.grupo2.ashley.chat.data.ChatUserRepository
import com.grupo2.ashley.chat.models.Message
import com.grupo2.ashley.chat.models.MessageStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AshleyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var chatNotificationManager: ChatNotificationManager

    @Inject
    lateinit var fcmTokenManager: FCMTokenManager

    @Inject
    lateinit var userRepository: ChatUserRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "AshleyFCMService"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")

        // Save the new token
        serviceScope.launch {
            fcmTokenManager.saveToken(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        // Handle data payload
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Handle notification payload (if sent from console)
        remoteMessage.notification?.let {
            Log.d(TAG, "Message notification body: ${it.body}")
            // Note: Notification messages are automatically displayed by the system
            // when the app is in the background. We only need to handle them manually
            // when the app is in the foreground.
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {
        try {
            val type = data["type"] ?: return

            when (type) {
                "new_message" -> handleNewMessage(data)
                "typing" -> handleTypingIndicator(data)
                else -> Log.w(TAG, "Unknown message type: $type")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling data message", e)
        }
    }

    private fun handleNewMessage(data: Map<String, String>) {
        serviceScope.launch {
            try {
                val conversationId = data["conversationId"] ?: return@launch
                val messageId = data["messageId"] ?: return@launch
                val senderId = data["senderId"] ?: return@launch
                val text = data["text"] ?: ""
                val imageUrl = data["imageUrl"]
                val timestamp = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()

                // Don't show notification if the message is from the current user
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                if (senderId == currentUserId) {
                    Log.d(TAG, "Ignoring notification for own message")
                    return@launch
                }

                // Get sender information
                val userProfiles = userRepository.getUserProfiles(listOf(senderId))
                val senderProfile = userProfiles[senderId]
                val senderName = if (senderProfile != null) {
                    "${senderProfile.firstName} ${senderProfile.lastName}".trim()
                } else {
                    "Unknown User"
                }
                val senderPhotoUrl = senderProfile?.profileImageUrl

                // Create message object
                val message = Message(
                    id = messageId,
                    senderId = senderId,
                    text = text,
                    timestamp = timestamp,
                    imageUrl = imageUrl,
                    status = MessageStatus.DELIVERED
                )

                // Show notification
                chatNotificationManager.showMessageNotification(
                    conversationId = conversationId,
                    message = message,
                    senderName = senderName,
                    senderPhotoUrl = senderPhotoUrl
                )

                Log.d(TAG, "Notification shown for message from $senderName")
            } catch (e: Exception) {
                Log.e(TAG, "Error handling new message notification", e)
            }
        }
    }

    private fun handleTypingIndicator(data: Map<String, String>) {
        // Handle typing indicator if needed
        // This could be used to show a notification or update UI
        Log.d(TAG, "Typing indicator received: $data")
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
        Log.d(TAG, "Some messages were deleted from server")
    }

    override fun onMessageSent(msgId: String) {
        super.onMessageSent(msgId)
        Log.d(TAG, "Message sent: $msgId")
    }

    override fun onSendError(msgId: String, exception: Exception) {
        super.onSendError(msgId, exception)
        Log.e(TAG, "Message send error: $msgId", exception)
    }
}
