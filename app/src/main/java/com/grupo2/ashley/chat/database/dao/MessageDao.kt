package com.grupo2.ashley.chat.database.dao

import androidx.room.*
import com.grupo2.ashley.chat.database.entities.MessageEntity
import com.grupo2.ashley.chat.models.MessageStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND isDeleted = 0 ORDER BY timestamp ASC")
    fun getMessagesByConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND isDeleted = 0 ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getMessagesPaginated(conversationId: String, limit: Int, offset: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId AND isDeleted = 0 ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLatestMessages(conversationId: String, limit: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE localOnly = 1 ORDER BY timestamp ASC")
    suspend fun getUnsyncedMessages(): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)

    @Query("UPDATE messages SET isSynced = 1, localOnly = 0 WHERE id = :messageId")
    suspend fun markAsSynced(messageId: String)

    @Query("UPDATE messages SET isDeleted = 1 WHERE id = :messageId")
    suspend fun markAsDeleted(messageId: String)

    @Query("UPDATE messages SET status = :status WHERE conversationId = :conversationId AND senderId != :currentUserId AND status != :status AND isDeleted = 0")
    suspend fun markConversationMessagesAsRead(conversationId: String, currentUserId: String, status: MessageStatus)

    @Query("SELECT id FROM messages WHERE conversationId = :conversationId AND senderId != :currentUserId AND status != :readStatus AND isDeleted = 0")
    suspend fun getUnreadMessageIds(conversationId: String, currentUserId: String, readStatus: MessageStatus): List<String>

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesByConversation(conversationId: String)

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId AND isDeleted = 0")
    suspend fun getMessageCount(conversationId: String): Int

    @Query("SELECT id FROM messages WHERE conversationId = :conversationId AND isDeleted = 0")
    suspend fun getMessageIds(conversationId: String): List<String>
}
