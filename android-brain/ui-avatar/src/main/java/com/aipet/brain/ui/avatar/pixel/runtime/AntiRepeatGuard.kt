package com.aipet.brain.ui.avatar.pixel.runtime

/**
 * Tracks recently played beat IDs and individual cooldowns to prevent visible repetition.
 *
 * Strategy:
 *  - historySize most-recent beats are excluded from selection.
 *  - Each beat records a cooldown period; during cooldown it is excluded regardless of history.
 *  - If filtering removes all candidates, history restriction is relaxed first, then cooldowns.
 */
internal class AntiRepeatGuard(private val historySize: Int = 4) {

    private val history = ArrayDeque<String>(historySize)
    private val cooldownExpiry = mutableMapOf<String, Long>()
    private var elapsedMs = 0L

    fun tick(deltaMillis: Long) {
        elapsedMs += deltaMillis
    }

    fun record(id: String, cooldownMs: Long) {
        if (history.size >= historySize) history.removeFirst()
        history.addLast(id)
        cooldownExpiry[id] = elapsedMs + cooldownMs
    }

    fun filter(candidates: List<IdleBeat>): List<IdleBeat> {
        // Tier 1: exclude both cooldown and recent history
        val strict = candidates.filter { !isOnCooldown(it.id) && it.id !in history }
        if (strict.isNotEmpty()) return strict

        // Tier 2: allow history repeats but still respect cooldowns
        val relaxed = candidates.filter { !isOnCooldown(it.id) }
        if (relaxed.isNotEmpty()) return relaxed

        // Tier 3: no valid exclusions — return all candidates unchanged
        return candidates
    }

    private fun isOnCooldown(id: String): Boolean {
        val expiry = cooldownExpiry[id] ?: return false
        return elapsedMs < expiry
    }
}
