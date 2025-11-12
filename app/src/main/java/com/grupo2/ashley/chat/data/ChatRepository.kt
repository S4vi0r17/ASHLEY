package com.grupo2.ashley.chat.data

import com.grupo2.ashley.chat.models.Conversation
import com.grupo2.ashley.chat.models.Message
import com.grupo2.ashley.chat.models.MessageStatus
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    // Messages
    fun observeMessages(conversationId: String): Flow<List<Message>>
    suspend fun sendMessage(conversationId: String, senderId: String, text: String, imageBytes: ByteArray? = null): Result<Message>
    suspend fun loadMoreMessages(conversationId: String, offset: Int, limit: Int): List<Message>
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)
    suspend fun deleteMessage(messageId: String, conversationId: String)
    suspend fun retryFailedMessage(messageId: String)
    suspend fun markMessagesAsRead(conversationId: String, currentUserId: String)

    // Conversations
    fun observeConversations(userId: String): Flow<List<Conversation>>
    suspend fun createOrGetConversation(userId1: String, userId2: String): Result<String>
    suspend fun markConversationAsRead(conversationId: String)
    suspend fun updateTypingStatus(conversationId: String, userId: String, isTyping: Boolean)
    suspend fun getProductInfoForConversation(conversationId: String): com.grupo2.ashley.chat.models.ProductInfo?
    fun observeTypingStatus(conversationId: String, otherUserId: String): Flow<Boolean>
    suspend fun getTotalUnreadCount(currentUserId: String): Int

    // Sync
    suspend fun syncOfflineMessages()
    suspend fun syncConversations(userId: String)
}
