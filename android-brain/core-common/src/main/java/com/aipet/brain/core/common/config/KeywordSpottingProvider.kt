package com.aipet.brain.core.common.config

enum class KeywordSpottingProvider(
    val persistedValue: String,
    val displayName: String
) {
    NONE(
        persistedValue = "none",
        displayName = "None"
    ),
    PORCUPINE(
        persistedValue = "porcupine",
        displayName = "Porcupine"
    ),
    TFLITE(
        persistedValue = "tflite",
        displayName = "TensorFlow Lite"
    ),
    CUSTOM(
        persistedValue = "custom",
        displayName = "Custom"
    );

    companion object {
        fun fromPersistedValue(value: String?): KeywordSpottingProvider {
            return entries.firstOrNull { provider ->
                provider.persistedValue == value
            } ?: NONE
        }
    }
}
