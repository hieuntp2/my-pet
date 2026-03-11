package com.aipet.brain.app.ui.camera

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.aipet.brain.perception.vision.model.DetectedFace
import com.aipet.brain.perception.vision.model.FaceDetectionResult
import kotlin.math.max

@Composable
internal fun FaceOverlay(
    faceDetectionResult: FaceDetectionResult?,
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier
) {
    val labelPaint = remember {
        Paint().apply {
            color = Color.White.toArgb()
            isAntiAlias = true
            style = Paint.Style.FILL
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val result = faceDetectionResult ?: return@Canvas
        if (result.faces.isEmpty()) {
            return@Canvas
        }

        val sourceWidth = result.sourceWidth().toFloat()
        val sourceHeight = result.sourceHeight().toFloat()
        if (sourceWidth <= 0f || sourceHeight <= 0f || size.width <= 0f || size.height <= 0f) {
            return@Canvas
        }

        val overlayWidth = size.width
        val overlayHeight = size.height
        val scale = max(overlayWidth / sourceWidth, overlayHeight / sourceHeight)
        val scaledWidth = sourceWidth * scale
        val scaledHeight = sourceHeight * scale
        val xOffset = (overlayWidth - scaledWidth) / 2f
        val yOffset = (overlayHeight - scaledHeight) / 2f
        val strokeWidth = 2.dp.toPx()
        labelPaint.textSize = 12.dp.toPx()

        result.faces.forEach { face ->
            val mappedRect = face.mapToOverlayRect(
                scale = scale,
                xOffset = xOffset,
                yOffset = yOffset,
                overlayWidth = overlayWidth,
                overlayHeight = overlayHeight,
                mirrorHorizontally = isFrontCamera
            )
            if (mappedRect.width <= 0f || mappedRect.height <= 0f) {
                return@forEach
            }

            drawRect(
                color = FaceBoxColor,
                topLeft = Offset(mappedRect.left, mappedRect.top),
                size = mappedRect.size,
                style = Stroke(width = strokeWidth)
            )

            face.trackingId?.let { trackingId ->
                val label = "id:$trackingId"
                val labelX = mappedRect.left + LabelHorizontalPaddingPx
                val labelY = if (mappedRect.top > labelPaint.textSize + LabelVerticalPaddingPx) {
                    mappedRect.top - LabelVerticalPaddingPx
                } else {
                    mappedRect.top + labelPaint.textSize + LabelVerticalPaddingPx
                }
                drawContext.canvas.nativeCanvas.drawText(label, labelX, labelY, labelPaint)
            }
        }
    }
}

private fun FaceDetectionResult.sourceWidth(): Int {
    return when (normalizeRotation(rotationDegrees)) {
        90, 270 -> frameHeight
        else -> frameWidth
    }
}

private fun FaceDetectionResult.sourceHeight(): Int {
    return when (normalizeRotation(rotationDegrees)) {
        90, 270 -> frameWidth
        else -> frameHeight
    }
}

private fun normalizeRotation(rotationDegrees: Int): Int {
    val positive = rotationDegrees % FullRotationDegrees
    return if (positive >= 0) positive else positive + FullRotationDegrees
}

private data class OverlayRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val size: androidx.compose.ui.geometry.Size
        get() = androidx.compose.ui.geometry.Size(width = width, height = height)
}

private fun DetectedFace.mapToOverlayRect(
    scale: Float,
    xOffset: Float,
    yOffset: Float,
    overlayWidth: Float,
    overlayHeight: Float,
    mirrorHorizontally: Boolean
): OverlayRect {
    val rawLeft = boundingBox.left * scale + xOffset
    val rawTop = boundingBox.top * scale + yOffset
    val rawRight = boundingBox.right * scale + xOffset
    val rawBottom = boundingBox.bottom * scale + yOffset

    val mirroredLeft = if (mirrorHorizontally) {
        overlayWidth - rawRight
    } else {
        rawLeft
    }
    val mirroredRight = if (mirrorHorizontally) {
        overlayWidth - rawLeft
    } else {
        rawRight
    }

    return OverlayRect(
        left = mirroredLeft.coerceIn(0f, overlayWidth),
        top = rawTop.coerceIn(0f, overlayHeight),
        right = mirroredRight.coerceIn(0f, overlayWidth),
        bottom = rawBottom.coerceIn(0f, overlayHeight)
    )
}

private val FaceBoxColor = Color(0xFFFFEB3B)
private const val FullRotationDegrees = 360
private const val LabelHorizontalPaddingPx = 6f
private const val LabelVerticalPaddingPx = 6f
