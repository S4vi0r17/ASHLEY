package com.grupo2.ashley.di

import android.content.Context
import androidx.room.Room
import com.grupo2.ashley.chat.database.AppDatabase
import com.grupo2.ashley.chat.database.dao.ConversationDao
import com.grupo2.ashley.chat.database.dao.MessageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ashley_chat_database"
        )
            .fallbackToDestructiveMigration() // Recreate database on schema changes (OK for MVP)
            .build()
    }

    @Provides
    fun provideMessageDao(database: AppDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    fun provideConversationDao(database: AppDatabase): ConversationDao {
        return database.conversationDao()
    }
}
