package com.aipet.brain.brain.events

enum class EventType {
    APP_STARTED,
    TEST_EVENT,
    UNKNOWN;

    companion object {
        fun fromRawValue(rawValue: String): EventType {
            return entries.firstOrNull { it.name == rawValue } ?: UNKNOWN
        }
    }
}
