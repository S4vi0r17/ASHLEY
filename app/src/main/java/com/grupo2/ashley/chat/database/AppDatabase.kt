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
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao

    companion object {
        // Migrar versión de 2 a 3
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE messages ADD COLUMN readAt INTEGER")
            }
        }

        // Migrar de 3 a 4
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE messages ADD COLUMN videoUrl TEXT")
                database.execSQL("ALTER TABLE messages ADD COLUMN mediaType TEXT")
            }
        }

        // Migrar de 4 a 5 - Agregar campo isMuted a conversations
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE conversations ADD COLUMN isMuted INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Migrar de 5 a 6 - Agregar campo isArchived a conversations
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE conversations ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Migrar de 6 a 7 - Agregar campo isBlocked a conversations
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE conversations ADD COLUMN isBlocked INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
