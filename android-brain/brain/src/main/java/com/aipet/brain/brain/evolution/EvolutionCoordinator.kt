package com.aipet.brain.brain.evolution

import android.util.Log
import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.PetActivityAppliedEventPayload
import com.aipet.brain.brain.personality.PetTrait
import com.aipet.brain.brain.personality.PetTraitRepository
import com.aipet.brain.brain.pet.PetCondition
import com.aipet.brain.brain.pet.PetEmotion
import com.aipet.brain.brain.pet.PetMood
import com.aipet.brain.brain.pet.PetState
import com.aipet.brain.brain.evolution.domain.BondRepositoryV2
import com.aipet.brain.brain.evolution.domain.BondStateV2
import com.aipet.brain.brain.evolution.domain.EpisodeRepository
import com.aipet.brain.brain.evolution.domain.MemoryEpisode
import com.aipet.brain.brain.evolution.domain.UserHabitRepository
import com.aipet.brain.brain.evolution.domain.SemanticMemoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Central coordinator for all evolution system components.
 *
 * Responsibilities:
 * - Manages the open episode candidate lifecycle
 * - Persists episodes on session close
 * - Runs inference (semantic facts, habit, personality) after sessions
 * - Updates the v2 bond state
 * - Provides evolution context to UI and behavior engine
 *
 * All heavy operations run on Dispatchers.IO.
 * This object is long-lived — create it once and share via constructor injection.
 */
