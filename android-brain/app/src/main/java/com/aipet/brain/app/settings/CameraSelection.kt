package com.aipet.brain.app.settings

enum class CameraSelection(
    val persistedValue: String,
    val displayName: String
) {
    FRONT(
        persistedValue = "front",
        displayName = "Front"
    ),
    BACK(
        persistedValue = "back",
        displayName = "Back"
    );

    companion object {
        fun fromPersistedValue(value: String?): CameraSelection {
            return entries.firstOrNull { selection ->
                selection.persistedValue == value
            } ?: FRONT
        }
    }
}
