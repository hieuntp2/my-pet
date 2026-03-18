package com.aipet.brain.brain.interaction

enum class PetInteractionType {
    TAP,
    LONG_PRESS;

    companion object {
        fun fromRawValue(rawValue: String): PetInteractionType {
            return entries.firstOrNull { it.name == rawValue } ?: TAP
        }
    }
}
