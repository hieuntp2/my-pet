package com.aipet.brain.perception.audio.model

/**
 * Lifecycle state contract for keyword spotter adapters.
 *
 * Stage A audio MVP remains unchanged; this state model prepares a clean offline extension.
 */
enum class KeywordSpotterState {
    IDLE,
    STARTING,
    RUNNING,
    STOPPING,
    FAILED
}
