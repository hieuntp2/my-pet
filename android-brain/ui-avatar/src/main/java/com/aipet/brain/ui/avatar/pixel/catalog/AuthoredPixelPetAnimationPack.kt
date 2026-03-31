package com.aipet.brain.ui.avatar.pixel.catalog

import com.aipet.brain.ui.avatar.pixel.bridge.PixelAnimationSetRegistry
import com.aipet.brain.ui.avatar.pixel.model.Asking
import com.aipet.brain.ui.avatar.pixel.model.Curious
import com.aipet.brain.ui.avatar.pixel.model.Excited
import com.aipet.brain.ui.avatar.pixel.model.Happy
import com.aipet.brain.ui.avatar.pixel.model.Hungry
import com.aipet.brain.ui.avatar.pixel.model.Lonely
import com.aipet.brain.ui.avatar.pixel.model.Looking
import com.aipet.brain.ui.avatar.pixel.model.Neutral
import com.aipet.brain.ui.avatar.pixel.model.PixelAnimationClip
import com.aipet.brain.ui.avatar.pixel.model.PixelAnimationFrameEntry
import com.aipet.brain.ui.avatar.pixel.model.PixelAnimationPlaybackMode
import com.aipet.brain.ui.avatar.pixel.model.PixelAnimationVariant
import com.aipet.brain.ui.avatar.pixel.model.PixelAnimationVariantTier
import com.aipet.brain.ui.avatar.pixel.model.PixelFrame64
import com.aipet.brain.ui.avatar.pixel.model.PixelPetAnimationStateSet
import com.aipet.brain.ui.avatar.pixel.model.PixelPetDefaultPalette
import com.aipet.brain.ui.avatar.pixel.model.Sleepy
import com.aipet.brain.ui.avatar.pixel.model.Surprised
import com.aipet.brain.ui.avatar.pixel.model.Thinking

object AuthoredPixelPetAnimationPack {
    fun createRegistry(): PixelAnimationSetRegistry {
        return PixelAnimationSetRegistry(
            mapOf(
                Neutral to PixelPetAnimationStateSet(
                    state = Neutral,
                    variants = createNeutralVariants()
                ),
                Happy to PixelPetAnimationStateSet(
                    state = Happy,
                    variants = createHappyVariants()
                ),
                Curious to PixelPetAnimationStateSet(
                    state = Curious,
                    variants = createCuriousVariants()
                ),
                Looking to PixelPetAnimationStateSet(
                    state = Looking,
                    variants = createLookingVariants()
                ),
                Asking to PixelPetAnimationStateSet(
                    state = Asking,
                    variants = createAskingVariants()
                ),
                Thinking to PixelPetAnimationStateSet(
                    state = Thinking,
                    variants = createThinkingVariants()
                ),
                Excited to PixelPetAnimationStateSet(
                    state = Excited,
                    variants = createExcitedVariants()
                ),
                Surprised to PixelPetAnimationStateSet(
                    state = Surprised,
                    variants = createSurprisedVariants()
                ),
                Hungry to PixelPetAnimationStateSet(
                    state = Hungry,
                    variants = createHungryVariants()
                ),
                Sleepy to PixelPetAnimationStateSet(
                    state = Sleepy,
                    variants = createSleepyVariants()
                ),
                Lonely to PixelPetAnimationStateSet(
                    state = Lonely,
                    variants = createLonelyVariants()
                )
            )
        )
    }

    private fun createNeutralVariants(): List<PixelAnimationVariant> {
        return listOf(
            expressiveVariant(
                id = "Neutral_A_SlowBlink",
                frameEntries = listOf(
                    frame(durationMillis = 720, frame = NeutralEyeTemplate.openFrame()),
                    frame(durationMillis = 180, frame = NeutralEyeTemplate.softBlinkFrame()),
                    frame(durationMillis = 120, frame = NeutralEyeTemplate.closedBlinkFrame()),
                    frame(durationMillis = 210, frame = NeutralEyeTemplate.softBlinkFrame())
                ),
                categories = setOf("neutral")
            ),
            expressiveVariant(
                id = "Neutral_B_GlanceLeft",
                frameEntries = listOf(
                    frame(durationMillis = 420, frame = NeutralEyeTemplate.openFrame()),
                    frame(durationMillis = 220, frame = NeutralEyeTemplate.glanceLeftFrame()),
                    frame(durationMillis = 180, frame = NeutralEyeTemplate.glanceLeftHoldFrame()),
                    frame(durationMillis = 260, frame = NeutralEyeTemplate.openFrame())
                ),
                categories = setOf("neutral")
            ),
            expressiveVariant(
                id = "Neutral_C_GlanceRight",
                frameEntries = listOf(
                    frame(durationMillis = 390, frame = NeutralEyeTemplate.openFrame()),
                    frame(durationMillis = 240, frame = NeutralEyeTemplate.glanceRightFrame()),
                    frame(durationMillis = 210, frame = NeutralEyeTemplate.glanceRightHoldFrame()),
                    frame(durationMillis = 250, frame = NeutralEyeTemplate.openFrame())
                ),
                categories = setOf("neutral")
            ),
            expressiveVariant(
                id = "Neutral_D_DoubleBlink",
                tier = PixelAnimationVariantTier.RARE,
                frameEntries = listOf(
                    frame(durationMillis = 520, frame = NeutralEyeTemplate.openFrame()),
                    frame(durationMillis = 130, frame = NeutralEyeTemplate.softBlinkFrame()),
                    frame(durationMillis = 90, frame = NeutralEyeTemplate.closedBlinkFrame()),
                    frame(durationMillis = 120, frame = NeutralEyeTemplate.openFrame()),
                    frame(durationMillis = 110, frame = NeutralEyeTemplate.softBlinkFrame()),
                    frame(durationMillis = 80, frame = NeutralEyeTemplate.closedBlinkFrame()),
                    frame(durationMillis = 260, frame = NeutralEyeTemplate.openFrame())
                ),
                categories = setOf("neutral")
            )
        )
    }

    private fun createHappyVariants(): List<PixelAnimationVariant> {
        return listOf(
            expressiveVariant(
                id = "Happy_A_SoftSquint",
                frameEntries = listOf(
                    frame(durationMillis = 220, frame = HappyEyeTemplate.brightOpenFrame()),
                    frame(durationMillis = 240, frame = HappyEyeTemplate.softSquintFrame()),
                    frame(durationMillis = 180, frame = HappyEyeTemplate.warmSquintFrame()),
                    frame(durationMillis = 240, frame = HappyEyeTemplate.softSquintFrame())
                ),
                categories = setOf("happy")
            ),
            expressiveVariant(
                id = "Happy_B_OpenBounce",
                frameEntries = listOf(
                    frame(durationMillis = 220, frame = HappyEyeTemplate.brightOpenFrame()),
                    frame(durationMillis = 160, frame = HappyEyeTemplate.bounceLiftFrame()),
                    frame(durationMillis = 220, frame = HappyEyeTemplate.brightOpenFrame()),
                    frame(durationMillis = 260, frame = HappyEyeTemplate.softSquintFrame())
                ),
                categories = setOf("happy")
            ),
            expressiveVariant(
                id = "Happy_C_WinkAsymmetry",
                frameEntries = listOf(
                    frame(durationMillis = 220, frame = HappyEyeTemplate.brightOpenFrame()),
                    frame(durationMillis = 140, frame = HappyEyeTemplate.leftWinkFrame()),
                    frame(durationMillis = 150, frame = HappyEyeTemplate.rightEyeSmileFrame()),
                    frame(durationMillis = 240, frame = HappyEyeTemplate.softSquintFrame())
                ),
                categories = setOf("happy")
            ),
            expressiveVariant(
                id = "Happy_D_BrightOpen",
                tier = PixelAnimationVariantTier.RARE,
                frameEntries = listOf(
                    frame(durationMillis = 300, frame = HappyEyeTemplate.brightOpenFrame()),
                    frame(durationMillis = 180, frame = HappyEyeTemplate.bounceLiftFrame()),
                    frame(durationMillis = 280, frame = HappyEyeTemplate.brightOpenFrame())
                ),
                categories = setOf("happy")
            )
        )
    }

