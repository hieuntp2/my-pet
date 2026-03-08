package com.aipet.brain.app.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CameraSelectionStore(
    private val dataStore: DataStore<Preferences>
) {
    val selectedCamera: Flow<CameraSelection> = dataStore.data.map { preferences ->
        CameraSelection.fromPersistedValue(preferences[selectedCameraKey])
    }

    suspend fun setSelectedCamera(selection: CameraSelection) {
        dataStore.edit { preferences ->
            preferences[selectedCameraKey] = selection.persistedValue
        }
    }

    companion object {
        private const val SETTINGS_FILE_NAME = "pet_brain_settings"
        private val selectedCameraKey = stringPreferencesKey("selected_camera")

        fun create(context: Context): CameraSelectionStore {
            val appContext = context.applicationContext
            val dataStore = PreferenceDataStoreFactory.create(
                produceFile = { appContext.preferencesDataStoreFile(SETTINGS_FILE_NAME) }
            )
            return CameraSelectionStore(dataStore)
        }
    }
}
