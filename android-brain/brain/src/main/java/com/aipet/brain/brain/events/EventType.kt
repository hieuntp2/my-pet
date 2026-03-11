package com.aipet.brain.brain.events

enum class EventType {
    APP_STARTED,
    TEST_EVENT,
    CAMERA_FRAME_RECEIVED,
    FACE_DETECTED,
    FACES_DETECTED,
    PERCEPTION_OBSERVATION_RECORDED,
    PERSON_SEEN_RECORDED,
    PERSON_UNKNOWN_DETECTED,
    OBJECT_DETECTED,
    OWNER_SEEN_DETECTED,
    ROBOT_GREETING_OWNER_TRIGGERED,
    USER_INTERACTED_PET,
    BRAIN_STATE_CHANGED,
    TRAITS_UPDATED,
    // Phase 1.5 audio interaction events consumed by perception, behavior, and audio-response modules.
    AUDIO_CAPTURE_STARTED,
    AUDIO_CAPTURE_STOPPED,
    SOUND_ENERGY_CHANGED,
    SOUND_DETECTED,
    VOICE_ACTIVITY_STARTED,
    VOICE_ACTIVITY_ENDED,
    WAKE_WORD_DETECTED,
    KEYWORD_DETECTED,
    AUDIO_RESPONSE_REQUESTED,
    AUDIO_RESPONSE_STARTED,
    AUDIO_RESPONSE_COMPLETED,
    AUDIO_RESPONSE_SKIPPED,
    UNKNOWN;

    companion object {
        fun fromRawValue(rawValue: String): EventType {
            return entries.firstOrNull { it.name == rawValue } ?: UNKNOWN
        }
    }
}
