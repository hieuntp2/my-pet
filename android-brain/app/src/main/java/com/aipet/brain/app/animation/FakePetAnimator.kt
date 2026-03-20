package com.aipet.brain.app.animation

import com.aipet.brain.app.ui.audio.model.AudioCategory
import com.aipet.brain.brain.activity.PetActivityType
import com.aipet.brain.brain.pet.PetEmotion
import com.aipet.brain.brain.pet.PetGreetingReaction
import com.aipet.brain.ui.avatar.model.AvatarEmotion
import com.aipet.brain.ui.avatar.model.AvatarEyeState
import com.aipet.brain.ui.avatar.model.AvatarMouthState
import com.aipet.brain.ui.avatar.model.AvatarState
import com.aipet.brain.ui.avatar.model.AvatarStateRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class FakePetAnimator(
    private val scope: CoroutineScope,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : PetAnimator {
    private val mutableState = MutableStateFlow(
        PetAnimationState(
            baseEmotion = PetEmotion.IDLE,
            activeEmotion = PetEmotion.IDLE,
            source = PetAnimationSource.MOOD,
            updatedAtMs = clock(),
            inputFrame = PetAnimationFrame(),
            surfaceState = surfaceStateFor(
                inputFrame = PetAnimationFrame(),
                emotion = PetEmotion.IDLE,
                idleEyeOverride = null,
                idleMouthOverride = null,
                blinkEyeOverride = null
            )
        )
    )
    private var transientAnimationJob: Job? = null
    private var blinkLoopJob: Job? = null
    private var idleLoopJob: Job? = null
    private var idleEyeOverride: AvatarEyeState? = null
    private var idleMouthOverride: AvatarMouthState? = null
    private var blinkEyeOverride: AvatarEyeState? = null

    override val state: StateFlow<PetAnimationState> = mutableState.asStateFlow()

    init {
        ensureIdleMotionLoopsStarted()
    }

    override fun syncInputFrame(frame: PetAnimationFrame) {
        mutableState.update { current ->
            val nextActiveEmotion = if (current.source.isTransient()) {
                current.activeEmotion
            } else {
                frame.emotion.toPetEmotion()
            }
            current.copy(
                baseEmotion = frame.emotion.toPetEmotion(),
                activeEmotion = nextActiveEmotion,
                source = if (current.source.isTransient()) current.source else PetAnimationSource.MOOD,
                updatedAtMs = clock(),
                inputFrame = frame,
                surfaceState = surfaceStateFor(
                    inputFrame = frame,
                    emotion = nextActiveEmotion,
                    idleEyeOverride = idleEyeOverride,
                    idleMouthOverride = idleMouthOverride,
                    blinkEyeOverride = blinkEyeOverride
                )
            )
        }
    }

    override fun playTrigger(
        trigger: PetAnimationTrigger,
        durationMs: Long
    ) {
        val emotion = trigger.emotion?.toPetEmotion() ?: state.value.baseEmotion
        playTransient(
            emotion = emotion,
            source = trigger.reactionType.toAnimationSource(),
            durationMs = durationMs,
            trigger = trigger
        )
    }

    override fun syncMood(emotion: PetEmotion) {
        syncInputFrame(
            state.value.inputFrame.copy(
                emotion = emotion.toAnimationEmotion(),
                trigger = PetAnimationTrigger.none()
            )
        )
    }

    override fun playGreeting(greeting: PetGreetingReaction, durationMs: Long) {
        playTrigger(
            trigger = PetAnimationTrigger(
                reactionType = PetAnimationReactionType.GREETING,
                emotion = greeting.emotion.toAnimationEmotion(),
                greetingType = when (greeting.emotion) {
                    PetEmotion.HUNGRY -> PetAnimationGreetingType.HUNGRY
                    PetEmotion.SLEEPY -> PetAnimationGreetingType.SLEEPY
                    PetEmotion.SAD -> PetAnimationGreetingType.LONELY
                    PetEmotion.CURIOUS -> PetAnimationGreetingType.CURIOUS
                    PetEmotion.EXCITED -> PetAnimationGreetingType.PLAYFUL
                    PetEmotion.HAPPY -> PetAnimationGreetingType.WARM
                    PetEmotion.IDLE -> PetAnimationGreetingType.CALM
                    PetEmotion.THINKING -> PetAnimationGreetingType.CALM
                }
            ),
            durationMs = durationMs
        )
    }

    override fun playReaction(
        emotion: PetEmotion,
        source: PetAnimationSource,
        durationMs: Long
    ) {
        playTrigger(
            trigger = PetAnimationTrigger(
                reactionType = source.toReactionType(),
                emotion = emotion.toAnimationEmotion()
            ),
            durationMs = durationMs
        )
    }

    override fun onTap(resultingEmotion: PetEmotion) {
        playTrigger(
            trigger = PetAnimationTrigger(
                reactionType = PetAnimationReactionType.TAP,
                emotion = resultingEmotion.toAnimationEmotion()
            ),
            durationMs = TAP_DURATION_MS
        )
    }

    override fun onLongPress(resultingEmotion: PetEmotion) {
        playTrigger(
            trigger = PetAnimationTrigger(
                reactionType = PetAnimationReactionType.LONG_PRESS,
                emotion = resultingEmotion.toAnimationEmotion()
            ),
            durationMs = LONG_PRESS_DURATION_MS
        )
    }

    override fun onActivityResult(
        activityType: PetActivityType,
        resultingEmotion: PetEmotion
    ) {
        playTrigger(
            trigger = PetAnimationTrigger(
                reactionType = activityType.toReactionType(),
                emotion = resultingEmotion.toAnimationEmotion(),
                activityResult = activityType.toActivityResult()
            ),
            durationMs = activityType.toDurationMs()
        )
    }

    override fun onSoundReaction(category: AudioCategory) {
        playTrigger(
            trigger = PetAnimationTrigger(
                reactionType = PetAnimationReactionType.SOUND,
                emotion = category.toEmotion().toAnimationEmotion()
            ),
            durationMs = SOUND_DURATION_MS
        )
    }

    private fun playTransient(
        emotion: PetEmotion,
        source: PetAnimationSource,
        durationMs: Long,
        trigger: PetAnimationTrigger
    ) {
        transientAnimationJob?.cancel()
        mutableState.update { current ->
            val updatedFrame = current.inputFrame.copy(trigger = trigger)
            current.copy(
                activeEmotion = emotion,
                source = source,
                updatedAtMs = clock(),
                inputFrame = updatedFrame,
                surfaceState = surfaceStateFor(
                    inputFrame = updatedFrame,
                    emotion = emotion,
                    idleEyeOverride = idleEyeOverride,
                    idleMouthOverride = idleMouthOverride,
                    blinkEyeOverride = blinkEyeOverride
                )
            )
        }
        transientAnimationJob = scope.launch {
            delay(durationMs)
            restoreBaseEmotion()
        }
    }

    private fun restoreBaseEmotion() {
        mutableState.update { current ->
            val restoredFrame = current.inputFrame.copy(trigger = PetAnimationTrigger.none())
            current.copy(
                activeEmotion = current.baseEmotion,
                source = PetAnimationSource.MOOD,
                updatedAtMs = clock(),
                inputFrame = restoredFrame,
                surfaceState = surfaceStateFor(
                    inputFrame = restoredFrame,
                    emotion = current.baseEmotion,
                    idleEyeOverride = idleEyeOverride,
                    idleMouthOverride = idleMouthOverride,
                    blinkEyeOverride = blinkEyeOverride
                )
            )
        }
    }

    private fun PetAnimationSource.isTransient(): Boolean {
        return this != PetAnimationSource.MOOD
    }

    private fun PetActivityType.toDurationMs(): Long {
        return when (this) {
            PetActivityType.FEED -> 1_000L
            PetActivityType.PLAY -> 1_250L
            PetActivityType.REST -> 1_400L
        }
    }

    private fun AudioCategory.toEmotion(): PetEmotion {
        return when (this) {
            AudioCategory.ACKNOWLEDGMENT,
            AudioCategory.GREETING,
            AudioCategory.HAPPY -> PetEmotion.HAPPY
            AudioCategory.CURIOUS -> PetEmotion.CURIOUS
            AudioCategory.SLEEPY -> PetEmotion.SLEEPY
            AudioCategory.SURPRISED -> PetEmotion.EXCITED
            AudioCategory.WARNING_NO -> PetEmotion.SAD
        }
    }

    private fun ensureIdleMotionLoopsStarted() {
        if (blinkLoopJob == null) {
            blinkLoopJob = scope.launch {
                while (currentCoroutineContext().isActive) {
                    val frame = state.value.inputFrame
                    delay(frame.nextBlinkDelayMs())
                    blinkEyeOverride = AvatarEyeState.CLOSED
                    refreshSurfaceState()
                    delay(frame.blinkHoldMs())
                    blinkEyeOverride = null
                    refreshSurfaceState()
                }
            }
        }
        if (idleLoopJob == null) {
            idleLoopJob = scope.launch {
                while (currentCoroutineContext().isActive) {
                    val frame = state.value.inputFrame
                    delay(frame.nextIdleCueDelayMs())
                    when (frame.idleCueType()) {
                        IdleCueType.SOFT_EYES -> {
                            idleEyeOverride = AvatarEyeState.HALF_OPEN
                            refreshSurfaceState()
                            delay(350L)
                            idleEyeOverride = null
                            refreshSurfaceState()
                        }

                        IdleCueType.SMALL_MOUTH -> {
                            idleMouthOverride = AvatarMouthState.SMALL_O
                            refreshSurfaceState()
                            delay(450L)
                            idleMouthOverride = null
                            refreshSurfaceState()
                        }

                        IdleCueType.SMILE -> {
                            idleMouthOverride = AvatarMouthState.SMILE
                            refreshSurfaceState()
                            delay(420L)
                            idleMouthOverride = null
                            refreshSurfaceState()
                        }
                    }
                }
            }
        }
    }

    private fun refreshSurfaceState() {
        mutableState.update { current ->
            current.copy(
                updatedAtMs = clock(),
                surfaceState = surfaceStateFor(
                    inputFrame = current.inputFrame,
                    emotion = current.activeEmotion,
                    idleEyeOverride = idleEyeOverride,
                    idleMouthOverride = idleMouthOverride,
                    blinkEyeOverride = blinkEyeOverride
                )
            )
        }
    }

    private fun surfaceStateFor(
        inputFrame: PetAnimationFrame,
        emotion: PetEmotion,
        idleEyeOverride: AvatarEyeState?,
        idleMouthOverride: AvatarMouthState?,
        blinkEyeOverride: AvatarEyeState?
    ): PetAnimationSurfaceState {
        val semanticBaseState = baseStateFor(inputFrame, emotion)
        val resolvedState = AvatarStateRules.applyTransientOverrides(
            baseState = semanticBaseState,
            idleEyeOverride = idleEyeOverride,
            idleMouthOverride = idleMouthOverride,
            blinkEyeOverride = blinkEyeOverride
        )
        return PetAnimationSurfaceState.AvatarFaceSurface(
            avatarState = AvatarStateRules.normalizeForRender(resolvedState)
        )
    }

    private fun baseStateFor(
        inputFrame: PetAnimationFrame,
        emotion: PetEmotion
    ): AvatarState {
        val triggerState = triggerStateFor(inputFrame.trigger)
        if (triggerState != null) {
            return triggerState
        }
        val baseEmotion = inputFrame.emotion.toAvatarEmotion()
        val stateFromEmotion = AvatarStateRules.stateForEmotion(baseEmotion)
        return when {
            inputFrame.energyBand == PetAnimationBand.LOW || inputFrame.hungerBand == PetAnimationBand.HIGH -> stateFromEmotion.copy(
                eyeState = AvatarEyeState.HALF_OPEN
            )

            inputFrame.flavor == PetAnimationFlavor.CURIOUS || emotion == PetEmotion.CURIOUS -> stateFromEmotion.copy(
                mouthState = AvatarMouthState.SMALL_O,
                eyeState = AvatarEyeState.HALF_OPEN
            )

            inputFrame.flavor == PetAnimationFlavor.AFFECTIONATE || inputFrame.socialBand == PetAnimationBand.HIGH -> stateFromEmotion.copy(
                mouthState = AvatarMouthState.SMILE
            )

            inputFrame.flavor == PetAnimationFlavor.PLAYFUL && inputFrame.energyBand != PetAnimationBand.LOW -> stateFromEmotion.copy(
                mouthState = AvatarMouthState.SMILE,
                eyeState = AvatarEyeState.OPEN
            )

            inputFrame.flavor == PetAnimationFlavor.CALM || inputFrame.emotion == PetAnimationEmotion.SLEEPY -> stateFromEmotion.copy(
                eyeState = AvatarEyeState.HALF_OPEN
            )

            else -> stateFromEmotion
        }
    }

    private fun triggerStateFor(trigger: PetAnimationTrigger): AvatarState? {
        return when (trigger.reactionType) {
            PetAnimationReactionType.NONE -> null
            PetAnimationReactionType.GREETING -> when (trigger.greetingType ?: PetAnimationGreetingType.CALM) {
                PetAnimationGreetingType.CALM -> AvatarState(AvatarEmotion.NEUTRAL, AvatarEyeState.OPEN, AvatarMouthState.NEUTRAL)
                PetAnimationGreetingType.WARM -> AvatarState(AvatarEmotion.HAPPY, AvatarEyeState.OPEN, AvatarMouthState.SMILE)
                PetAnimationGreetingType.HUNGRY -> AvatarState(AvatarEmotion.CURIOUS, AvatarEyeState.OPEN, AvatarMouthState.SMALL_O)
                PetAnimationGreetingType.SLEEPY -> AvatarState(AvatarEmotion.SLEEPY, AvatarEyeState.HALF_OPEN, AvatarMouthState.NEUTRAL)
                PetAnimationGreetingType.LONELY -> AvatarState(AvatarEmotion.HAPPY, AvatarEyeState.HALF_OPEN, AvatarMouthState.SMILE)
                PetAnimationGreetingType.CURIOUS -> AvatarState(AvatarEmotion.CURIOUS, AvatarEyeState.HALF_OPEN, AvatarMouthState.SMALL_O)
                PetAnimationGreetingType.PLAYFUL -> AvatarState(AvatarEmotion.SURPRISED, AvatarEyeState.OPEN, AvatarMouthState.OPEN)
            }

            PetAnimationReactionType.TAP -> AvatarState(AvatarEmotion.HAPPY, AvatarEyeState.OPEN, AvatarMouthState.SMALL_O)
            PetAnimationReactionType.LONG_PRESS -> AvatarState(AvatarEmotion.HAPPY, AvatarEyeState.HALF_OPEN, AvatarMouthState.SMILE)
            PetAnimationReactionType.FEED -> AvatarState(AvatarEmotion.HAPPY, AvatarEyeState.OPEN, AvatarMouthState.SMILE)
            PetAnimationReactionType.PLAY -> AvatarState(AvatarEmotion.SURPRISED, AvatarEyeState.OPEN, AvatarMouthState.OPEN)
            PetAnimationReactionType.REST -> AvatarState(AvatarEmotion.SLEEPY, AvatarEyeState.HALF_OPEN, AvatarMouthState.NEUTRAL)
            PetAnimationReactionType.SOUND -> AvatarState(AvatarEmotion.SURPRISED, AvatarEyeState.OPEN, AvatarMouthState.SMALL_O)
            PetAnimationReactionType.REACTION -> AvatarStateRules.stateForEmotion((trigger.emotion ?: PetAnimationEmotion.HAPPY).toAvatarEmotion())
        }
    }

    private fun PetAnimationFrame.nextBlinkDelayMs(): Long {
        return when (energyBand) {
            PetAnimationBand.LOW -> Random.nextLong(from = 2_400L, until = 4_400L)
            PetAnimationBand.MID -> Random.nextLong(from = 3_200L, until = 5_800L)
            PetAnimationBand.HIGH -> Random.nextLong(from = 4_200L, until = 7_000L)
        }
    }

    private fun PetAnimationFrame.blinkHoldMs(): Long {
        return when (emotion) {
            PetAnimationEmotion.SLEEPY -> 220L
            else -> 150L
        }
    }

    private fun PetAnimationFrame.nextIdleCueDelayMs(): Long {
        return when (energyBand) {
            PetAnimationBand.LOW -> Random.nextLong(from = 6_000L, until = 10_000L)
            PetAnimationBand.MID -> Random.nextLong(from = 4_500L, until = 8_000L)
            PetAnimationBand.HIGH -> Random.nextLong(from = 3_000L, until = 6_000L)
        }
    }

    private fun PetAnimationFrame.idleCueType(): IdleCueType {
        return when {
            emotion == PetAnimationEmotion.SLEEPY || flavor == PetAnimationFlavor.CALM -> IdleCueType.SOFT_EYES
            flavor == PetAnimationFlavor.PLAYFUL || energyBand == PetAnimationBand.HIGH -> IdleCueType.SMALL_MOUTH
            flavor == PetAnimationFlavor.AFFECTIONATE || socialBand == PetAnimationBand.HIGH -> IdleCueType.SMILE
            hungerBand == PetAnimationBand.HIGH -> IdleCueType.SMALL_MOUTH
            else -> IdleCueType.SOFT_EYES
        }
    }

    private fun PetAnimationReactionType.toAnimationSource(): PetAnimationSource {
        return when (this) {
            PetAnimationReactionType.NONE -> PetAnimationSource.MOOD
            PetAnimationReactionType.GREETING -> PetAnimationSource.GREETING
            PetAnimationReactionType.TAP -> PetAnimationSource.TAP
            PetAnimationReactionType.LONG_PRESS -> PetAnimationSource.LONG_PRESS
            PetAnimationReactionType.FEED -> PetAnimationSource.FEED
            PetAnimationReactionType.PLAY -> PetAnimationSource.PLAY
            PetAnimationReactionType.REST -> PetAnimationSource.REST
            PetAnimationReactionType.SOUND -> PetAnimationSource.SOUND
            PetAnimationReactionType.REACTION -> PetAnimationSource.REACTION
        }
    }

    private fun PetAnimationSource.toReactionType(): PetAnimationReactionType {
        return when (this) {
            PetAnimationSource.MOOD -> PetAnimationReactionType.REACTION
            PetAnimationSource.GREETING -> PetAnimationReactionType.GREETING
            PetAnimationSource.REACTION -> PetAnimationReactionType.REACTION
            PetAnimationSource.TAP -> PetAnimationReactionType.TAP
            PetAnimationSource.LONG_PRESS -> PetAnimationReactionType.LONG_PRESS
            PetAnimationSource.FEED -> PetAnimationReactionType.FEED
            PetAnimationSource.PLAY -> PetAnimationReactionType.PLAY
            PetAnimationSource.REST -> PetAnimationReactionType.REST
            PetAnimationSource.SOUND -> PetAnimationReactionType.SOUND
        }
    }

    private fun PetActivityType.toReactionType(): PetAnimationReactionType {
        return when (this) {
            PetActivityType.FEED -> PetAnimationReactionType.FEED
            PetActivityType.PLAY -> PetAnimationReactionType.PLAY
            PetActivityType.REST -> PetAnimationReactionType.REST
        }
    }

    private fun PetActivityType.toActivityResult(): PetAnimationActivityResult {
        return when (this) {
            PetActivityType.FEED -> PetAnimationActivityResult.FEED
            PetActivityType.PLAY -> PetAnimationActivityResult.PLAY
            PetActivityType.REST -> PetAnimationActivityResult.REST
        }
    }

    private fun PetAnimationEmotion.toPetEmotion(): PetEmotion {
        return when (this) {
            PetAnimationEmotion.CALM -> PetEmotion.IDLE
            PetAnimationEmotion.HAPPY -> PetEmotion.HAPPY
            PetAnimationEmotion.CURIOUS -> PetEmotion.CURIOUS
            PetAnimationEmotion.SLEEPY -> PetEmotion.SLEEPY
            PetAnimationEmotion.SAD -> PetEmotion.SAD
            PetAnimationEmotion.EXCITED -> PetEmotion.EXCITED
            PetAnimationEmotion.HUNGRY -> PetEmotion.HUNGRY
        }
    }

    private fun PetEmotion.toAnimationEmotion(): PetAnimationEmotion {
        return when (this) {
            PetEmotion.IDLE -> PetAnimationEmotion.CALM
            PetEmotion.HAPPY -> PetAnimationEmotion.HAPPY
            PetEmotion.CURIOUS -> PetAnimationEmotion.CURIOUS
            PetEmotion.SLEEPY -> PetAnimationEmotion.SLEEPY
            PetEmotion.SAD -> PetAnimationEmotion.SAD
            PetEmotion.EXCITED -> PetAnimationEmotion.EXCITED
            PetEmotion.HUNGRY -> PetAnimationEmotion.HUNGRY
            PetEmotion.THINKING -> PetAnimationEmotion.CALM
        }
    }

    private fun PetAnimationEmotion.toAvatarEmotion(): AvatarEmotion {
        return when (this) {
            PetAnimationEmotion.CALM -> AvatarEmotion.NEUTRAL
            PetAnimationEmotion.HAPPY -> AvatarEmotion.HAPPY
            PetAnimationEmotion.CURIOUS -> AvatarEmotion.CURIOUS
            PetAnimationEmotion.SLEEPY -> AvatarEmotion.SLEEPY
            PetAnimationEmotion.SAD -> AvatarEmotion.SLEEPY
            PetAnimationEmotion.EXCITED -> AvatarEmotion.SURPRISED
            PetAnimationEmotion.HUNGRY -> AvatarEmotion.CURIOUS
        }
    }

    private enum class IdleCueType {
        SOFT_EYES,
        SMALL_MOUTH,
        SMILE
    }

    private companion object {
        const val TAP_DURATION_MS = 800L
        const val LONG_PRESS_DURATION_MS = 1_200L
        const val SOUND_DURATION_MS = 1_100L
    }
}