class EvolutionCoordinator(
    private val episodeRepository: EpisodeRepository,
    private val semanticRepository: SemanticMemoryRepository,
    private val bondRepository: BondRepositoryV2,
    private val habitRepository: UserHabitRepository,
    private val traitRepository: PetTraitRepository,
    private val eventBus: EventBus,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val groupingEngine = EpisodeGroupingEngine(
        summarizer = EpisodeSummarizer(),
        importanceScorer = ImportanceScorer(),
        nowProvider = nowProvider
    )
    private val semanticInference = SemanticFactInferenceEngine(semanticRepository, nowProvider)
    private val habitAggregator = HabitAggregator(habitRepository, nowProvider)
    private val relationshipEngine = RelationshipUpdateEngineV2(nowProvider)
    private val personalityEngine = PersonalityEvolutionEngine(nowProvider)
    private val reunionResolver = ReunionResolver()
    private val expectationResolver = ExpectedReturnWindowResolver()

    // Cached context exposed for behavior engine and debug
    private val _evolutionContext = MutableStateFlow<EvolutionContext?>(null)
    val evolutionContext: StateFlow<EvolutionContext?> = _evolutionContext.asStateFlow()

    /**
     * Call when the app opens (app-open lifecycle trigger).
     * Resolves reunion type, starts episode, and builds the evolution context.
     */
    fun onAppOpened(petState: PetState, petTraits: PetTrait?) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val now = nowProvider()
                val bond = bondRepository.load()
                val habit = habitRepository.load()
                val currentPhase = DayPhaseResolver.resolve(now)
                val absenceMs = if (petState.lastOpenAt > 0) now - petState.lastOpenAt else 0L

                val reunionType = reunionResolver.resolve(
                    ReunionResolver.ReunionContext(
                        absenceMs = absenceMs,
                        bond = bond,
                        habitProfile = habit,
                        neglectStreak = petState.neglectStreak,
                        careStreak = petState.careStreak,
                        lastExpectedWindowDaypart = habit.strongestDaypart,
                        currentDaypart = currentPhase.name,
                        nowMs = now
                    )
                )
                val expectation = expectationResolver.resolve(habit, currentPhase, absenceMs)

                groupingEngine.onSessionStarted(reunionType.name)

                val recentEpisodes = episodeRepository.loadRecent(20)
                val lifecycleModifiers = LifecycleBaselineModifiers.for_(currentPhase)
                val personalityProfile = petTraits?.let { DerivedPersonalityProfile.from(it) } ?: "unknown"

                _evolutionContext.value = EvolutionContext(
                    bond = bond,
                    habitProfile = habit,
                    dayPhase = currentPhase,
                    reunionType = reunionType,
                    expectationState = expectation,
                    recentEpisodes = recentEpisodes,
                    lifecycleModifiers = lifecycleModifiers,
                    personalityProfile = personalityProfile
                )

                publishReunionEvent(reunionType, absenceMs, bond)
                Log.d(TAG, "App opened: reunionType=$reunionType phase=$currentPhase")
            }.onFailure { e ->
                Log.e(TAG, "Error in onAppOpened", e)
            }
        }
    }

    /** Record a pet event for the current episode. */
    fun recordPetEvent(
        emotion: PetEmotion,
        mood: PetMood,
        isInteraction: Boolean,
        interactionType: String? = null
    ) {
        groupingEngine.recordEvent(
            emotionName = emotion.name,
            moodName = mood.name,
            isInteraction = isInteraction,
            interactionType = interactionType
        )
    }

    /** Record a care quality change for the current episode. */
    fun recordCareChange(careScoreDelta: Int, bondDelta: Int) {
        groupingEngine.recordCareChange(careScoreDelta, bondDelta)
    }

    /** Record a neglect signal for the current episode. */
    fun recordNeglectSignal() {
        groupingEngine.recordNeglectSignal()
    }

    /**
     * Subscribe to the event bus to auto-record pet interactions into the open episode.
     * Call once on app startup from PetBrainApp. Runs on Default dispatcher, lightweight.
     */
    fun startObservingEvents() {
        scope.launch {
            eventBus.observe().collect { event ->
                handleEventForEpisode(event)
            }
        }
    }

    private fun handleEventForEpisode(event: EventEnvelope) {
        when (event.type) {
            EventType.PET_FED -> {
                val payload = PetActivityAppliedEventPayload.fromJson(event.payloadJson)
                val bondDelta = payload?.bondDelta ?: 1
                recordPetEvent(
                    emotion = PetEmotion.HAPPY,
                    mood = PetMood.HAPPY,
                    isInteraction = true,
                    interactionType = "FEED"
                )
                recordCareChange(careScoreDelta = 10, bondDelta = bondDelta)
            }
            EventType.PET_PLAYED -> {
                val payload = PetActivityAppliedEventPayload.fromJson(event.payloadJson)
                val bondDelta = payload?.bondDelta ?: 0
                recordPetEvent(
                    emotion = PetEmotion.EXCITED,
                    mood = PetMood.EXCITED,
                    isInteraction = true,
                    interactionType = "PLAY"
                )
                recordCareChange(careScoreDelta = 5, bondDelta = bondDelta)
            }
            EventType.PET_RESTED -> {
                val payload = PetActivityAppliedEventPayload.fromJson(event.payloadJson)
                val bondDelta = payload?.bondDelta ?: 0
                recordPetEvent(
                    emotion = PetEmotion.IDLE,
                    mood = PetMood.NEUTRAL,
                    isInteraction = true,
                    interactionType = "REST"
                )
                recordCareChange(careScoreDelta = 3, bondDelta = bondDelta)
            }
            EventType.PET_GREETED -> {
                recordPetEvent(
                    emotion = PetEmotion.HAPPY,
                    mood = PetMood.HAPPY,
                    isInteraction = true,
                    interactionType = "GREET"
                )
            }
            EventType.AFFECTION_INTERACTION,
            EventType.USER_INTERACTED_PET -> {
                recordPetEvent(
                    emotion = PetEmotion.HAPPY,
                    mood = PetMood.HAPPY,
                    isInteraction = true,
                    interactionType = "TOUCH"
                )
            }
            EventType.PET_LONG_PRESSED -> {
                recordPetEvent(
                    emotion = PetEmotion.HAPPY,
                    mood = PetMood.HAPPY,
                    isInteraction = true,
                    interactionType = "CUDDLE"
                )
                recordCareChange(careScoreDelta = 2, bondDelta = 1)
            }
            EventType.PET_NEGLECT_APPLIED -> {
                recordNeglectSignal()
                recordCareChange(careScoreDelta = -10, bondDelta = -2)
            }
            EventType.PET_OVERSTIMULATION_APPLIED -> {
                recordCareChange(careScoreDelta = -5, bondDelta = 0)
            }
            EventType.PET_CARE_ACTION_APPLIED -> {
                // Covered by the specific PET_FED/PLAYED/RESTED above; skip here
                Unit
            }
            EventType.BRAIN_STATE_CHANGED -> {
                // Passive state recording — keeps activity metric alive between interactions
                recordPetEvent(
                    emotion = PetEmotion.IDLE,
                    mood = PetMood.NEUTRAL,
                    isInteraction = false
                )
            }
            else -> Unit
        }
    }

    /**
     * Finalizes the episode, persists it, runs inference.
     */
    fun onSessionEnded(petState: PetState, traits: PetTrait?) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val episode = groupingEngine.closeCurrentEpisode()
                if (episode != null) {
                    episodeRepository.save(episode)
                    Log.d(TAG, "Episode saved: id=${episode.id} importance=${episode.importanceScore}")
                    runPostSessionInference(episode, petState, traits)
                }
            }.onFailure { e ->
                Log.e(TAG, "Error in onSessionEnded", e)
            }
        }
    }

    private suspend fun runPostSessionInference(
        episode: MemoryEpisode,
        petState: PetState,
        traits: PetTrait?
    ) {
        val recentEpisodes = episodeRepository.loadRecent(20)
        val bond = bondRepository.load()

        // Update bond v2
        val sessionOutcome = RelationshipUpdateEngineV2.SessionOutcome(
            careScoreDelta = episode.careScoreDelta,
            interactionCount = episode.interactionCount,
            hadNeglect = episode.neglectSignal,
            reunionType = ReunionType.entries
                .firstOrNull { it.name == episode.reunionType } ?: ReunionType.ROUTINE_RETURN,
            sessionDurationMs = episode.durationMs
        )
        val updatedBond = relationshipEngine.update(bond, sessionOutcome, recentEpisodes)
        bondRepository.save(updatedBond)

        // Infer semantic facts
        semanticInference.inferFromEpisodes(recentEpisodes)

        // Update habit profile
        habitAggregator.updateFromEpisodes(recentEpisodes)

        // Evolve personality traits
        if (traits != null) {
            val evolvedTraits = personalityEngine.evolve(traits, recentEpisodes)
            if (evolvedTraits != traits) {
                traitRepository.save(evolvedTraits)
                Log.d(TAG, "Personality traits evolved for petId=${traits.petId}")
            }
        }

        // Refresh context after inference
        val updatedHabit = habitRepository.load()
        val ctx = _evolutionContext.value
        if (ctx != null) {
            _evolutionContext.value = ctx.copy(
                bond = updatedBond,
                habitProfile = updatedHabit,
                recentEpisodes = recentEpisodes
            )
        }

        publishBondUpdatedEvent(updatedBond)
        Log.d(TAG, "Post-session inference complete. bond=${updatedBond.label()}")
    }

    private suspend fun publishReunionEvent(
        reunionType: ReunionType,
        absenceMs: Long,
        bond: BondStateV2
    ) {
        val payload = """{"reunionType":"${reunionType.name}","absenceMs":$absenceMs,"bondLabel":"${bond.label()}"}"""
        eventBus.publish(
            EventEnvelope.create(
                type = EventType.EVOLUTION_REUNION_RESOLVED,
                payloadJson = payload
            )
        )
    }

    private suspend fun publishBondUpdatedEvent(bond: BondStateV2) {
        val payload = """{"affection":${bond.affection},"trust":${bond.trust},"dependency":${bond.dependency},"stability":${bond.stability},"label":"${bond.label()}"}"""
        eventBus.publish(
            EventEnvelope.create(
                type = EventType.EVOLUTION_BOND_UPDATED,
                payloadJson = payload
            )
        )
    }

    private companion object {
        const val TAG = "EvolutionCoordinator"
    }
}
