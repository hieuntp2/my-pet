package com.aipet.brain.app.onboarding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.first

class PetOnboardingStore(
    private val dataStore: DataStore<Preferences>
) {
    suspend fun isNamingCompletedForProfile(profileId: String): Boolean {
        val normalizedProfileId = profileId.trim()
        if (normalizedProfileId.isBlank()) {
            return false
        }
        val preferences = dataStore.data.first()
        return preferences[completedNamingProfileIdKey] == normalizedProfileId
    }

    suspend fun markNamingCompleted(profileId: String) {
        val normalizedProfileId = profileId.trim()
        require(normalizedProfileId.isNotBlank()) { "profileId cannot be blank." }
        dataStore.edit { preferences ->
            preferences[completedNamingProfileIdKey] = normalizedProfileId
        }
    }

    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(completedNamingProfileIdKey)
        }
    }

    companion object {
        private const val SETTINGS_FILE_NAME = "pet_brain_onboarding"
        private val completedNamingProfileIdKey = stringPreferencesKey("completed_naming_profile_id")

        fun create(context: Context): PetOnboardingStore {
            val appContext = context.applicationContext
            val dataStore = PreferenceDataStoreFactory.create(
                produceFile = { appContext.preferencesDataStoreFile(SETTINGS_FILE_NAME) }
            )
            return PetOnboardingStore(dataStore)
        }
    }
}