    private fun createCuriousVariants(): List<PixelAnimationVariant> {
        return listOf(
            expressiveVariant(
                id = "Curious_A_LookLeft",
                frameEntries = listOf(
                    frame(durationMillis = 180, frame = CuriousEyeTemplate.widenFrame()),
                    frame(durationMillis = 240, frame = CuriousEyeTemplate.lookLeftFrame()),
                    frame(durationMillis = 180, frame = CuriousEyeTemplate.lookLeftHoldFrame()),
                    frame(durationMillis = 220, frame = CuriousEyeTemplate.settleFrame())
                ),
                categories = setOf("curious")
            ),
            expressiveVariant(
                id = "Curious_B_LookRight",
                frameEntries = listOf(
                    frame(durationMillis = 220, frame = CuriousEyeTemplate.settleFrame()),
                    frame(durationMillis = 240, frame = CuriousEyeTemplate.lookRightFrame()),
                    frame(durationMillis = 180, frame = CuriousEyeTemplate.lookRightHoldFrame()),
                    frame(durationMillis = 220, frame = CuriousEyeTemplate.settleFrame())
                ),
                categories = setOf("curious")
            ),
            expressiveVariant(
                id = "Curious_C_FocusSquint",
                frameEntries = listOf(
                    frame(durationMillis = 260, frame = CuriousEyeTemplate.focusSquintFrame()),
                    frame(durationMillis = 180, frame = CuriousEyeTemplate.focusTightFrame()),
                    frame(durationMillis = 240, frame = CuriousEyeTemplate.focusSquintFrame())
                ),
                categories = setOf("curious")
            ),
            expressiveVariant(
                id = "Curious_D_WidenThenSettle",
                tier = PixelAnimationVariantTier.RARE,
                frameEntries = listOf(
                    frame(durationMillis = 190, frame = CuriousEyeTemplate.widenFrame()),
                    frame(durationMillis = 260, frame = CuriousEyeTemplate.settleFrame()),
                    frame(durationMillis = 260, frame = CuriousEyeTemplate.focusSquintFrame())
                ),
                categories = setOf("curious")
            )
        )
    }

    private fun createLookingVariants(): List<PixelAnimationVariant> {
        return listOf(
            expressiveVariant(
                id = "Looking_A_FocusLeft",
                frameEntries = listOf(
                    frame(durationMillis = 260, frame = LookingEyeTemplate.centerFrame()),
                    frame(durationMillis = 300, frame = LookingEyeTemplate.scanLeftFrame()),
                    frame(durationMillis = 220, frame = LookingEyeTemplate.holdLeftFrame()),
                    frame(durationMillis = 260, frame = LookingEyeTemplate.centerFrame())
                ),
                categories = setOf("looking")
            ),
            expressiveVariant(
                id = "Looking_B_FocusRight",
                frameEntries = listOf(
                    frame(durationMillis = 240, frame = LookingEyeTemplate.centerFrame()),
                    frame(durationMillis = 320, frame = LookingEyeTemplate.scanRightFrame()),
                    frame(durationMillis = 220, frame = LookingEyeTemplate.holdRightFrame()),
                    frame(durationMillis = 240, frame = LookingEyeTemplate.centerFrame())
                ),
                categories = setOf("looking")
            ),
            expressiveVariant(
                id = "Looking_C_TightObserve",
                frameEntries = listOf(
                    frame(durationMillis = 340, frame = LookingEyeTemplate.tightObserveFrame()),
                    frame(durationMillis = 220, frame = LookingEyeTemplate.pulseFrame()),
                    frame(durationMillis = 360, frame = LookingEyeTemplate.tightObserveFrame())
                ),
                categories = setOf("looking")
            )
        )
    }

    private fun createAskingVariants(): List<PixelAnimationVariant> {
        return listOf(
            expressiveVariant(
                id = "Asking_A_QuestionTiltLeft",
                frameEntries = listOf(
                    frame(durationMillis = 260, frame = AskingEyeTemplate.questionLeftFrame()),
                    frame(durationMillis = 280, frame = AskingEyeTemplate.questionPulseLeftFrame()),
                    frame(durationMillis = 260, frame = AskingEyeTemplate.questionLeftFrame())
                ),
                categories = setOf("asking")
            ),
            expressiveVariant(
                id = "Asking_B_QuestionTiltRight",
                frameEntries = listOf(
                    frame(durationMillis = 260, frame = AskingEyeTemplate.questionRightFrame()),
                    frame(durationMillis = 280, frame = AskingEyeTemplate.questionPulseRightFrame()),
                    frame(durationMillis = 260, frame = AskingEyeTemplate.questionRightFrame())
                ),
                categories = setOf("asking")
            ),
            expressiveVariant(
                id = "Asking_C_CuriousPause",
                tier = PixelAnimationVariantTier.RARE,
                frameEntries = listOf(
                    frame(durationMillis = 420, frame = AskingEyeTemplate.questionCenterFrame()),
                    frame(durationMillis = 260, frame = AskingEyeTemplate.questionPulseLeftFrame()),
                    frame(durationMillis = 420, frame = AskingEyeTemplate.questionCenterFrame())
                ),
                categories = setOf("asking")
            )
        )
    }

    private fun createSleepyVariants(): List<PixelAnimationVariant> {
        return listOf(
            expressiveVariant(
                id = "Sleepy_A_HalfLidLoop",
                frameEntries = listOf(
                    frame(durationMillis = 620, frame = SleepyEyeTemplate.halfLidFrame()),
                    frame(durationMillis = 280, frame = SleepyEyeTemplate.heavierHalfLidFrame()),
                    frame(durationMillis = 460, frame = SleepyEyeTemplate.halfLidFrame())
                ),
                categories = setOf("sleepy")
            ),
            expressiveVariant(
                id = "Sleepy_B_LongBlink",
                frameEntries = listOf(
                    frame(durationMillis = 340, frame = SleepyEyeTemplate.halfLidFrame()),
                    frame(durationMillis = 220, frame = SleepyEyeTemplate.preBlinkFrame()),
                    frame(durationMillis = 500, frame = SleepyEyeTemplate.closedBlinkFrame()),
                    frame(durationMillis = 360, frame = SleepyEyeTemplate.reopenFrame())
                ),
                categories = setOf("sleepy")
            ),
            expressiveVariant(
                id = "Sleepy_C_DroopDrift",
                frameEntries = listOf(
                    frame(durationMillis = 420, frame = SleepyEyeTemplate.leftDroopFrame()),
                    frame(durationMillis = 360, frame = SleepyEyeTemplate.centerDriftFrame()),
                    frame(durationMillis = 440, frame = SleepyEyeTemplate.rightDroopFrame()),
                    frame(durationMillis = 320, frame = SleepyEyeTemplate.centerDriftFrame())
                ),
                categories = setOf("sleepy")
            ),
            expressiveVariant(
                id = "Sleepy_D_StaggerClose",
                tier = PixelAnimationVariantTier.RARE,
                frameEntries = listOf(
                    frame(durationMillis = 320, frame = SleepyEyeTemplate.halfLidFrame()),
                    frame(durationMillis = 260, frame = SleepyEyeTemplate.leftClosedFrame()),
                    frame(durationMillis = 240, frame = SleepyEyeTemplate.rightHeavyFrame()),
                    frame(durationMillis = 540, frame = SleepyEyeTemplate.bothClosedRestFrame()),
                    frame(durationMillis = 360, frame = SleepyEyeTemplate.reopenFrame())
                ),
                categories = setOf("sleepy")
            ),
            // E: Near-doze micro-event — lids sink to almost-closed slit, hold, flutter reopen.
            // Reads as the pet fighting sleep: visible 30-second variation anchor.
            expressiveVariant(
                id = "Sleepy_E_NearDoze",
                frameEntries = listOf(
                    frame(durationMillis = 380, frame = SleepyEyeTemplate.halfLidFrame()),
                    frame(durationMillis = 300, frame = SleepyEyeTemplate.heavierHalfLidFrame()),
                    frame(durationMillis = 420, frame = SleepyEyeTemplate.nearDozeFrame()),
                    frame(durationMillis = 680, frame = SleepyEyeTemplate.bothClosedRestFrame()),
                    frame(durationMillis = 200, frame = SleepyEyeTemplate.flutterOpenFrame()),
                    frame(durationMillis = 260, frame = SleepyEyeTemplate.nearDozeFrame()),
                    frame(durationMillis = 480, frame = SleepyEyeTemplate.reopenFrame())
                ),
                categories = setOf("sleepy")
            ),
            // F: Wake-correction flutter — brief snap-open as if startled awake, then sinks
            // back down into the heavy half-lid. Provides the "fighting sleep" micro-event.
            expressiveVariant(
                id = "Sleepy_F_WakeCorrection",
                tier = PixelAnimationVariantTier.RARE,
                frameEntries = listOf(
                    frame(durationMillis = 260, frame = SleepyEyeTemplate.nearDozeFrame()),
                    frame(durationMillis = 120, frame = SleepyEyeTemplate.flutterOpenFrame()),
                    frame(durationMillis = 180, frame = SleepyEyeTemplate.halfLidFrame()),
                    frame(durationMillis = 340, frame = SleepyEyeTemplate.heavierHalfLidFrame()),
                    frame(durationMillis = 400, frame = SleepyEyeTemplate.halfLidFrame())
                ),
                categories = setOf("sleepy")
            )
        )
    }

