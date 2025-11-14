package com.grupo2.ashley.chat.database.dao

import androidx.room.*
import com.grupo2.ashley.chat.database.entities.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    // Obtiene todas las conversaciones en tiempo real (Flow)
    @Query("SELECT * FROM conversations ORDER BY lastMessageTimestamp DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    // Obtiene las conversaciones de un usuario en tiempo real (Flow)
    @Query("SELECT * FROM conversations WHERE participantsJson LIKE '%' || :userId || '%' ORDER BY lastMessageTimestamp DESC")
    fun getUserConversations(userId: String): Flow<List<ConversationEntity>>

    // Obtiene las conversaciones de un usuario de forma síncrona
    @Query("SELECT * FROM conversations WHERE participantsJson LIKE '%' || :userId || '%' ORDER BY lastMessageTimestamp DESC")
    suspend fun getUserConversationsSync(userId: String): List<ConversationEntity>

    // Obtiene una conversación por su ID
    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    suspend fun getConversationById(conversationId: String): ConversationEntity?

    // Inserta o reemplaza una conversación
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    // Inserta o reemplaza múltiples conversaciones
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<ConversationEntity>)

    // Actualiza una conversación existente
    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    // Actualiza el contador de mensajes no leídos
    @Query("UPDATE conversations SET unreadCount = :count WHERE id = :conversationId")
    suspend fun updateUnreadCount(conversationId: String, count: Int)

    // Marca una conversación como leída (contador a 0)
    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :conversationId")
    suspend fun markAsRead(conversationId: String)

    // Actualiza el último mensaje de la conversación
    @Query("UPDATE conversations SET lastMessageText = :text, lastMessageTimestamp = :timestamp, lastMessageSenderId = :senderId WHERE id = :conversationId")
    suspend fun updateLastMessage(conversationId: String, text: String?, timestamp: Long?, senderId: String?)

    // Elimina una conversación (aún no se ha inplementado)
    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: String)

    // Elimina todas las conversaciones (aún no se ha inplementado)
    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()
}
