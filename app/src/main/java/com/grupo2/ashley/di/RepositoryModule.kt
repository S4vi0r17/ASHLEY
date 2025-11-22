package com.grupo2.ashley.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.grupo2.ashley.chat.data.ChatRepository
import com.grupo2.ashley.chat.data.ChatRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

// Módulo de Dagger/Hilt que provee instancias de Firebase y el CoroutineScope de aplicación
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    // Provee una instancia singleton de FirebaseAuth
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    // Provee la referencia raíz de Firebase Realtime Database
    @Provides
    @Singleton
    fun provideFirebaseDatabase(): DatabaseReference {
        return FirebaseDatabase.getInstance().reference
    }

    // Provee una instancia singleton de Firebase Firestore
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    // Provee un CoroutineScope global para tareas de larga duración en la aplicación
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindingsModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository
}
