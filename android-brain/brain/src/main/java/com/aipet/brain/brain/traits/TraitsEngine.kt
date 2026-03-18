package com.aipet.brain.brain.traits

import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.TraitsUpdatedEventPayload
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect

class TraitsEngine(
    private val repository: TraitsSnapshotRepository,
    private val eventBus: EventBus,
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
    private val idProvider: () -> String = { UUID.randomUUID().toString() }
) {
    private val currentTraits = MutableStateFlow<TraitsSnapshot?>(null)

    fun observeTraits(): StateFlow<TraitsSnapshot?> {
        return currentTraits.asStateFlow()
    }

    suspend fun initializeIfNeeded() {
        val existing = repository.latest()
        if (existing != null) {
            currentTraits.value = existing
            return
        }

        val defaults = TraitsSnapshot(
            snapshotId = idProvider(),
            capturedAtMs = nowProvider(),
            curiosity = DEFAULT_CURIOSITY,
            sociability = DEFAULT_SOCIABILITY,
            energy = DEFAULT_ENERGY,
            patience = DEFAULT_PATIENCE,
            boldness = DEFAULT_BOLDNESS
        )
        repository.save(defaults)
        currentTraits.value = defaults
    }

    suspend fun observeEventsAndApplyRules() {
        eventBus.observe().collect { event ->
            when (event.type) {
                EventType.USER_INTERACTED_PET,
                EventType.PET_LONG_PRESSED -> applyPetRule(event)
                EventType.BRAIN_STATE_CHANGED -> applyInactivityRule(event)
                else -> Unit
            }
        }
    }

    private suspend fun applyPetRule(event: EventEnvelope) {
        val current = repository.latest() ?: currentTraits.value ?: return
        val updated = current.copy(
            snapshotId = idProvider(),
            capturedAtMs = event.timestampMs,
            sociability = (current.sociability + PET_SOCIABILITY_DELTA).clampTrait()
        )
        persistIfChanged(updated)
    }

    private suspend fun applyInactivityRule(event: EventEnvelope) {
        val payload = event.payloadJson
        if (!payload.contains("\"toState\":\"SLEEPY\"")) {
            return
        }
        if (!payload.contains("\"reason\":\"INACTIVITY_TIMEOUT\"")) {
            return
        }
        val current = repository.latest() ?: currentTraits.value ?: return
        val updated = current.copy(
            snapshotId = idProvider(),
            capturedAtMs = event.timestampMs,
            sociability = (current.sociability - INACTIVITY_SOCIABILITY_DELTA).clampTrait()
        )
        persistIfChanged(updated)
    }

    private suspend fun persistIfChanged(
        updated: TraitsSnapshot
    ) {
        val previous = repository.latest() ?: currentTraits.value ?: return
        val changedFields = previous.changedTraitFieldsComparedTo(updated)
        if (changedFields.isEmpty()) {
            return
        }
        repository.save(updated)
        currentTraits.value = updated
        eventBus.publish(
            EventEnvelope.create(
                type = EventType.TRAITS_UPDATED,
                timestampMs = updated.capturedAtMs,
                payloadJson = TraitsUpdatedEventPayload(
                    changedAtMs = updated.capturedAtMs,
                    changedFields = changedFields,
                    previous = previous,
                    current = updated
                ).toJson()
            )
        )
    }

    private fun TraitsSnapshot.changedTraitFieldsComparedTo(other: TraitsSnapshot): List<String> {
        val changedFields = mutableListOf<String>()
        if (curiosity != other.curiosity) {
            changedFields.add("curiosity")
        }
        if (sociability != other.sociability) {
            changedFields.add("sociability")
        }
        if (energy != other.energy) {
            changedFields.add("energy")
        }
        if (patience != other.patience) {
            changedFields.add("patience")
        }
        if (boldness != other.boldness) {
            changedFields.add("boldness")
        }
        return changedFields
    }

    private fun Float.clampTrait(): Float {
        return coerceIn(0f, 1f)
    }

    companion object {
        private const val DEFAULT_CURIOSITY = 0.55f
        private const val DEFAULT_SOCIABILITY = 0.50f
        private const val DEFAULT_ENERGY = 0.65f
        private const val DEFAULT_PATIENCE = 0.50f
        private const val DEFAULT_BOLDNESS = 0.45f

        private const val PET_SOCIABILITY_DELTA = 0.02f
        private const val INACTIVITY_SOCIABILITY_DELTA = 0.02f
    }
}
