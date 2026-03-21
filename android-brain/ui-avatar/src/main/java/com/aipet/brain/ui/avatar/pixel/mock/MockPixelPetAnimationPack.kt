package com.aipet.brain.ui.avatar.pixel.mock

import com.aipet.brain.ui.avatar.pixel.bridge.PixelAnimationSetRegistry
import com.aipet.brain.ui.avatar.pixel.model.Curious
import com.aipet.brain.ui.avatar.pixel.model.Happy
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
import com.aipet.brain.ui.avatar.pixel.model.Thinking

object MockPixelPetAnimationPack {
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
                Thinking to PixelPetAnimationStateSet(
                    state = Thinking,
                    variants = listOf(
                        variant(
                            id = "thinking_glance",
                            frames = listOf(
                                thinkingFrame(leftPupilX = 19, rightPupilX = 39),
                                thinkingFrame(leftPupilX = 21, rightPupilX = 41)
                            )
                        )
                    )
                ),
                Sleepy to PixelPetAnimationStateSet(
                    state = Sleepy,
                    variants = listOf(
                        variant(
                            id = "sleepy_blink",
                            frames = listOf(
                                sleepyFrame(yRange = 26..31),
                                sleepyFrame(yRange = 28..29)
                            )
                        )
                    )
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

    private fun frame(
        durationMillis: Int,
        frame: PixelFrame64
    ): PixelAnimationFrameEntry {
        return PixelAnimationFrameEntry(
            frame = frame,
            durationMillis = durationMillis
        )
    }

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

    private fun variant(
        id: String,
        frames: List<PixelFrame64>
    ): PixelAnimationVariant {
        return PixelAnimationVariant(
            id = id,
            clip = PixelAnimationClip(
                id = "${id}_clip",
                playbackMode = PixelAnimationPlaybackMode.LOOP,
                frames = frames.map { frame ->
                    PixelAnimationFrameEntry(
                        frame = frame,
                        durationMillis = FRAME_DURATION_MILLIS
                    )
                }
            ),
            tier = PixelAnimationVariantTier.PRIMARY,
            weight = 1
        )
    }

    private fun thinkingFrame(
        leftPupilX: Int,
        rightPupilX: Int
    ): PixelFrame64 {
        return legacyBaseFrame(
            eyeYRange = 22..37,
            accentYRange = 16..17,
            leftPupilXRange = leftPupilX..(leftPupilX + 3),
            rightPupilXRange = rightPupilX..(rightPupilX + 3),
            centerAccent = true
        )
    }

    private fun sleepyFrame(
        yRange: IntRange
    ): PixelFrame64 {
        return legacyBaseFrame(
            eyeYRange = yRange,
            accentYRange = 24..25,
            leftPupilXRange = 18..20,
            rightPupilXRange = 42..44
        )
    }

    private fun legacyBaseFrame(
        eyeYRange: IntRange,
        accentYRange: IntRange,
        leftPupilXRange: IntRange,
        rightPupilXRange: IntRange,
        accentColorKey: String = PixelPetDefaultPalette.AccentKey,
        centerAccent: Boolean = false
    ): PixelFrame64 {
        val palette = PixelPetDefaultPalette.palette
        val transparent = palette.indexOf(PixelPetDefaultPalette.TransparentKey)
        val eyeBase = palette.indexOf(PixelPetDefaultPalette.EyeBaseKey)
        val pupil = palette.indexOf(PixelPetDefaultPalette.PupilKey)
        val highlight = palette.indexOf(PixelPetDefaultPalette.HighlightKey)
        val accent = palette.indexOf(accentColorKey)
        val pixels = MutableList(PixelFrame64.PIXEL_COUNT) { transparent }

        fun fillRect(xRange: IntRange, yRange: IntRange, colorIndex: Int) {
            for (y in yRange) {
                for (x in xRange) {
                    pixels[(y * PixelFrame64.WIDTH) + x] = colorIndex
                }
            }
        }

        fillRect(xRange = 14..25, yRange = eyeYRange, colorIndex = eyeBase)
        fillRect(xRange = 38..49, yRange = eyeYRange, colorIndex = eyeBase)
        fillRect(xRange = leftPupilXRange, yRange = 26..31, colorIndex = pupil)
        fillRect(xRange = rightPupilXRange, yRange = 26..31, colorIndex = pupil)
        fillRect(xRange = 20..20, yRange = 27..27, colorIndex = highlight)
        fillRect(xRange = 44..44, yRange = 27..27, colorIndex = highlight)
        fillRect(xRange = 12..27, yRange = accentYRange, colorIndex = accent)
        fillRect(xRange = 36..51, yRange = accentYRange, colorIndex = accent)

        if (centerAccent) {
            fillRect(xRange = 29..34, yRange = 21..22, colorIndex = accent)
        }

        return PixelFrame64(
            palette = palette,
            pixelIndices = pixels
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
            extraHighlight: Boolean = false
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

    private const val FRAME_DURATION_MILLIS = 240
}
