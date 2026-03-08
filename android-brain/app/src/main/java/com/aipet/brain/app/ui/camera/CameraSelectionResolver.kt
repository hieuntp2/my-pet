package com.aipet.brain.app.ui.camera

import com.aipet.brain.app.settings.CameraSelection

data class CameraAvailability(
    val hasFrontCamera: Boolean,
    val hasBackCamera: Boolean
)

object CameraSelectionResolver {
    fun resolve(
        selectedCamera: CameraSelection,
        availability: CameraAvailability
    ): CameraSelection? {
        return when (selectedCamera) {
            CameraSelection.FRONT -> {
                when {
                    availability.hasFrontCamera -> CameraSelection.FRONT
                    availability.hasBackCamera -> CameraSelection.BACK
                    else -> null
                }
            }

            CameraSelection.BACK -> {
                when {
                    availability.hasBackCamera -> CameraSelection.BACK
                    availability.hasFrontCamera -> CameraSelection.FRONT
                    else -> null
                }
            }
        }
    }
}
