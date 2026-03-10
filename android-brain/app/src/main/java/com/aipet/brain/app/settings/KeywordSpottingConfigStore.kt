package com.aipet.brain.app.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.aipet.brain.core.common.config.KeywordSpottingConfig
import com.aipet.brain.core.common.config.KeywordSpottingProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class KeywordSpottingConfigStore(
    private val dataStore: DataStore<Preferences>
) {
    val config: Flow<KeywordSpottingConfig> = dataStore.data.map { preferences ->
        val enabled = preferences[keywordSpottingEnabledKey] ?: KeywordSpottingConfig.DEFAULT.enabled
        val provider = KeywordSpottingProvider.fromPersistedValue(
            preferences[keywordSpottingProviderKey]
        )
        KeywordSpottingConfig(
            enabled = enabled,
            provider = provider
        )
    }

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[keywordSpottingEnabledKey] = enabled
        }
    }

    suspend fun setProvider(provider: KeywordSpottingProvider) {
        dataStore.edit { preferences ->
            preferences[keywordSpottingProviderKey] = provider.persistedValue
        }
    }

    suspend fun setConfig(config: KeywordSpottingConfig) {
        dataStore.edit { preferences ->
            preferences[keywordSpottingEnabledKey] = config.enabled
            preferences[keywordSpottingProviderKey] = config.provider.persistedValue
        }
    }

    companion object {
        private const val SETTINGS_FILE_NAME = "pet_brain_audio_settings"
        private val keywordSpottingEnabledKey = booleanPreferencesKey("keyword_spotting_enabled")
        private val keywordSpottingProviderKey = stringPreferencesKey("keyword_spotting_provider")

        fun create(context: Context): KeywordSpottingConfigStore {
            val appContext = context.applicationContext
            val dataStore = PreferenceDataStoreFactory.create(
                produceFile = { appContext.preferencesDataStoreFile(SETTINGS_FILE_NAME) }
            )
            return KeywordSpottingConfigStore(dataStore)
        }
    }
}
