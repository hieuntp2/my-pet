package com.aipet.brain.perception.vision.objectdetection.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.tensorflow.lite.DataType

internal class ObjectDetectionPreprocessor(
    private val inputWidth: Int,
    private val inputHeight: Int,
    private val inputDataType: DataType
) {
    private val inputPixelCount = inputWidth * inputHeight

    init {
        require(inputWidth > 0 && inputHeight > 0) {
            "Object detection input dimensions must be positive."
        }
        require(
            inputDataType == DataType.FLOAT32 || inputDataType == DataType.UINT8
        ) {
            "Unsupported object detection input data type: $inputDataType"
        }
    }

    fun toModelInput(
        frameBitmap: Bitmap,
        rotationDegrees: Int
    ): Result<PreprocessedObjectFrame> {
        if (frameBitmap.isRecycled || frameBitmap.width <= 0 || frameBitmap.height <= 0) {
            return Result.failure(
                IllegalArgumentException("Object detection frame bitmap is invalid or recycled.")
            )
        }

        return runCatching {
            var rotatedBitmap: Bitmap? = null
            var resizedBitmap: Bitmap? = null
            try {
                val normalizedRotation = normalizeRotation(rotationDegrees)
                val sourceBitmap = if (normalizedRotation == 0) {
                    frameBitmap
                } else {
                    val matrix = Matrix().apply {
                        postRotate(normalizedRotation.toFloat())
                    }
                    Bitmap.createBitmap(
                        frameBitmap,
                        0,
                        0,
                        frameBitmap.width,
                        frameBitmap.height,
                        matrix,
                        true
                    ).also { created ->
                        rotatedBitmap = created
                    }
                }

                val processedBitmap = if (sourceBitmap.width == inputWidth &&
                    sourceBitmap.height == inputHeight
                ) {
                    sourceBitmap
                } else {
                    Bitmap.createScaledBitmap(
                        sourceBitmap,
                        inputWidth,
                        inputHeight,
                        true
                    ).also { created ->
                        resizedBitmap = created
                    }
                }

                val inputBuffer = ByteBuffer.allocateDirect(
                    inputPixelCount * CHANNEL_COUNT * bytesPerChannel(inputDataType)
                ).order(ByteOrder.nativeOrder())

                val pixels = IntArray(inputPixelCount)
                processedBitmap.getPixels(
                    pixels,
                    0,
                    inputWidth,
                    0,
                    0,
                    inputWidth,
                    inputHeight
                )

                when (inputDataType) {
                    DataType.FLOAT32 -> fillFloatInput(pixels, inputBuffer)
                    DataType.UINT8 -> fillUint8Input(pixels, inputBuffer)
                    else -> error("Unsupported input type: $inputDataType")
                }
                inputBuffer.rewind()

                PreprocessedObjectFrame(
                    inputBuffer = inputBuffer,
                    sourceFrameWidth = sourceBitmap.width,
                    sourceFrameHeight = sourceBitmap.height
                )
            } finally {
                val rotated = rotatedBitmap
                if (rotated != null &&
                    rotated !== frameBitmap &&
                    !rotated.isRecycled
                ) {
                    rotated.recycle()
                }
                val resized = resizedBitmap
                if (resized != null &&
                    resized !== frameBitmap &&
                    resized !== rotated &&
                    !resized.isRecycled
                ) {
                    resized.recycle()
                }
            }
        }
    }

    private fun fillFloatInput(pixels: IntArray, inputBuffer: ByteBuffer) {
        for (pixel in pixels) {
            val red = (pixel shr RED_SHIFT and CHANNEL_MASK).toFloat()
            val green = (pixel shr GREEN_SHIFT and CHANNEL_MASK).toFloat()
            val blue = (pixel and CHANNEL_MASK).toFloat()

            inputBuffer.putFloat(red / MAX_COLOR_VALUE)
            inputBuffer.putFloat(green / MAX_COLOR_VALUE)
            inputBuffer.putFloat(blue / MAX_COLOR_VALUE)
        }
    }

    private fun fillUint8Input(pixels: IntArray, inputBuffer: ByteBuffer) {
        for (pixel in pixels) {
            val red = (pixel shr RED_SHIFT and CHANNEL_MASK).toByte()
            val green = (pixel shr GREEN_SHIFT and CHANNEL_MASK).toByte()
            val blue = (pixel and CHANNEL_MASK).toByte()

            inputBuffer.put(red)
            inputBuffer.put(green)
            inputBuffer.put(blue)
        }
    }

    private fun normalizeRotation(rotationDegrees: Int): Int {
        val normalized = ((rotationDegrees % FULL_ROTATION_DEGREES) + FULL_ROTATION_DEGREES) %
            FULL_ROTATION_DEGREES
        require(normalized % RIGHT_ANGLE_DEGREES == 0) {
            "Rotation must be a multiple of $RIGHT_ANGLE_DEGREES but was $rotationDegrees"
        }
        return normalized
    }

    private fun bytesPerChannel(dataType: DataType): Int {
        return when (dataType) {
            DataType.FLOAT32 -> FLOAT_BYTES
            DataType.UINT8 -> UINT8_BYTES
            else -> error("Unsupported input type: $dataType")
        }
    }

    companion object {
        private const val CHANNEL_COUNT = 3
        private const val CHANNEL_MASK = 0xFF
        private const val RED_SHIFT = 16
        private const val GREEN_SHIFT = 8
        private const val MAX_COLOR_VALUE = 255f
        private const val FLOAT_BYTES = 4
        private const val UINT8_BYTES = 1
        private const val RIGHT_ANGLE_DEGREES = 90
        private const val FULL_ROTATION_DEGREES = 360
    }
}

internal data class PreprocessedObjectFrame(
    val inputBuffer: ByteBuffer,
    val sourceFrameWidth: Int,
    val sourceFrameHeight: Int
)
