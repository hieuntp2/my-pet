package com.aipet.brain.app.animation

import com.aipet.brain.app.ui.audio.model.AudioCategory
import com.aipet.brain.brain.activity.PetActivityType
import com.aipet.brain.brain.pet.PetEmotion
import com.aipet.brain.brain.pet.PetGreetingReaction
import com.aipet.brain.pixel.avatar.clips.ClipRegistry
import com.aipet.brain.pixel.avatar.controller.PixelAnimationController
import com.aipet.brain.pixel.avatar.model.PetVisualState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * [PetAnimator] implementation backed by the pixel eye animation system.
 *
 * Responsibilities:
 * - Owns a [PixelAnimationController] and drives it based on incoming emotion signals.
 * - Maps [PetEmotion] -> [PetVisualState] via [PetEmotionToVisualStateMapper].
 * - Produces [PetAnimationSurfaceState.PixelPetSurface] for the UI layer.
 * - Handles transient triggers (greetings, reactions, taps) by temporarily switching states.
 */
class PixelPetAnimator(
    private val scope: CoroutineScope,
    private val random: Random = Random.Default
) : PetAnimator {

    private val controller = PixelAnimationController(
        scope = scope,
        stateRegistry = ClipRegistry.ALL_STATES,
        random = random
    )

    private var currentBaseEmotion: PetEmotion = PetEmotion.IDLE
    private var transientJob: Job? = null

    private val _state = MutableStateFlow(
        PetAnimationState(
            baseEmotion = PetEmotion.IDLE,
            activeEmotion = PetEmotion.IDLE,
            source = PetAnimationSource.MOOD,
            runtimeMode = PetAnimationRuntimeMode.PIXEL,
            surfaceState = PetAnimationSurfaceState.PixelPetSurface(
                frame = controller.currentFrame.value
            )
        )
    )
    override val state: StateFlow<PetAnimationState> = _state.asStateFlow()

    init {
        // Sync surface state whenever the controller emits a new frame
        controller.currentFrame.onEach { frame ->
            _state.value = _state.value.copy(
                surfaceState = PetAnimationSurfaceState.PixelPetSurface(frame = frame)
            )
        }.launchIn(scope)
    }

    override fun syncInputFrame(frame: PetAnimationFrame) {
        val emotion = frame.emotion.toPetEmotion()
        syncMood(emotion)
    }

    override fun syncMood(emotion: PetEmotion) {
        currentBaseEmotion = emotion
        if (transientJob == null || transientJob?.isActive == false) {
            applyEmotion(emotion, PetAnimationSource.MOOD)
        }
    }

    override fun playGreeting(greeting: PetGreetingReaction, durationMs: Long) {
        playTransient(greeting.emotion, PetAnimationSource.GREETING, durationMs)
    }

    override fun playReaction(emotion: PetEmotion, source: PetAnimationSource, durationMs: Long) {
        playTransient(emotion, source, durationMs)
    }

    override fun onTap(resultingEmotion: PetEmotion) {
        playTransient(resultingEmotion, PetAnimationSource.TAP, PetAnimator.DEFAULT_REACTION_DURATION_MS)
    }

    override fun onLongPress(resultingEmotion: PetEmotion) {
        playTransient(resultingEmotion, PetAnimationSource.LONG_PRESS, PetAnimator.DEFAULT_REACTION_DURATION_MS)
    }

    override fun onActivityResult(activityType: PetActivityType, resultingEmotion: PetEmotion) {
        val source = when (activityType) {
            PetActivityType.FEED -> PetAnimationSource.FEED
            PetActivityType.PLAY -> PetAnimationSource.PLAY
            PetActivityType.REST -> PetAnimationSource.REST
        }
        playTransient(resultingEmotion, source, PetAnimator.DEFAULT_REACTION_DURATION_MS)
    }

    override fun onSoundReaction(category: AudioCategory) {
        val emotion = PetEmotionMappings.audioToPetEmotionOrNull(category) ?: currentBaseEmotion
        playTransient(emotion, PetAnimationSource.SOUND, 1_500L)
    }

    override fun playTrigger(trigger: PetAnimationTrigger, durationMs: Long) {
        val emotion = trigger.emotion?.toPetEmotion() ?: currentBaseEmotion
        val source = trigger.reactionType.toAnimationSource()
        playTransient(emotion, source, durationMs)
    }

    // ---------- internals ----------

    private fun applyEmotion(emotion: PetEmotion, source: PetAnimationSource) {
        val visualState = PetEmotionToVisualStateMapper.map(emotion)
        controller.setVisualState(visualState)
        _state.value = _state.value.copy(
            activeEmotion = emotion,
            source = source,
            runtimeMode = PetAnimationRuntimeMode.PIXEL
        )
    }

    private fun playTransient(emotion: PetEmotion, source: PetAnimationSource, durationMs: Long) {
        transientJob?.cancel()
        applyEmotion(emotion, source)
        _state.value = _state.value.copy(baseEmotion = currentBaseEmotion, activeEmotion = emotion)
        transientJob = scope.launch {
            delay(durationMs)
            applyEmotion(currentBaseEmotion, PetAnimationSource.MOOD)
            transientJob = null
        }
    }

    private fun PetAnimationReactionType.toAnimationSource(): PetAnimationSource = when (this) {
        PetAnimationReactionType.GREETING -> PetAnimationSource.GREETING
        PetAnimationReactionType.TAP -> PetAnimationSource.TAP
        PetAnimationReactionType.LONG_PRESS -> PetAnimationSource.LONG_PRESS
        PetAnimationReactionType.FEED -> PetAnimationSource.FEED
        PetAnimationReactionType.PLAY -> PetAnimationSource.PLAY
        PetAnimationReactionType.REST -> PetAnimationSource.REST
        else -> PetAnimationSource.MOOD
    }

    private fun PetAnimationEmotion.toPetEmotion(): PetEmotion =
        PetEmotionMappings.animationToPetEmotion(this)
}