    private fun createExcitedVariants(): List<PixelAnimationVariant> {
        return listOf(
            // Anticipation -> wide open burst -> happy bounce -> settle: used for greeting/win
            reactionVariant(
                id = "Excited_A_GreetingBurst",
                frameEntries = listOf(
                    frame(durationMillis = 100, frame = ExcitedEyeTemplate.anticipateFrame()),
                    frame(durationMillis = 180, frame = ExcitedEyeTemplate.wideOpenFrame()),
                    frame(durationMillis = 200, frame = ExcitedEyeTemplate.brightBurstFrame()),
                    frame(durationMillis = 140, frame = ExcitedEyeTemplate.bounceFrame()),
                    frame(durationMillis = 220, frame = ExcitedEyeTemplate.settleFrame())
                ),
                categories = setOf("excited")
            ),
            // Lighter variant: quick widen + squint settle
            reactionVariant(
                id = "Excited_B_QuickJoy",
                frameEntries = listOf(
                    frame(durationMillis = 120, frame = ExcitedEyeTemplate.wideOpenFrame()),
                    frame(durationMillis = 240, frame = ExcitedEyeTemplate.brightBurstFrame()),
                    frame(durationMillis = 200, frame = ExcitedEyeTemplate.settleFrame())
                ),
                categories = setOf("excited")
            )
        )
    }

    private fun createSurprisedVariants(): List<PixelAnimationVariant> {
        return listOf(
            // Snap-wide freeze + recover: used for loud sound / spam overstimulation
            reactionVariant(
                id = "Surprised_A_SnapFreeze",
                frameEntries = listOf(
                    frame(durationMillis = 80,  frame = SurprisedEyeTemplate.snapWideFrame()),
                    frame(durationMillis = 420, frame = SurprisedEyeTemplate.freezeFrame()),
                    frame(durationMillis = 160, frame = SurprisedEyeTemplate.recoverFrame()),
                    frame(durationMillis = 180, frame = SurprisedEyeTemplate.settleFrame())
                ),
                categories = setOf("surprised")
            ),
            // Lighter blink-startle for keyword detection
            reactionVariant(
                id = "Surprised_B_StartleBlink",
                frameEntries = listOf(
                    frame(durationMillis = 90,  frame = SurprisedEyeTemplate.snapWideFrame()),
                    frame(durationMillis = 180, frame = SurprisedEyeTemplate.freezeFrame()),
                    frame(durationMillis = 200, frame = SurprisedEyeTemplate.settleFrame())
                ),
                categories = setOf("surprised")
            )
        )
    }

    private fun createHungryVariants(): List<PixelAnimationVariant> {
        return listOf(
            // Primary ambient loop: droopy attentive eyes + side scan for food
            expressiveVariant(
                id = "Hungry_A_SearchScan",
                frameEntries = listOf(
                    frame(durationMillis = 480, frame = HungryEyeTemplate.attentiveHungerFrame()),
                    frame(durationMillis = 320, frame = HungryEyeTemplate.scanLeftFrame()),
                    frame(durationMillis = 240, frame = HungryEyeTemplate.holdLeftFrame()),
                    frame(durationMillis = 360, frame = HungryEyeTemplate.attentiveHungerFrame()),
                    frame(durationMillis = 300, frame = HungryEyeTemplate.scanRightFrame()),
                    frame(durationMillis = 240, frame = HungryEyeTemplate.holdRightFrame()),
                    frame(durationMillis = 400, frame = HungryEyeTemplate.attentiveHungerFrame())
                ),
                categories = setOf("hungry")
            ),
            // Secondary: slow longing blink with concerned brows
            expressiveVariant(
                id = "Hungry_B_LongingBlink",
                frameEntries = listOf(
                    frame(durationMillis = 560, frame = HungryEyeTemplate.attentiveHungerFrame()),
                    frame(durationMillis = 180, frame = HungryEyeTemplate.preBlinkFrame()),
                    frame(durationMillis = 440, frame = HungryEyeTemplate.closedBlinkFrame()),
                    frame(durationMillis = 280, frame = HungryEyeTemplate.attentiveHungerFrame())
                ),
                categories = setOf("hungry")
            )
        )
    }

    private fun createThinkingVariants(): List<PixelAnimationVariant> {
        return listOf(
            expressiveVariant(
                id = "Thinking_A_SideHold",
                frameEntries = listOf(
                    frame(durationMillis = 280, frame = ThinkingEyeTemplate.centerFocusFrame()),
                    frame(durationMillis = 520, frame = ThinkingEyeTemplate.sideHoldLeftFrame()),
                    frame(durationMillis = 300, frame = ThinkingEyeTemplate.sideHoldReturnFrame())
                ),
                categories = setOf("thinking")
            ),
            expressiveVariant(
                id = "Thinking_B_AlternatingSquint",
                frameEntries = listOf(
                    frame(durationMillis = 340, frame = ThinkingEyeTemplate.leftSquintFrame()),
                    frame(durationMillis = 360, frame = ThinkingEyeTemplate.centerFocusFrame()),
                    frame(durationMillis = 340, frame = ThinkingEyeTemplate.rightSquintFrame()),
                    frame(durationMillis = 360, frame = ThinkingEyeTemplate.centerFocusFrame())
                ),
                categories = setOf("thinking")
            ),
            expressiveVariant(
                id = "Thinking_C_FocusPulse",
                frameEntries = listOf(
                    frame(durationMillis = 360, frame = ThinkingEyeTemplate.focusPulseFrame()),
                    frame(durationMillis = 220, frame = ThinkingEyeTemplate.tightFocusFrame()),
                    frame(durationMillis = 420, frame = ThinkingEyeTemplate.focusPulseFrame())
                ),
                categories = setOf("thinking")
            ),
            expressiveVariant(
                id = "Thinking_D_CenteredStillness",
                tier = PixelAnimationVariantTier.RARE,
                frameEntries = listOf(
                    frame(durationMillis = 760, frame = ThinkingEyeTemplate.centerStillFrame()),
                    frame(durationMillis = 260, frame = ThinkingEyeTemplate.tightFocusFrame()),
                    frame(durationMillis = 680, frame = ThinkingEyeTemplate.centerStillFrame())
                ),
                categories = setOf("thinking")
            )
        )
    }

