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
    val status: MessageStatus = MessageStatus.SENT,
    val isSynced: Boolean = false, // True when synced to Firebase
    val localOnly: Boolean = false, // True for messages waiting to be sent
    val isDeleted: Boolean = false // True when message has been deleted
) {
    fun toMessage(): Message {
        return Message(
            id = id,
            senderId = senderId,
            text = text,
            timestamp = timestamp,
            imageUrl = imageUrl,
            status = status,
            isDeleted = isDeleted
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
                status = message.status,
                isSynced = isSynced,
                localOnly = false,
                isDeleted = message.isDeleted
            )
        }
    }
}
