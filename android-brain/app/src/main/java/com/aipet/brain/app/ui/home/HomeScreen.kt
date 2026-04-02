package com.aipet.brain.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aipet.brain.app.behavior.experience.TalkDirective
import com.aipet.brain.brain.logic.audio.AudioStimulus
import com.aipet.brain.brain.logic.audio.KeywordStimulus
import com.aipet.brain.brain.logic.audio.SoundStimulus
import com.aipet.brain.brain.pet.PetCondition
import com.aipet.brain.brain.pet.PetEmotion
import com.aipet.brain.brain.pet.PetGreetingReaction
import com.aipet.brain.brain.pet.PetState
import com.aipet.brain.brain.state.BrainState
import com.aipet.brain.ui.avatar.pixel.bridge.PixelPetBridgeState

/**
 * Full-screen pet stage � the redesigned Looi-like Home experience.
 *
 * Architecture:
 * - [PetReactionController] injects transient one-shot reactions (tap, greeting, sound)
 *   on top of the bridge-driven visual state. Each reaction auto-clears when its clip finishes.
 * - [HomeAmbientGlow] reacts to emotion/conditions for scene atmosphere.
 * - [HomeTalkBubbleOrchestrator] selects the contextual speech bubble with priority + dedupe.
 * - [HomeFxOverlay] shows particle FX tied to interaction outcomes.
 * - [SparkMiniGame] is the integrated Catch-the-Spark game, launched from the menu.
 *
 * Layout:
 * - Black near-black fullscreen stage (#080810)
 * - Pixel pet face centered at 300dp, -24dp vertical offset
 * - Talk bubble 180dp below center
 * - Single MoreVert menu button top-right
 * - No navigation tabs, no dashboard cards
 */