    /**
     * Lonely ambient loop family. Visually distinct from Sleepy (no heavy lids, no drift),
     * distinct from Sad (no withdrawn downcast). Reads as: expectant, center-facing, slightly
     * tentative — wanting connection. Gaze behavior cycles: hold center → brief glance away →
     * return. Blink timing is slower and more deliberate than neutral.
     */
    private fun createLonelyVariants(): List<PixelAnimationVariant> {
        return listOf(
            // A: Expectant center hold — long open gaze, slow blink, return to center.
            // Primary loop: communicates "waiting, watching, hoping".
            expressiveVariant(
                id = "Lonely_A_ExpectantHold",
                frameEntries = listOf(
                    frame(durationMillis = 720, frame = LonelyEyeTemplate.expectantCenterFrame()),
                    frame(durationMillis = 200, frame = LonelyEyeTemplate.softBlinkBuildFrame()),
                    frame(durationMillis = 380, frame = LonelyEyeTemplate.slowBlinkClosedFrame()),
                    frame(durationMillis = 320, frame = LonelyEyeTemplate.reopenExpectantFrame()),
                    frame(durationMillis = 560, frame = LonelyEyeTemplate.expectantCenterFrame())
                ),
                categories = setOf("lonely")
            ),
            // B: Glance right and return — brief look-away, slow return to center.
            // Reads as: "looking for someone to come, then hoping again".
            expressiveVariant(
                id = "Lonely_B_GlanceAwayReturn",
                frameEntries = listOf(
                    frame(durationMillis = 480, frame = LonelyEyeTemplate.expectantCenterFrame()),
                    frame(durationMillis = 280, frame = LonelyEyeTemplate.glanceRightFrame()),
                    frame(durationMillis = 380, frame = LonelyEyeTemplate.holdGlanceFrame()),
                    frame(durationMillis = 260, frame = LonelyEyeTemplate.glanceReturnFrame()),
                    frame(durationMillis = 600, frame = LonelyEyeTemplate.expectantCenterFrame())
                ),
                categories = setOf("lonely")
            ),
            // C: Glance left and return — opposite direction variant.
            // Alternates with B to prevent robotic left-only behavior.
            expressiveVariant(
                id = "Lonely_C_GlanceLookReturn",
                frameEntries = listOf(
                    frame(durationMillis = 440, frame = LonelyEyeTemplate.expectantCenterFrame()),
                    frame(durationMillis = 300, frame = LonelyEyeTemplate.glanceLeftFrame()),
                    frame(durationMillis = 360, frame = LonelyEyeTemplate.holdGlanceLeftFrame()),
                    frame(durationMillis = 240, frame = LonelyEyeTemplate.glanceReturnFrame()),
                    frame(durationMillis = 580, frame = LonelyEyeTemplate.expectantCenterFrame())
                ),
                categories = setOf("lonely")
            ),
            // D: Tentative attention pulse — lids dip slightly then open again, like hesitation.
            // RARE. Reads as: "almost said something, then didn't". Clingy hesitancy beat.
            expressiveVariant(
                id = "Lonely_D_TentativePulse",
                tier = PixelAnimationVariantTier.RARE,
                frameEntries = listOf(
                    frame(durationMillis = 500, frame = LonelyEyeTemplate.expectantCenterFrame()),
                    frame(durationMillis = 220, frame = LonelyEyeTemplate.tentativeDipFrame()),
                    frame(durationMillis = 180, frame = LonelyEyeTemplate.reopenExpectantFrame()),
                    frame(durationMillis = 420, frame = LonelyEyeTemplate.expectantCenterFrame()),
                    frame(durationMillis = 200, frame = LonelyEyeTemplate.tentativeDipFrame()),
                    frame(durationMillis = 580, frame = LonelyEyeTemplate.expectantCenterFrame())
                ),
                categories = setOf("lonely")
            )
        )
    }

    private fun frame(
        durationMillis: Int,
        frame: PixelFrame64
    ): PixelAnimationFrameEntry {
        return PixelAnimationFrameEntry(
            frame = frame,
            durationMillis = durationMillis
        )
    }

    /** Looping ambient variant — used for idle state families. */
    private fun expressiveVariant(
        id: String,
        frameEntries: List<PixelAnimationFrameEntry>,
        categories: Set<String>,
        tier: PixelAnimationVariantTier = PixelAnimationVariantTier.PRIMARY
    ): PixelAnimationVariant {
        return PixelAnimationVariant(
            id = id,
            clip = PixelAnimationClip(
                id = "${id}_clip",
                playbackMode = PixelAnimationPlaybackMode.LOOP,
                frames = frameEntries
            ),
            tier = tier,
            weight = 1,
            categories = categories
        )
    }

    /** One-shot reaction variant — plays once then the orchestrator picks a new clip. */
    private fun reactionVariant(
        id: String,
        frameEntries: List<PixelAnimationFrameEntry>,
        categories: Set<String>
    ): PixelAnimationVariant {
        return PixelAnimationVariant(
            id = id,
            clip = PixelAnimationClip(
                id = "${id}_clip",
                playbackMode = PixelAnimationPlaybackMode.ONE_SHOT,
                frames = frameEntries
            ),
            tier = PixelAnimationVariantTier.PRIMARY,
            weight = 1,
            categories = categories
        )
    }

    private object NeutralEyeTemplate {
        fun openFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame()

        fun softBlinkFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftTopLidRows = 4,
            rightTopLidRows = 4,
            leftBottomLidRows = 4,
            rightBottomLidRows = 4
        )

