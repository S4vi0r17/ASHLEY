package com.grupo2.ashley.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.grupo2.ashley.auth.SessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de Hilt para proveer dependencias de autenticación y sesión
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    /**
     * Provee una instancia singleton de SessionManager
     */
    @Provides
    @Singleton
    fun provideSessionManager(
        @ApplicationContext context: Context,
        auth: FirebaseAuth
    ): SessionManager {
        return SessionManager(context, auth)
    }
}
