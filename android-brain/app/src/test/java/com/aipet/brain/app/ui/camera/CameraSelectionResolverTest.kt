package com.aipet.brain.app.ui.camera

import com.aipet.brain.app.settings.CameraSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CameraSelectionResolverTest {
    @Test
    fun resolve_usesSelectedBackCameraWhenAvailable() {
        val resolved = CameraSelectionResolver.resolve(
            selectedCamera = CameraSelection.BACK,
            availability = CameraAvailability(
                hasFrontCamera = true,
                hasBackCamera = true
            )
        )

        assertEquals(CameraSelection.BACK, resolved)
    }

    @Test
    fun resolve_fallsBackToBackWhenFrontIsUnavailable() {
        val resolved = CameraSelectionResolver.resolve(
            selectedCamera = CameraSelection.FRONT,
            availability = CameraAvailability(
                hasFrontCamera = false,
                hasBackCamera = true
            )
        )

        assertEquals(CameraSelection.BACK, resolved)
    }

    @Test
    fun resolve_returnsNullWhenNoCameraIsAvailable() {
        val resolved = CameraSelectionResolver.resolve(
            selectedCamera = CameraSelection.FRONT,
            availability = CameraAvailability(
                hasFrontCamera = false,
                hasBackCamera = false
            )
        )

        assertNull(resolved)
    }
}
