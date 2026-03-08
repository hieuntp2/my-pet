package com.aipet.brain.app.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CameraSelectionStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun selectedCamera_defaultsToFront() = runTest {
        val store = createStore(
            scope = backgroundScope,
            fileName = "default_camera.preferences_pb"
        )

        val selectedCamera = store.selectedCamera.first()

        assertEquals(CameraSelection.FRONT, selectedCamera)
    }

    @Test
    fun setSelectedCamera_savesAndLoadsBackCamera() = runTest {
        val store = createStore(
            scope = backgroundScope,
            fileName = "saved_camera.preferences_pb"
        )

        store.setSelectedCamera(CameraSelection.BACK)
        val selectedCamera = store.selectedCamera.first()

        assertEquals(CameraSelection.BACK, selectedCamera)
    }

    private fun createStore(
        scope: CoroutineScope,
        fileName: String
    ): CameraSelectionStore {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(temporaryFolder.root, fileName) }
        )
        return CameraSelectionStore(dataStore)
    }
}
