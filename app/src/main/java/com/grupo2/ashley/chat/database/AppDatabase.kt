package com.grupo2.ashley.chat.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.grupo2.ashley.chat.database.converters.Converters
import com.grupo2.ashley.chat.database.dao.ConversationDao
import com.grupo2.ashley.chat.database.dao.MessageDao
import com.grupo2.ashley.chat.database.entities.ConversationEntity
import com.grupo2.ashley.chat.database.entities.MessageEntity

@Database(
    entities = [MessageEntity::class, ConversationEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao
}
