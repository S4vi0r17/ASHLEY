package com.grupo2.ashley.chat.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.grupo2.ashley.chat.data.ChatUserRepository
import com.grupo2.ashley.chat.database.dao.ConversationDao
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

    @Inject
    lateinit var conversationDao: ConversationDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "AshleyFCMService"
    }

    // Se llama cuando Firebase genera un nuevo token FCM para el dispositivo
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")

        serviceScope.launch {
            fcmTokenManager.saveToken(token)
        }
    }

    // Recibe mensajes push de Firebase Cloud Messaging
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        // Manejo de la data de push noti
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Las push noti se moestrarán automaticamente por el sistema cuando la app esté en segundo plano y en primero
        remoteMessage.notification?.let {
            Log.d(TAG, "Message notification body: ${it.body}")
        }
    }

    // Procesa mensajes de datos (data payload) recibidos desde FCM
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

    // Maneja notificaciones de nuevos mensajes y las muestra al usuario
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

                // Check if the conversation is muted
                val isMuted = conversationDao.isConversationMuted(conversationId) ?: false
                if (isMuted) {
                    Log.d(TAG, "Conversation $conversationId is MUTED, skipping notification")
                    return@launch
                }
                Log.d(TAG, "Conversation $conversationId is NOT muted, showing notification")

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

    // Maneja indicadores de escritura recibidos por FCM
    private fun handleTypingIndicator(data: Map<String, String>) {
        // Handle typing indicator if needed
        // This could be used to show a notification or update UI
        Log.d(TAG, "Typing indicator received: $data")
    }

    // Se llama cuando algunos mensajes fueron eliminados del servidor
    override fun onDeletedMessages() {
        super.onDeletedMessages()
        Log.d(TAG, "Some messages were deleted from server")
    }

    // Se llama cuando un mensaje fue enviado exitosamente
    override fun onMessageSent(msgId: String) {
        super.onMessageSent(msgId)
        Log.d(TAG, "Message sent: $msgId")
    }

    // Se llama cuando ocurre un error al enviar un mensaje
    override fun onSendError(msgId: String, exception: Exception) {
        super.onSendError(msgId, exception)
        Log.e(TAG, "Message send error: $msgId", exception)
    }
}
