package com.aipet.brain.brain.events

enum class EventType {
    APP_STARTED,
    TEST_EVENT,
    CAMERA_FRAME_RECEIVED,
    PERCEPTION_OBSERVATION_RECORDED,
    PERSON_SEEN_RECORDED,
    PERSON_UNKNOWN_DETECTED,
    OBJECT_DETECTED,
    OWNER_SEEN_DETECTED,
    ROBOT_GREETING_OWNER_TRIGGERED,
    USER_INTERACTED_PET,
    BRAIN_STATE_CHANGED,
    TRAITS_UPDATED,
    UNKNOWN;

    companion object {
        fun fromRawValue(rawValue: String): EventType {
            return entries.firstOrNull { it.name == rawValue } ?: UNKNOWN
        }
    }
}
