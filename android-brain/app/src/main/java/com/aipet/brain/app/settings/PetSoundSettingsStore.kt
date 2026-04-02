package com.aipet.brain.app.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PetSoundSettingsStore(
    private val dataStore: DataStore<Preferences>
) {
    val soundEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[soundEnabledKey] ?: DEFAULT_SOUND_ENABLED
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[soundEnabledKey] = enabled
        }
    }

    companion object {
        private const val SETTINGS_FILE_NAME = "pet_brain_audio_behavior_settings"
        private const val DEFAULT_SOUND_ENABLED = true
        private val soundEnabledKey = booleanPreferencesKey("pet_sound_enabled")

        fun create(context: Context): PetSoundSettingsStore {
            val appContext = context.applicationContext
            val dataStore = PreferenceDataStoreFactory.create(
                produceFile = { appContext.preferencesDataStoreFile(SETTINGS_FILE_NAME) }
            )
            return PetSoundSettingsStore(dataStore)
        }
    }
}
