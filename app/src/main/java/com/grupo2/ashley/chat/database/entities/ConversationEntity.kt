package com.grupo2.ashley.chat.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.grupo2.ashley.chat.models.Conversation
import com.grupo2.ashley.chat.models.LastMessage

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    val participantsJson: String,
    val lastMessageText: String?,
    val lastMessageTimestamp: Long?,
    val lastMessageSenderId: String?,
    val unreadCount: Int = 0,
    val isSynced: Boolean = false
) {
    fun toConversation(participants: List<String>): Conversation {
        return Conversation(
            id = id,
            participants = participants,
            lastMessage = if (lastMessageText != null && lastMessageTimestamp != null && lastMessageSenderId != null) {
                LastMessage(
                    text = lastMessageText,
                    timestamp = lastMessageTimestamp,
                    senderId = lastMessageSenderId,
                    unreadCount = unreadCount
                )
            } else null,
            participantsInfo = emptyMap()
        )
    }

    companion object {
        fun fromConversation(conversation: Conversation): ConversationEntity {
            return ConversationEntity(
                id = conversation.id,
                participantsJson = conversation.participants.joinToString(","),
                lastMessageText = conversation.lastMessage?.text,
                lastMessageTimestamp = conversation.lastMessage?.timestamp,
                lastMessageSenderId = conversation.lastMessage?.senderId,
                unreadCount = conversation.lastMessage?.unreadCount ?: 0,
                isSynced = true
            )
        }
    }
}