@Composable
fun HomeScreen(
    homeUiModel: HomeUiModel,
    homeInteractionUiState: HomeInteractionUiState,
    avatarBridgeState: PixelPetBridgeState,
    behaviorTalkDirective: TalkDirective?,
    isBehaviorExperienceAuthoritative: Boolean,
    appOpenGreeting: PetGreetingReaction?,
    latestAudioStimulus: AudioStimulus?,
    latestAudioOutputStartedAtMs: Long,
    invitationTriggerToken: Long,
    petState: PetState? = null,
    brainState: BrainState = BrainState.IDLE,
    onPetTap: () -> Unit,
    onPetLongPress: () -> Unit,
    onFeedPet: () -> Unit,
    onPlayWithPet: () -> Unit,
    onLetPetRest: () -> Unit,
    onMiniGameCelebrate: () -> Unit,
    onMiniGameFail: () -> Unit,
    onInvitationIgnored: () -> Unit,
    onNavigateToDebug: () -> Unit,
    onNavigateToDiary: () -> Unit
) {
    // -- Reaction controller (H4-04/05, H5, H6, H7) ---------------------------
    val reactionController = remember { PetReactionController() }
    LaunchedEffect(latestAudioOutputStartedAtMs) {
        if (latestAudioOutputStartedAtMs > 0L) {
            reactionController.recordAudioOutput(latestAudioOutputStartedAtMs)
        }
    }
    // -- Scene FX (H10) — declared early so all LaunchedEffects can reference it --
    var activeFx by remember { mutableStateOf<HomeFxType?>(null) }
    // H5: Greeting reaction � injected once when app-open greeting arrives
    LaunchedEffect(appOpenGreeting?.message) {
        if (isBehaviorExperienceAuthoritative) {
            return@LaunchedEffect
        }
        val greeting = appOpenGreeting ?: return@LaunchedEffect
        val isExcited = greeting.emotion == PetEmotion.EXCITED ||
                greeting.emotion == PetEmotion.HAPPY
        reactionController.triggerGreeting(excited = isExcited)
    }

    // H6: Tap / long-press reactions � immediate visual feedback before brain responds
    // (actual tap/longPress callbacks also call onPetTap/onPetLongPress below)

    // H7: Audio stimulus reactions + self-trigger guard + keyword FX (merged to avoid duplicate key)
    LaunchedEffect(latestAudioStimulus?.timestampMs) {
        if (isBehaviorExperienceAuthoritative) {
            return@LaunchedEffect
        }
        val stimulus = latestAudioStimulus ?: return@LaunchedEffect
        when (stimulus) {
            is KeywordStimulus -> {
                reactionController.triggerKeyword()
                if (activeFx == null) activeFx = HomeFxType.EXCLAMATION
            }
            is SoundStimulus -> if (stimulus.smoothedEnergy > LOUD_SOUND_ENERGY_THRESHOLD) {
                reactionController.triggerLoudSound()
            }
            else -> { /* VoiceActivityStimulus � no startle needed */ }
        }
    }

    // -- Talk bubble orchestrator ----------------------------------------------
    val activeBubble = rememberHomeTalkBubbleOrchestrator(
        behaviorTalkDirective = behaviorTalkDirective,
        appOpenGreeting = appOpenGreeting,
        feedbackMessage = homeInteractionUiState.feedbackMessage,
        feedbackToken = homeInteractionUiState.feedbackToken,
        conditions = homeUiModel.currentConditions
    )


    // FX from interaction feedback � keyed on feedbackToken so repeated same-message taps fire
    LaunchedEffect(homeInteractionUiState.feedbackToken) {
        if (homeInteractionUiState.feedbackMessage == null) return@LaunchedEffect
        activeFx = when {
            homeInteractionUiState.feedbackIsBlocked -> HomeFxType.EXCLAMATION
            homeUiModel.currentEmotion == PetEmotion.HAPPY ||
                    homeUiModel.currentEmotion == PetEmotion.EXCITED -> HomeFxType.HEARTS
            else -> HomeFxType.SPARKS
        }
    }

    // ZZZ FX on sleepy onset
    LaunchedEffect(homeUiModel.currentConditions) {
        if (PetCondition.SLEEPY in homeUiModel.currentConditions && activeFx == null) {
            activeFx = HomeFxType.ZZZ
        }
    }

    // Excited greeting FX (SPARKS/HEARTS burst on app open)
    LaunchedEffect(appOpenGreeting?.emotion) {
        val emotion = appOpenGreeting?.emotion ?: return@LaunchedEffect
        if (emotion == PetEmotion.EXCITED || emotion == PetEmotion.HAPPY) {
            activeFx = HomeFxType.SPARKS
        }
    }

    // -- Mini-game (H12) -------------------------------------------------------
    val sparkController = rememberSparkGameController(
        onWin = {
            activeFx = HomeFxType.HEARTS
            if (isBehaviorExperienceAuthoritative) {
                onMiniGameCelebrate()
            } else {
                reactionController.triggerGameCelebrate()
            }
        },
        onFail = {
            if (isBehaviorExperienceAuthoritative) {
                onMiniGameFail()
            } else {
                reactionController.triggerGameFail()
            }
        },
        onInviteIgnored = {
            onInvitationIgnored()
        }
    )

    // Invitation ownership is app-runtime driven. Home only renders invite state
    // when the runtime emits a new invitation trigger.
    LaunchedEffect(invitationTriggerToken) {
        if (invitationTriggerToken > 0L && sparkController.state.phase == SparkGamePhase.INACTIVE) {
            sparkController.startInvite()
        }
    }

    // Invitation bubble: show a contextual line when the pet is inviting
    LaunchedEffect(sparkController.state.phase) {
        if (sparkController.state.phase == SparkGamePhase.INVITE) {
            activeFx = HomeFxType.SPARKS
        }
    }

    // -- Menu sheet ------------------------------------------------------------
    var showMenuSheet by remember { mutableStateOf(false) }

    // -- Stage -----------------------------------------------------------------
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeColors.stageDark)
    ) {
        // H9: Ambient glow driven by emotion and need conditions
        HomeAmbientGlow(
            emotion = homeUiModel.currentEmotion,
            conditions = homeUiModel.currentConditions,
            modifier = Modifier.fillMaxSize()
        )

        // H2/H4: Face — centered, 300dp, floating idle bob via graphicsLayer in HomePixelPetAvatar
        HomePixelPetAvatar(
            bridgeState = avatarBridgeState,
            reactionController = reactionController,
            petState = petState,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-24).dp),
            displaySize = 300.dp,
            onTap = {
                if (sparkController.state.phase == SparkGamePhase.INVITE) {
                    // User accepted the pet's invitation — start game immediately
                    onPlayWithPet()
                    sparkController.acceptInvite()
                } else if (homeInteractionUiState.canTapPet) {
                    // H6-01: immediate visual reaction before brain processes
                    if (!isBehaviorExperienceAuthoritative) {
                        reactionController.triggerTap(isBlocked = false)
                    }
                    onPetTap()
                } else {
                    // H6-03: blocked tap anti-spam reaction
                    if (!isBehaviorExperienceAuthoritative) {
                        reactionController.triggerTap(isBlocked = true)
                    }
                }
            },
            onLongPress = {
                if (homeInteractionUiState.canLongPressPet) {
                    // H6-02: cuddle long-press reaction
                    if (!isBehaviorExperienceAuthoritative) {
                        reactionController.triggerLongPress()
                    }
                    onPetLongPress()
                }
            }
        )

        // H8: Talk bubble � positioned just below the face center
        HomeTalkBubble(
            bubble = activeBubble,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 180.dp)
                .padding(horizontal = 32.dp)
        )

        // H10: Scene FX overlay (hearts, sparks, zzz, exclamation)
        if (activeFx != null) {
            HomeFxOverlay(
                fxType = activeFx!!,
                modifier = Modifier.fillMaxSize(),
                onDone = { activeFx = null }
            )
        }

        // H12: Spark mini-game overlay (shown when INVITE, ACTIVE, WIN, or LOSE phase)
        if (sparkController.state.phase == SparkGamePhase.INVITE ||
            sparkController.state.isActive ||
            sparkController.state.phase == SparkGamePhase.WIN ||
            sparkController.state.phase == SparkGamePhase.LOSE
        ) {
            SparkGameOverlay(
                controller = sparkController,
                modifier = Modifier.fillMaxSize()
            )
        }

        // H11: Single visible control � compact menu button (top-right)
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 14.dp, end = 14.dp)
                .clip(RoundedCornerShape(10.dp)),
            color = HomeColors.sheetDark.copy(alpha = 0.6f),
            tonalElevation = 0.dp
        ) {
            IconButton(onClick = { showMenuSheet = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu",
                    tint = HomeColors.iconTint
                )
            }
        }
    }

    // H11: Menu sheet � rendered outside stage Box to cover full screen
    if (showMenuSheet) {
        Box(modifier = Modifier.fillMaxSize()) {
            HomeMenuSheet(
                onDismiss = { showMenuSheet = false },
                onDebug = {
                    showMenuSheet = false
                    onNavigateToDebug()
                },
                onDiary = {
                    showMenuSheet = false
                    onNavigateToDiary()
                },
                onFeedPet = {
                    showMenuSheet = false
                    onFeedPet()
                },
                onPlayWithPet = {
                    showMenuSheet = false
                    onPlayWithPet()
                    sparkController.startGame()
                },
                onLetPetRest = {
                    showMenuSheet = false
                    onLetPetRest()
                }
            )
        }
    }
}

// Energy threshold for loud-sound surprise reaction (empirically tuned for typical env noise)
private const val LOUD_SOUND_ENERGY_THRESHOLD = 0.55