        fun closedBlinkFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftClosedSlit = true,
            rightClosedSlit = true
        )

        fun glanceLeftFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = -2,
            rightPupilOffset = -2
        )

        fun glanceLeftHoldFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = -1,
            rightPupilOffset = -2,
            leftTopLidRows = 1,
            rightTopLidRows = 1
        )

        fun glanceRightFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = 2,
            rightPupilOffset = 2
        )

        fun glanceRightHoldFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = 2,
            rightPupilOffset = 1,
            leftTopLidRows = 1,
            rightTopLidRows = 1
        )
    }

    private object HappyEyeTemplate {
        // Happy reads as friendly warmth through bright highlights and gentle eyelid compression.
        fun softSquintFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftTopLidRows = 2,
            rightTopLidRows = 2,
            leftBottomLidRows = 2,
            rightBottomLidRows = 2,
            eyebrowLeftYOffset = 1,
            eyebrowRightYOffset = 1,
            eyebrowLeftColor = CanonicalEyeRenderer.highlight,
            eyebrowRightColor = CanonicalEyeRenderer.highlight,
            extraHighlight = true
        )

        fun warmSquintFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftTopLidRows = 3,
            rightTopLidRows = 3,
            leftBottomLidRows = 2,
            rightBottomLidRows = 2,
            eyebrowLeftYOffset = 1,
            eyebrowRightYOffset = 1,
            eyebrowLeftColor = CanonicalEyeRenderer.highlight,
            eyebrowRightColor = CanonicalEyeRenderer.highlight,
            extraHighlight = true
        )

        fun brightOpenFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            eyebrowLeftYOffset = -1,
            eyebrowRightYOffset = -1,
            eyebrowLeftColor = CanonicalEyeRenderer.highlight,
            eyebrowRightColor = CanonicalEyeRenderer.highlight,
            extraHighlight = true
        )

        fun bounceLiftFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = 1,
            rightPupilOffset = -1,
            eyebrowLeftYOffset = -1,
            eyebrowRightYOffset = -1,
            eyebrowLeftColor = CanonicalEyeRenderer.highlight,
            eyebrowRightColor = CanonicalEyeRenderer.highlight,
            extraHighlight = true
        )

        fun leftWinkFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftClosedSlit = true,
            rightTopLidRows = 2,
            rightBottomLidRows = 1,
            eyebrowLeftYOffset = 1,
            eyebrowRightYOffset = 1,
            eyebrowLeftColor = CanonicalEyeRenderer.highlight,
            eyebrowRightColor = CanonicalEyeRenderer.highlight,
            extraHighlight = true
        )

        fun rightEyeSmileFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftTopLidRows = 2,
            leftBottomLidRows = 1,
            rightTopLidRows = 1,
            rightBottomLidRows = 1,
            eyebrowLeftYOffset = 1,
            eyebrowRightYOffset = 1,
            eyebrowLeftColor = CanonicalEyeRenderer.highlight,
            eyebrowRightColor = CanonicalEyeRenderer.highlight,
            extraHighlight = true
        )
    }

    private object CuriousEyeTemplate {
        // Curious stays subtle: tighter lids, directed gaze, and slight brow asymmetry rather than big motion.
        fun settleFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftTopLidRows = 1,
            rightTopLidRows = 1,
            eyebrowLeftYOffset = -1,
            eyebrowRightYOffset = -1
        )

        fun lookLeftFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = -2,
            rightPupilOffset = -2,
            leftTopLidRows = 1,
            rightTopLidRows = 2,
            eyebrowLeftYOffset = -1,
            eyebrowRightYOffset = 0
        )

        fun lookLeftHoldFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = -2,
            rightPupilOffset = -1,
            leftTopLidRows = 2,
            rightTopLidRows = 2,
            eyebrowLeftYOffset = -1,
            eyebrowRightYOffset = 0
        )

        fun lookRightFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = 2,
            rightPupilOffset = 2,
            leftTopLidRows = 2,
            rightTopLidRows = 1,
            eyebrowLeftYOffset = 0,
            eyebrowRightYOffset = -1
        )

        fun lookRightHoldFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = 1,
            rightPupilOffset = 2,
            leftTopLidRows = 2,
            rightTopLidRows = 2,
            eyebrowLeftYOffset = 0,
            eyebrowRightYOffset = -1
        )

        fun focusSquintFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = 1,
            rightPupilOffset = -1,
            leftTopLidRows = 2,
            rightTopLidRows = 2,
            leftBottomLidRows = 1,
            rightBottomLidRows = 1,
            eyebrowLeftYOffset = 0,
            eyebrowRightYOffset = 0
        )

        fun focusTightFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = 1,
            rightPupilOffset = -1,
            leftTopLidRows = 3,
            rightTopLidRows = 3,
            leftBottomLidRows = 1,
            rightBottomLidRows = 1,
            eyebrowLeftYOffset = 1,
            eyebrowRightYOffset = 1
        )

        fun widenFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            eyebrowLeftYOffset = -2,
            eyebrowRightYOffset = -2,
            extraHighlight = true
        )
    }

    private object LookingEyeTemplate {
        fun centerFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftTopLidRows = 2,
            rightTopLidRows = 2,
            leftBottomLidRows = 1,
            rightBottomLidRows = 1,
            eyebrowLeftYOffset = -1,
            eyebrowRightYOffset = -1
        )

        fun scanLeftFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = -2,
            rightPupilOffset = -2,
            leftTopLidRows = 2,
            rightTopLidRows = 3,
            leftBottomLidRows = 1,
            rightBottomLidRows = 1,
            eyebrowLeftYOffset = 0,
            eyebrowRightYOffset = -1
        )

        fun holdLeftFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = -2,
            rightPupilOffset = -1,
            leftTopLidRows = 3,
            rightTopLidRows = 3,
            leftBottomLidRows = 1,
            rightBottomLidRows = 1,
            eyebrowLeftYOffset = 0,
            eyebrowRightYOffset = 0
        )

        fun scanRightFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = 2,
            rightPupilOffset = 2,
            leftTopLidRows = 3,
            rightTopLidRows = 2,
            leftBottomLidRows = 1,
            rightBottomLidRows = 1,
            eyebrowLeftYOffset = -1,
            eyebrowRightYOffset = 0
        )

        fun holdRightFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = 1,
            rightPupilOffset = 2,
            leftTopLidRows = 3,
            rightTopLidRows = 3,
            leftBottomLidRows = 1,
            rightBottomLidRows = 1,
            eyebrowLeftYOffset = 0,
            eyebrowRightYOffset = 0
        )

        fun tightObserveFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftTopLidRows = 4,
            rightTopLidRows = 4,
            leftBottomLidRows = 1,
            rightBottomLidRows = 1,
            eyebrowLeftYOffset = 0,
            eyebrowRightYOffset = 0
        )

        fun pulseFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftTopLidRows = 3,
            rightTopLidRows = 3,
            leftBottomLidRows = 1,
            rightBottomLidRows = 1,
            eyebrowLeftYOffset = -1,
            eyebrowRightYOffset = -1,
            extraHighlight = true
        )
    }

    private object AskingEyeTemplate {
        fun questionCenterFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftTopLidRows = 2,
            rightTopLidRows = 2,
            eyebrowLeftYOffset = -1,
            eyebrowRightYOffset = 0,
            questionMarkMode = QuestionMarkMode.CENTER
        )

        fun questionLeftFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = -1,
            rightPupilOffset = 1,
            leftTopLidRows = 2,
            rightTopLidRows = 3,
            eyebrowLeftYOffset = -1,
            eyebrowRightYOffset = 0,
            questionMarkMode = QuestionMarkMode.LEFT
        )

        fun questionPulseLeftFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = -1,
            rightPupilOffset = 1,
            leftTopLidRows = 3,
            rightTopLidRows = 3,
            eyebrowLeftYOffset = -1,
            eyebrowRightYOffset = 0,
            extraHighlight = true,
            questionMarkMode = QuestionMarkMode.LEFT
        )

        fun questionRightFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = 1,
            rightPupilOffset = -1,
            leftTopLidRows = 3,
            rightTopLidRows = 2,
            eyebrowLeftYOffset = 0,
            eyebrowRightYOffset = -1,
            questionMarkMode = QuestionMarkMode.RIGHT
        )

        fun questionPulseRightFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = 1,
            rightPupilOffset = -1,
            leftTopLidRows = 3,
            rightTopLidRows = 3,
            eyebrowLeftYOffset = 0,
            eyebrowRightYOffset = -1,
            extraHighlight = true,
            questionMarkMode = QuestionMarkMode.RIGHT
        )
    }

    private object SleepyEyeTemplate {
        fun halfLidFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftTopLidRows = 4,
            rightTopLidRows = 4,
            leftBottomLidRows = 2,
            rightBottomLidRows = 2,
            eyebrowLeftYOffset = 1,
            eyebrowRightYOffset = 1
        )

        fun heavierHalfLidFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftTopLidRows = 5,
            rightTopLidRows = 5,
            leftBottomLidRows = 3,
            rightBottomLidRows = 3,
            eyebrowLeftYOffset = 2,
            eyebrowRightYOffset = 2
        )

        fun preBlinkFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftTopLidRows = 6,
            rightTopLidRows = 6,
            leftBottomLidRows = 4,
            rightBottomLidRows = 4,
            eyebrowLeftYOffset = 2,
            eyebrowRightYOffset = 2
        )

        fun closedBlinkFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftClosedSlit = true,
            rightClosedSlit = true,
            eyebrowLeftYOffset = 2,
            eyebrowRightYOffset = 2
        )

        fun reopenFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftTopLidRows = 4,
            rightTopLidRows = 4,
            leftBottomLidRows = 1,
            rightBottomLidRows = 1,
            eyebrowLeftYOffset = 1,
            eyebrowRightYOffset = 1
        )

        fun leftDroopFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = -1,
            rightPupilOffset = -1,
            leftTopLidRows = 5,
            rightTopLidRows = 4,
            leftBottomLidRows = 2,
            rightBottomLidRows = 2,
            eyebrowLeftYOffset = 2,
            eyebrowRightYOffset = 1
        )

        fun centerDriftFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftTopLidRows = 5,
            rightTopLidRows = 5,
            leftBottomLidRows = 2,
            rightBottomLidRows = 2,
            eyebrowLeftYOffset = 2,
            eyebrowRightYOffset = 2
        )

        fun rightDroopFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = 1,
            rightPupilOffset = 1,
            leftTopLidRows = 4,
            rightTopLidRows = 5,
            leftBottomLidRows = 2,
            rightBottomLidRows = 2,
            eyebrowLeftYOffset = 1,
            eyebrowRightYOffset = 2
        )

        fun leftClosedFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftClosedSlit = true,
            rightTopLidRows = 5,
            rightBottomLidRows = 3,
            eyebrowLeftYOffset = 2,
            eyebrowRightYOffset = 2
        )

        fun rightHeavyFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftTopLidRows = 4,
            leftBottomLidRows = 2,
            rightTopLidRows = 6,
            rightBottomLidRows = 4,
            eyebrowLeftYOffset = 1,
            eyebrowRightYOffset = 2
        )

        fun bothClosedRestFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftClosedSlit = true,
            rightClosedSlit = true,
            eyebrowLeftYOffset = 3,
            eyebrowRightYOffset = 3
        )

        // Near-doze: eyes almost entirely closed but not slit — a narrow gap remains.
        // Distinct from bothClosedRest (slit) — this is "barely visible opening".
        fun nearDozeFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftTopLidRows = 6,
            rightTopLidRows = 6,
            leftBottomLidRows = 4,
            rightBottomLidRows = 4,
            eyebrowLeftYOffset = 3,
            eyebrowRightYOffset = 3
        )

        // Flutter-open: brief involuntary reopen after near-doze, before sinking back.
        // Top lids suddenly lift to half-lid position — brows drop back to normal-sleepy.
        fun flutterOpenFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftTopLidRows = 3,
            rightTopLidRows = 3,
            leftBottomLidRows = 1,
            rightBottomLidRows = 1,
            eyebrowLeftYOffset = 1,
            eyebrowRightYOffset = 1
        )
    }

    private object ThinkingEyeTemplate {
        fun centerFocusFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftTopLidRows = 2,
            rightTopLidRows = 2,
            leftBottomLidRows = 1,
            rightBottomLidRows = 1,
            eyebrowLeftYOffset = 0,
            eyebrowRightYOffset = 0
        )

        fun sideHoldLeftFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = -2,
            rightPupilOffset = -2,
            leftTopLidRows = 2,
            rightTopLidRows = 3,
            leftBottomLidRows = 1,
            rightBottomLidRows = 1,
            eyebrowLeftYOffset = 0,
            eyebrowRightYOffset = 1
        )

        fun sideHoldReturnFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = -1,
            rightPupilOffset = -1,
            leftTopLidRows = 2,
            rightTopLidRows = 2,
            leftBottomLidRows = 1,
            rightBottomLidRows = 1,
            eyebrowLeftYOffset = 0,
            eyebrowRightYOffset = 0
        )

        fun leftSquintFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = -1,
            rightPupilOffset = 1,
            leftTopLidRows = 4,
            rightTopLidRows = 2,
            leftBottomLidRows = 2,
            rightBottomLidRows = 1,
            eyebrowLeftYOffset = 1,
            eyebrowRightYOffset = 0
        )

        fun rightSquintFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = 1,
            rightPupilOffset = -1,
            leftTopLidRows = 2,
            rightTopLidRows = 4,
            leftBottomLidRows = 1,
            rightBottomLidRows = 2,
            eyebrowLeftYOffset = 0,
            eyebrowRightYOffset = 1
        )

        fun focusPulseFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftTopLidRows = 3,
            rightTopLidRows = 3,
            leftBottomLidRows = 1,
            rightBottomLidRows = 1,
            eyebrowLeftYOffset = 0,
            eyebrowRightYOffset = 0,
            extraHighlight = true
        )

        fun tightFocusFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftTopLidRows = 4,
            rightTopLidRows = 4,
            leftBottomLidRows = 1,
            rightBottomLidRows = 1,
            eyebrowLeftYOffset = 1,
            eyebrowRightYOffset = 1
        )

        fun centerStillFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftTopLidRows = 3,
            rightTopLidRows = 3,
            leftBottomLidRows = 1,
            rightBottomLidRows = 1,
            eyebrowLeftYOffset = 1,
            eyebrowRightYOffset = 1
        )
    }

    /**
     * Excited reaction eye templates — used for ONE_SHOT greeting/win clips.
     * Theme: wide open, raised brows, extra highlight, then settle to squint.
     */
    private object ExcitedEyeTemplate {
        // Slight anticipation: brows raised, eyes slightly open extra
        fun anticipateFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            eyebrowLeftYOffset = -2,
            eyebrowRightYOffset = -2,
            eyebrowLeftColor = CanonicalEyeRenderer.highlight,
            eyebrowRightColor = CanonicalEyeRenderer.highlight
        )

        // Action: fully wide open with raised brows + extra highlight
        fun wideOpenFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            eyebrowLeftYOffset = -2,
            eyebrowRightYOffset = -2,
            eyebrowLeftColor = CanonicalEyeRenderer.highlight,
            eyebrowRightColor = CanonicalEyeRenderer.highlight,
            extraHighlight = true
        )

        // Peak burst: pupils slightly asymmetric bounce feel
        fun brightBurstFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = 1,
            rightPupilOffset = -1,
            eyebrowLeftYOffset = -2,
            eyebrowRightYOffset = -2,
            eyebrowLeftColor = CanonicalEyeRenderer.highlight,
            eyebrowRightColor = CanonicalEyeRenderer.highlight,
            extraHighlight = true
        )

        // Bounce: pupils back to symmetric, brows still high
        fun bounceFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            eyebrowLeftYOffset = -1,
            eyebrowRightYOffset = -1,
            eyebrowLeftColor = CanonicalEyeRenderer.highlight,
            eyebrowRightColor = CanonicalEyeRenderer.highlight,
            extraHighlight = true
        )

        // Settle: happy squint — warm landing
        fun settleFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftTopLidRows = 2,
            rightTopLidRows = 2,
            leftBottomLidRows = 1,
            rightBottomLidRows = 1,
            eyebrowLeftYOffset = 1,
            eyebrowRightYOffset = 1,
            eyebrowLeftColor = CanonicalEyeRenderer.highlight,
            eyebrowRightColor = CanonicalEyeRenderer.highlight,
            extraHighlight = true
        )
    }

    /**
     * Surprised reaction eye templates — used for ONE_SHOT startle/overstimulated clips.
     * Theme: instant snap-wide, freeze, slow recover.
     */
    private object SurprisedEyeTemplate {
        // Instant snap: widest possible open, brows way up
        fun snapWideFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            eyebrowLeftYOffset = -3,
            eyebrowRightYOffset = -3,
            eyebrowLeftColor = CanonicalEyeRenderer.highlight,
            eyebrowRightColor = CanonicalEyeRenderer.highlight,
            extraHighlight = true
        )

        // Freeze hold: same wide but without extra highlight (less intense)
        fun freezeFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            eyebrowLeftYOffset = -2,
            eyebrowRightYOffset = -2
        )

        // Begin recovery: lids drop slightly
        fun recoverFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftTopLidRows = 1,
            rightTopLidRows = 1,
            eyebrowLeftYOffset = -1,
            eyebrowRightYOffset = -1
        )

        // Final settle: back to neutral open
        fun settleFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame()
    }

    /**
     * Hungry ambient eye templates — used for LOOP ambient state when pet is hungry.
     * Theme: droopy-attentive (concerned brows), side scan searching for food.
     */
    private object HungryEyeTemplate {
        // Base idle: slightly drooped, concerned brows
        fun attentiveHungerFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftTopLidRows = 2,
            rightTopLidRows = 2,
            eyebrowLeftYOffset = 1,
            eyebrowRightYOffset = 1
        )

        // Scan left — searching
        fun scanLeftFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = -2,
            rightPupilOffset = -2,
            leftTopLidRows = 2,
            rightTopLidRows = 3,
            eyebrowLeftYOffset = 0,
            eyebrowRightYOffset = 1
        )

        // Hold left gaze
        fun holdLeftFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = -2,
            rightPupilOffset = -1,
            leftTopLidRows = 2,
            rightTopLidRows = 3,
            eyebrowLeftYOffset = 0,
            eyebrowRightYOffset = 1
        )

        // Scan right — searching
        fun scanRightFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = 2,
            rightPupilOffset = 2,
            leftTopLidRows = 3,
            rightTopLidRows = 2,
            eyebrowLeftYOffset = 1,
            eyebrowRightYOffset = 0
        )

        // Hold right gaze
        fun holdRightFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = 1,
            rightPupilOffset = 2,
            leftTopLidRows = 3,
            rightTopLidRows = 2,
            eyebrowLeftYOffset = 1,
            eyebrowRightYOffset = 0
        )

        // Pre-blink droop
        fun preBlinkFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftTopLidRows = 3,
            rightTopLidRows = 3,
            leftBottomLidRows = 1,
            rightBottomLidRows = 1,
            eyebrowLeftYOffset = 1,
            eyebrowRightYOffset = 1
        )

        // Closed blink
        fun closedBlinkFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftClosedSlit = true,
            rightClosedSlit = true,
            eyebrowLeftYOffset = 1,
            eyebrowRightYOffset = 1
        )
    }

    /**
     * Lonely ambient eye templates.
     *
     * Visual identity: open, center-facing, with softly worried brows (+1 offset).
     * No heavy lids (unlike Sleepy). No withdrawn downcast (unlike Sad).
     * Gaze lives at 0 to ±1 — never aggressive scan. Blinks are slow and deliberate.
     * Glances are brief and return to center expectantly.
     *
     * Key distinction from Sleepy: pupils are centered, lids are mostly open (1-2 rows only).
     * Key distinction from Sad: brows are soft-worried (+1), not deeply furrowed (+2/+3).
     *                           Eyes remain open and forward-facing, not downcast/withdrawn.
     */
    private object LonelyEyeTemplate {
        // Base expectant frame: open eyes, soft worried brow, pupils perfectly centered.
        // The fundamental lonely "look" — present and waiting.
        fun expectantCenterFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftTopLidRows = 1,
            rightTopLidRows = 1,
            eyebrowLeftYOffset = 1,
            eyebrowRightYOffset = 1
        )

        // Pre-blink droop: lids fall slowly before a slow blink — deliberate, not reflexive.
        fun softBlinkBuildFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftTopLidRows = 3,
            rightTopLidRows = 3,
            leftBottomLidRows = 1,
            rightBottomLidRows = 1,
            eyebrowLeftYOffset = 1,
            eyebrowRightYOffset = 1
        )

        // Slow blink closed: eyes fully closed but brows remain soft-worried.
        fun slowBlinkClosedFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftClosedSlit = true,
            rightClosedSlit = true,
            eyebrowLeftYOffset = 1,
            eyebrowRightYOffset = 1
        )

        // Reopen expectant: lids open back up to base, resuming the waiting gaze.
        fun reopenExpectantFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftTopLidRows = 2,
            rightTopLidRows = 2,
            eyebrowLeftYOffset = 1,
            eyebrowRightYOffset = 1
        )

        // Brief glance right: looking toward where someone might appear. Pupils shift right.
        fun glanceRightFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = 1,
            rightPupilOffset = 2,
            leftTopLidRows = 1,
            rightTopLidRows = 2,
            eyebrowLeftYOffset = 1,
            eyebrowRightYOffset = 0
        )

        // Hold the right glance — a moment of hope / anticipation.
        fun holdGlanceFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = 1,
            rightPupilOffset = 2,
            leftTopLidRows = 2,
            rightTopLidRows = 2,
            eyebrowLeftYOffset = 1,
            eyebrowRightYOffset = 1
        )

        // Brief glance left variant.
        fun glanceLeftFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = -2,
            rightPupilOffset = -1,
            leftTopLidRows = 2,
            rightTopLidRows = 1,
            eyebrowLeftYOffset = 0,
            eyebrowRightYOffset = 1
        )

        // Hold the left glance.
        fun holdGlanceLeftFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftPupilOffset = -2,
            rightPupilOffset = -1,
            leftTopLidRows = 2,
            rightTopLidRows = 2,
            eyebrowLeftYOffset = 1,
            eyebrowRightYOffset = 1
        )

        // Return-to-center: pupils shift back from glance, reassuming expectant gaze.
        fun glanceReturnFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftTopLidRows = 1,
            rightTopLidRows = 1,
            eyebrowLeftYOffset = 1,
            eyebrowRightYOffset = 1
        )

        // Tentative dip: lids briefly droop as if about to say something, then stop.
        // Reads as hesitance or unspoken longing — the "clingy hesitation" beat.
        fun tentativeDipFrame(): PixelFrame64 = CanonicalEyeRenderer.buildFrame(
            leftTopLidRows = 2,
            rightTopLidRows = 2,
            leftBottomLidRows = 1,
            rightBottomLidRows = 1,
            eyebrowLeftYOffset = 2,
            eyebrowRightYOffset = 2
        )
    }

    private enum class QuestionMarkMode {
        NONE,
        LEFT,
        CENTER,
        RIGHT
    }

    private object CanonicalEyeRenderer {
        private val palette = PixelPetDefaultPalette.palette
        private val transparent = palette.indexOf(PixelPetDefaultPalette.TransparentKey)
        private val eyeBase = palette.indexOf(PixelPetDefaultPalette.EyeBaseKey)
        private val pupil = palette.indexOf(PixelPetDefaultPalette.PupilKey)
        val highlight: Int = palette.indexOf(PixelPetDefaultPalette.HighlightKey)
        private val accent = palette.indexOf(PixelPetDefaultPalette.AccentKey)

        private const val eyeTop = 21
        private const val leftEyeStartX = 12
        private const val rightEyeStartX = 37
        private const val pupilBaseStartX = 5
        private const val pupilTopOffset = 5
        private const val eyebrowTopY = 17
        private const val maxHorizontalPupilOffset = 2
        private val eyeRowWidths = listOf(
            4..10,
            2..12,
            1..13,
            0..14,
            0..14,
            0..14,
            0..14,
            0..14,
            0..14,
            0..14,
            0..14,
            0..14,
            0..14,
            1..13,
            2..12,
            4..10
        )

        fun buildFrame(
            leftPupilOffset: Int = 0,
            rightPupilOffset: Int = 0,
            leftTopLidRows: Int = 0,
            rightTopLidRows: Int = 0,
            leftBottomLidRows: Int = 0,
            rightBottomLidRows: Int = 0,
            leftClosedSlit: Boolean = false,
            rightClosedSlit: Boolean = false,
            eyebrowLeftYOffset: Int = 0,
            eyebrowRightYOffset: Int = 0,
            eyebrowLeftColor: Int = accent,
            eyebrowRightColor: Int = accent,
            extraHighlight: Boolean = false,
            questionMarkMode: QuestionMarkMode = QuestionMarkMode.NONE
        ): PixelFrame64 {
            require(leftPupilOffset in -maxHorizontalPupilOffset..maxHorizontalPupilOffset)
            require(rightPupilOffset in -maxHorizontalPupilOffset..maxHorizontalPupilOffset)

            val pixels = MutableList(PixelFrame64.PIXEL_COUNT) { transparent }
            drawEyebrowBand(pixels = pixels, startX = leftEyeStartX, yOffset = eyebrowLeftYOffset, colorIndex = eyebrowLeftColor)
            drawEyebrowBand(pixels = pixels, startX = rightEyeStartX, yOffset = eyebrowRightYOffset, colorIndex = eyebrowRightColor)
            drawEyeShell(pixels = pixels, startX = leftEyeStartX)
            drawEyeShell(pixels = pixels, startX = rightEyeStartX)

            if (leftClosedSlit) {
                drawClosedBlink(pixels = pixels, startX = leftEyeStartX)
            } else {
                drawPupil(pixels = pixels, startX = leftEyeStartX, horizontalOffset = leftPupilOffset)
                applyLids(
                    pixels = pixels,
                    startX = leftEyeStartX,
                    topRows = leftTopLidRows,
                    bottomRows = leftBottomLidRows
                )
                drawHighlights(
                    pixels = pixels,
                    startX = leftEyeStartX,
                    horizontalOffset = leftPupilOffset,
                    extraHighlight = extraHighlight
                )
            }

            if (rightClosedSlit) {
                drawClosedBlink(pixels = pixels, startX = rightEyeStartX)
            } else {
                drawPupil(pixels = pixels, startX = rightEyeStartX, horizontalOffset = rightPupilOffset)
                applyLids(
                    pixels = pixels,
                    startX = rightEyeStartX,
                    topRows = rightTopLidRows,
                    bottomRows = rightBottomLidRows
                )
                drawHighlights(
                    pixels = pixels,
                    startX = rightEyeStartX,
                    horizontalOffset = rightPupilOffset,
                    extraHighlight = extraHighlight
                )
            }

            drawQuestionMark(pixels = pixels, mode = questionMarkMode)

            return PixelFrame64(
                palette = palette,
                pixelIndices = pixels
            )
        }

        private fun drawEyebrowBand(
            pixels: MutableList<Int>,
            startX: Int,
            yOffset: Int,
            colorIndex: Int
        ) {
            fillRect(
                pixels,
                xRange = (startX + 3)..(startX + 11),
                yRange = (eyebrowTopY + yOffset)..(eyebrowTopY + yOffset),
                colorIndex = colorIndex
            )
            fillRect(
                pixels,
                xRange = (startX + 2)..(startX + 12),
                yRange = (eyebrowTopY + yOffset + 1)..(eyebrowTopY + yOffset + 1),
                colorIndex = colorIndex
            )
        }

        private fun drawEyeShell(
            pixels: MutableList<Int>,
            startX: Int
        ) {
            eyeRowWidths.forEachIndexed { rowIndex, xRange ->
                fillRect(
                    pixels = pixels,
                    xRange = (startX + xRange.first)..(startX + xRange.last),
                    yRange = (eyeTop + rowIndex)..(eyeTop + rowIndex),
                    colorIndex = eyeBase
                )
            }
        }

        private fun drawPupil(
            pixels: MutableList<Int>,
            startX: Int,
            horizontalOffset: Int
        ) {
            fillRect(
                pixels = pixels,
                xRange = (startX + pupilBaseStartX + horizontalOffset)..(startX + pupilBaseStartX + horizontalOffset + 3),
                yRange = (eyeTop + pupilTopOffset)..(eyeTop + pupilTopOffset + 4),
                colorIndex = pupil
            )
            fillRect(
                pixels = pixels,
                xRange = (startX + pupilBaseStartX + horizontalOffset + 1)..(startX + pupilBaseStartX + horizontalOffset + 2),
                yRange = (eyeTop + pupilTopOffset - 1)..(eyeTop + pupilTopOffset - 1),
                colorIndex = pupil
            )
        }

        private fun drawHighlights(
            pixels: MutableList<Int>,
            startX: Int,
            horizontalOffset: Int,
            extraHighlight: Boolean
        ) {
            setPixel(
                pixels = pixels,
                x = startX + pupilBaseStartX + horizontalOffset,
                y = eyeTop + pupilTopOffset,
                colorIndex = highlight
            )
            setPixel(
                pixels = pixels,
                x = startX + pupilBaseStartX + horizontalOffset + 1,
                y = eyeTop + pupilTopOffset + 1,
                colorIndex = highlight
            )
            if (extraHighlight) {
                setPixel(
                    pixels = pixels,
                    x = startX + pupilBaseStartX + horizontalOffset + 2,
                    y = eyeTop + pupilTopOffset,
                    colorIndex = highlight
                )
            }
        }

        private fun applyLids(
            pixels: MutableList<Int>,
            startX: Int,
            topRows: Int,
            bottomRows: Int
        ) {
            repeat(topRows.coerceAtMost(eyeRowWidths.size / 2)) { lidIndex ->
                val row = eyeTop + lidIndex
                fillRect(
                    pixels = pixels,
                    xRange = (startX + eyeRowWidths[lidIndex].first)..(startX + eyeRowWidths[lidIndex].last),
                    yRange = row..row,
                    colorIndex = accent
                )
            }
            repeat(bottomRows.coerceAtMost(eyeRowWidths.size / 2)) { lidIndex ->
                val templateIndex = eyeRowWidths.lastIndex - lidIndex
                val row = eyeTop + templateIndex
                fillRect(
                    pixels = pixels,
                    xRange = (startX + eyeRowWidths[templateIndex].first)..(startX + eyeRowWidths[templateIndex].last),
                    yRange = row..row,
                    colorIndex = accent
                )
            }
        }

        private fun drawClosedBlink(
            pixels: MutableList<Int>,
            startX: Int
        ) {
            fillRect(pixels, xRange = (startX + 2)..(startX + 12), yRange = (eyeTop + 7)..(eyeTop + 7), colorIndex = accent)
            fillRect(pixels, xRange = (startX + 1)..(startX + 13), yRange = (eyeTop + 8)..(eyeTop + 8), colorIndex = accent)
            fillRect(pixels, xRange = (startX + 2)..(startX + 12), yRange = (eyeTop + 9)..(eyeTop + 9), colorIndex = highlight)
        }

        private fun drawQuestionMark(
            pixels: MutableList<Int>,
            mode: QuestionMarkMode
        ) {
            val centerX = when (mode) {
                QuestionMarkMode.NONE -> return
                QuestionMarkMode.LEFT -> 24
                QuestionMarkMode.CENTER -> 31
                QuestionMarkMode.RIGHT -> 40
            }
            val topY = 9
            val dotY = 17
            setPixel(pixels, centerX - 1, topY, highlight)
            setPixel(pixels, centerX, topY, highlight)
            setPixel(pixels, centerX + 1, topY + 1, highlight)
            setPixel(pixels, centerX + 1, topY + 2, highlight)
            setPixel(pixels, centerX, topY + 3, highlight)
            setPixel(pixels, centerX, topY + 5, highlight)
            setPixel(pixels, centerX, dotY, highlight)
        }

        private fun fillRect(
            pixels: MutableList<Int>,
            xRange: IntRange,
            yRange: IntRange,
            colorIndex: Int
        ) {
            for (y in yRange) {
                for (x in xRange) {
                    setPixel(pixels = pixels, x = x, y = y, colorIndex = colorIndex)
                }
            }
        }

        private fun setPixel(
            pixels: MutableList<Int>,
            x: Int,
            y: Int,
            colorIndex: Int
        ) {
            pixels[(y * PixelFrame64.WIDTH) + x] = colorIndex
        }
    }
}
