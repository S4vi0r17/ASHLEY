package com.grupo2.ashley.chat.database.dao

import androidx.room.*
import com.grupo2.ashley.chat.database.entities.MessageEntity
import com.grupo2.ashley.chat.models.MessageStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    // Obtiene mensajes de una conversación en tiempo real (Flow)
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND isDeleted = 0 ORDER BY timestamp ASC")
    fun getMessagesByConversation(conversationId: String): Flow<List<MessageEntity>>

    // Obtiene mensajes con paginación
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND isDeleted = 0 ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getMessagesPaginated(conversationId: String, limit: Int, offset: Int): List<MessageEntity>

    // Obtiene los mensajes más recientes
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND isDeleted = 0 ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLatestMessages(conversationId: String, limit: Int): List<MessageEntity>

    // Obtiene mensajes que aún no se han sincronizado con Firebase
    @Query("SELECT * FROM messages WHERE localOnly = 1 ORDER BY timestamp ASC")
    suspend fun getUnsyncedMessages(): List<MessageEntity>

    // Inserta o reemplaza un mensaje
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    // Inserta o reemplaza múltiples mensajes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    // Actualiza un mensaje existente
    @Update
    suspend fun updateMessage(message: MessageEntity)

    // Actualiza el estado de un mensaje
    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)

    // Marca un mensaje como sincronizado con Firebase
    @Query("UPDATE messages SET isSynced = 1, localOnly = 0 WHERE id = :messageId")
    suspend fun markAsSynced(messageId: String)

    // Marca un mensaje como eliminado (soft delete)
    @Query("UPDATE messages SET isDeleted = 1 WHERE id = :messageId")
    suspend fun markAsDeleted(messageId: String)

    // Marca todos los mensajes de otros usuarios en una conversación como leídos
    @Query("UPDATE messages SET status = :status, readAt = :readAt WHERE conversationId = :conversationId AND senderId != :currentUserId AND status != :status AND isDeleted = 0")
    suspend fun markConversationMessagesAsRead(conversationId: String, currentUserId: String, status: MessageStatus, readAt: Long)

    // Obtiene IDs de mensajes no leídos de otros usuarios
    @Query("SELECT id FROM messages WHERE conversationId = :conversationId AND senderId != :currentUserId AND status != :readStatus AND isDeleted = 0")
    suspend fun getUnreadMessageIds(conversationId: String, currentUserId: String, readStatus: MessageStatus): List<String>

    // Elimina permanentemente un mensaje
    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    // Elimina todos los mensajes de una conversación
    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesByConversation(conversationId: String)

    // Cuenta los mensajes de una conversación
    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId AND isDeleted = 0")
    suspend fun getMessageCount(conversationId: String): Int

    // Obtiene todos los IDs de mensajes de una conversación
    @Query("SELECT id FROM messages WHERE conversationId = :conversationId AND isDeleted = 0")
    suspend fun getMessageIds(conversationId: String): List<String>

    // Cuenta los mensajes no leídos de otros usuarios en una conversación
    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId AND senderId != :currentUserId AND status != 'READ' AND isDeleted = 0")
    suspend fun getUnreadMessageCount(conversationId: String, currentUserId: String): Int
}
