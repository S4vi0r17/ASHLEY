package com.grupo2.ashley.chat.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.grupo2.ashley.chat.models.Message
import com.grupo2.ashley.chat.models.MessageStatus

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val senderId: String,
    val text: String,
    val timestamp: Long,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val mediaType: String? = null,
    val status: MessageStatus = MessageStatus.SENT,
    val isSynced: Boolean = false, // True cuando se ha sincronizado con Firebase
    val localOnly: Boolean = false, // True cuando esperamos a que se envíe a Firebase
    val isDeleted: Boolean = false, // True cuando eliminamos un mensaje :v
    val readAt: Long? = null
) {
    fun toMessage(): Message {
        return Message(
            id = id,
            senderId = senderId,
            text = text,
            timestamp = timestamp,
            imageUrl = imageUrl,
            videoUrl = videoUrl,
            mediaType = mediaType,
            status = status,
            isDeleted = isDeleted,
            readAt = readAt
        )
    }

    companion object {
        fun fromMessage(message: Message, conversationId: String, isSynced: Boolean = true): MessageEntity {
            return MessageEntity(
                id = message.id,
                conversationId = conversationId,
                senderId = message.senderId ?: "",
                text = message.text,
                timestamp = message.timestamp,
                imageUrl = message.imageUrl,
                videoUrl = message.videoUrl,
                mediaType = message.mediaType,
                status = message.status,
                isSynced = isSynced,
                localOnly = false,
                isDeleted = message.isDeleted,
                readAt = message.readAt
            )
        }
    }
}
