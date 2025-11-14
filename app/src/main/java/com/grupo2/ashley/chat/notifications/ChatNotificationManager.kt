package com.grupo2.ashley.chat.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.*
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.grupo2.ashley.MainActivity
import com.grupo2.ashley.R
import com.grupo2.ashley.chat.models.Message
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val CHANNEL_ID = "chat_messages"
        private const val CHANNEL_NAME = "Chat Messages"
        private const val CHANNEL_DESCRIPTION = "Notifications for new chat messages"
        private const val NOTIFICATION_ID_BASE = 1000
    }

    init {
        createNotificationChannel()
    }

    // Crea el canal de notificaciones para mensajes de chat (Android O+)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Muestra una notificación para un mensaje nuevo
    fun showMessageNotification(
        conversationId: String,
        message: Message,
        senderName: String,
        senderPhotoUrl: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("conversationId", conversationId)
            putExtra("openChat", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            conversationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create sender person for messaging style
        val sender = Person.Builder()
            .setName(senderName)
            .apply {
                if (!senderPhotoUrl.isNullOrEmpty()) {
                    // TODO: Load icon from URL if needed
                }
            }
            .build()

        val messagingStyle = NotificationCompat.MessagingStyle(
            Person.Builder().setName("Me").build()
        )

        messagingStyle.addMessage(
            message.text.ifEmpty { if (message.imageUrl != null) "Photo" else "Message" },
            message.timestamp,
            sender
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your chat icon
            .setStyle(messagingStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        notificationManager.notify(
            NOTIFICATION_ID_BASE + conversationId.hashCode(),
            notification
        )
    }

    // Muestra una notificación agrupada para múltiples mensajes de la misma conversación
    fun showGroupedMessageNotification(
        conversationId: String,
        messages: List<Pair<Message, String>>,
        conversationName: String
    ) {
        if (messages.isEmpty()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("conversationId", conversationId)
            putExtra("openChat", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            conversationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val messagingStyle = NotificationCompat.MessagingStyle(
            Person.Builder().setName("Me").build()
        )

        messages.forEach { (message, senderName) ->
            val sender = Person.Builder()
                .setName(senderName)
                .build()

            messagingStyle.addMessage(
                message.text.ifEmpty { if (message.imageUrl != null) "Photo" else "Message" },
                message.timestamp,
                sender
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setStyle(messagingStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setNumber(messages.size)
            .build()

        notificationManager.notify(
            NOTIFICATION_ID_BASE + conversationId.hashCode(),
            notification
        )
    }

    // Cancela la notificación de una conversación específica
    fun cancelNotification(conversationId: String) {
        notificationManager.cancel(NOTIFICATION_ID_BASE + conversationId.hashCode())
    }

    // Cancela todas las notificaciones de chat
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}
