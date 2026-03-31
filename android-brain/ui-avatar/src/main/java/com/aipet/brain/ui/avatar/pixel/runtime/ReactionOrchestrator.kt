package com.aipet.brain.ui.avatar.pixel.runtime

import com.aipet.brain.ui.avatar.pixel.animation.FaceReactionType

/**
 * Executes reaction sequences phase by phase (attention → anticipation → main → settle).
 * Higher-priority reactions overwrite lower-priority ones.
 * onCompleted is invoked when the last phase finishes.
 */
internal class ReactionOrchestrator {

    var onCompleted: (() -> Unit)? = null
    val isActive: Boolean get() = activeEntry != null

    data class ReactionDirective(
        val targets: LayerTargets,
        val profile: TransitionProfile
    )

    private data class ActiveEntry(
        val definition: ReactionDefinition,
        val priority: Int,
        var phaseIndex: Int = 0,
        var phaseTimer: Long = 0L
    )

    private var activeEntry: ActiveEntry? = null

    /**
     * Attempt to start a reaction. Ignored if a higher-priority reaction is active.
     * Equal-priority reactions restart the sequence with the new type.
     */
    fun tryTrigger(type: FaceReactionType, priority: Int) {
        val existing = activeEntry
        if (existing != null && existing.priority > priority) return
        val definition = ReactionLibrary.all[type] ?: return
        activeEntry = ActiveEntry(definition = definition, priority = priority)
    }

    /**
     * Advance the active reaction by [deltaMillis] and return the current phase directive.
     * Returns null when the reaction sequence has just completed (cleaned up internally).
     */
    fun advance(deltaMillis: Long): ReactionDirective? {
        val entry = activeEntry ?: return null
        val phases = entry.definition.phases

        entry.phaseTimer += deltaMillis
        if (entry.phaseTimer >= phases[entry.phaseIndex].holdDurationMs) {
            entry.phaseTimer = 0L
            entry.phaseIndex++
        }

        if (entry.phaseIndex >= phases.size) {
            activeEntry = null
            onCompleted?.invoke()
            return null
        }

        val phase = phases[entry.phaseIndex]
        return ReactionDirective(targets = phase.targets, profile = phase.transitionProfile)
    }
}
