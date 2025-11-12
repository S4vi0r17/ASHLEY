package com.grupo2.ashley.chat.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.grupo2.ashley.chat.database.converters.Converters
import com.grupo2.ashley.chat.database.dao.ConversationDao
import com.grupo2.ashley.chat.database.dao.MessageDao
import com.grupo2.ashley.chat.database.entities.ConversationEntity
import com.grupo2.ashley.chat.database.entities.MessageEntity

@Database(
    entities = [MessageEntity::class, ConversationEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao

    companion object {
        // Migration from version 2 to 3: Add readAt column
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE messages ADD COLUMN readAt INTEGER")
            }
        }
    }
}
