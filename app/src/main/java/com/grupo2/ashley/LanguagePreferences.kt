package com.grupo2.ashley

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object LanguagePreferences {
    private val Context.dataStore by preferencesDataStore(name = "settings")

    private val LANGUAGE_KEY = stringPreferencesKey("app_language")

    suspend fun saveLanguage(context: Context, language: String) {
        context.dataStore.edit { prefs ->
            prefs[LANGUAGE_KEY] = language
        }
    }

    fun languageFlow(context: Context): Flow<String?> =
        context.dataStore.data.map { prefs -> prefs[LANGUAGE_KEY] }
}
