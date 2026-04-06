package com.aipet.brain.ui.avatar.pixel.runtime

import com.aipet.brain.ui.avatar.pixel.animation.FaceAnimationContext
import com.aipet.brain.ui.avatar.pixel.bridge.PixelPetAvatarIntent

/**
 * Translates FaceAnimationContext (intent + vitals) into an IdleBehaviorFamily
 * and continuous LayerLeakModifiers that affect all animation layers.
 *
 * State leakage rules:
 * - High sleepiness -> SLEEPY family, drooped lids, slow transitions
 * - Low energy -> slower transitions, slightly heavier lids
 * - Hungry / lonely intent -> EXPECTANT family, mild concerned brow
 * - High social -> PLAYFUL / ENGAGED
 * - High bond -> warmer baseline brow color for a friendlier expression
 */
internal class FaceResolver {

    fun resolveIdleFamily(context: FaceAnimationContext): IdleBehaviorFamily = when {
        context.sleepiness > 72 -> IdleBehaviorFamily.SLEEPY
        context.intent == PixelPetAvatarIntent.LOW_ENERGY && context.energy < 28 -> IdleBehaviorFamily.SLEEPY
        context.intent == PixelPetAvatarIntent.LONELY_NEED -> IdleBehaviorFamily.EXPECTANT
        context.intent == PixelPetAvatarIntent.HUNGRY_NEED -> IdleBehaviorFamily.EXPECTANT
        context.intent == PixelPetAvatarIntent.ASKING -> IdleBehaviorFamily.EXPECTANT
        context.intent == PixelPetAvatarIntent.ATTENTIVE -> IdleBehaviorFamily.CURIOUS
        context.intent == PixelPetAvatarIntent.LOOKING -> IdleBehaviorFamily.CURIOUS
        context.intent == PixelPetAvatarIntent.PROCESSING -> IdleBehaviorFamily.CURIOUS
        context.intent == PixelPetAvatarIntent.SURPRISED -> IdleBehaviorFamily.CURIOUS
        context.intent == PixelPetAvatarIntent.ENGAGED -> IdleBehaviorFamily.PLAYFUL
        context.intent == PixelPetAvatarIntent.EXCITED -> IdleBehaviorFamily.PLAYFUL
        context.social < 30 && context.intent == PixelPetAvatarIntent.NEUTRAL -> IdleBehaviorFamily.EXPECTANT
        else -> IdleBehaviorFamily.CALM
    }

    fun resolveStateLeakage(context: FaceAnimationContext): LayerLeakModifiers {
        val bondWarmth = when {
            context.bond >= 80 -> 0.55f
            context.bond >= 65 -> 0.38f
            context.bond >= 50 -> 0.24f
            else -> 0f
        }
        val socialPenalty = when {
            context.social < 25 -> 0.18f
            context.social < 40 -> 0.08f
            else -> 0f
        }

        return LayerLeakModifiers(
            topLidAdd = when {
                context.sleepiness > 60 -> ((context.sleepiness - 60) / 200f).coerceAtMost(0.20f)
                context.energy < 30 -> ((30 - context.energy) / 300f).coerceAtMost(0.08f)
                else -> 0f
            },
            browYAdd = when {
                context.hunger > 60 -> ((context.hunger - 60) / 300f).coerceAtMost(0.12f)
                context.social < 35 -> 0.08f
                else -> 0f
            },
            browWarmthFloor = (bondWarmth - socialPenalty).coerceIn(0f, 0.65f),
            speedMultiplier = when {
                context.sleepiness > 70 -> 0.52f
                context.sleepiness > 50 -> 0.72f
                context.energy < 28 -> 0.62f
                else -> 1.0f
            }
        )
    }
}
