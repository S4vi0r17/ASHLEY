package com.grupo2.ashley.chat.data

import com.grupo2.ashley.chat.models.Conversation
import com.grupo2.ashley.chat.models.Message
import com.grupo2.ashley.chat.models.MessageStatus
import kotlinx.coroutines.flow.Flow

interface ChatRepository {

    // Messages

    // Observa mensajes de una conversación en tiempo real
    fun observeMessages(conversationId: String): Flow<List<Message>>

    // Envía un mensaje con texto, imagen o video
    suspend fun sendMessage(conversationId: String, senderId: String, text: String, imageBytes: ByteArray? = null, videoBytes: ByteArray? = null): Result<Message>

    // Carga más mensajes con paginación
    suspend fun loadMoreMessages(conversationId: String, offset: Int, limit: Int): List<Message>

    // Actualiza el estado de un mensaje
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)

    // Elimina un mensaje
    suspend fun deleteMessage(messageId: String, conversationId: String)

    // Reintenta enviar un mensaje fallido
    suspend fun retryFailedMessage(messageId: String)

    // Marca mensajes como leídos
    suspend fun markMessagesAsRead(conversationId: String, currentUserId: String)

    // Conversations

    // Observa todas las conversaciones de un usuario
    fun observeConversations(userId: String): Flow<List<Conversation>>

    // Crea o recupera una conversación entre dos usuarios
    suspend fun createOrGetConversation(userId1: String, userId2: String): Result<String>

    // Marca una conversación como leída
    suspend fun markConversationAsRead(conversationId: String)

    // Actualiza el estado de escritura del usuario
    suspend fun updateTypingStatus(conversationId: String, userId: String, isTyping: Boolean)

    // Obtiene información del producto de la conversación
    suspend fun getProductInfoForConversation(conversationId: String): com.grupo2.ashley.chat.models.ProductInfo?

    // Observa el estado de escritura de otro usuario
    fun observeTypingStatus(conversationId: String, otherUserId: String): Flow<Boolean>

    // Obtiene el total de mensajes sin leer
    suspend fun getTotalUnreadCount(currentUserId: String): Int

    // Sync

    // Sincroniza mensajes pendientes cuando hay conexión
    suspend fun syncOfflineMessages()

    // Sincroniza todas las conversaciones desde Firebase
    suspend fun syncConversations(userId: String)
}
