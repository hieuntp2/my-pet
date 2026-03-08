package com.aipet.brain.brain.events

enum class EventType {
    APP_STARTED,
    TEST_EVENT,
    CAMERA_FRAME_RECEIVED,
    PERCEPTION_OBSERVATION_RECORDED,
    PERSON_SEEN_RECORDED,
    OWNER_SEEN_DETECTED,
    ROBOT_GREETING_OWNER_TRIGGERED,
    UNKNOWN;

    companion object {
        fun fromRawValue(rawValue: String): EventType {
            return entries.firstOrNull { it.name == rawValue } ?: UNKNOWN
        }
    }
}
