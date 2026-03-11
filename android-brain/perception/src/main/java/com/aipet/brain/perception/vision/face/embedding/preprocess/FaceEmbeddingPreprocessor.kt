package com.aipet.brain.perception.vision.face.embedding.preprocess

import android.graphics.Bitmap
import com.aipet.brain.perception.vision.face.embedding.model.PixelNormalization
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FaceEmbeddingPreprocessor(
    private val inputWidth: Int,
    private val inputHeight: Int,
    private val pixelNormalization: PixelNormalization
) {
    private val inputPixelCount = inputWidth * inputHeight

    init {
        require(inputWidth > 0 && inputHeight > 0) {
            "Input dimensions must be positive."
        }
    }

    fun toModelInput(faceBitmap: Bitmap): Result<ByteBuffer> {
        if (faceBitmap.isRecycled || faceBitmap.width <= 0 || faceBitmap.height <= 0) {
            return Result.failure(
                IllegalArgumentException("Face bitmap is invalid or recycled.")
            )
        }

        return runCatching {
            var resizedBitmap: Bitmap? = null
            try {
                resizedBitmap = if (faceBitmap.width == inputWidth && faceBitmap.height == inputHeight) {
                    faceBitmap
                } else {
                    Bitmap.createScaledBitmap(faceBitmap, inputWidth, inputHeight, true)
                }

                val inputBuffer = ByteBuffer.allocateDirect(
                    inputPixelCount * CHANNEL_COUNT * BYTES_PER_FLOAT
                ).order(ByteOrder.nativeOrder())

                val pixels = IntArray(inputPixelCount)
                resizedBitmap.getPixels(
                    pixels,
                    0,
                    inputWidth,
                    0,
                    0,
                    inputWidth,
                    inputHeight
                )

                for (pixel in pixels) {
                    val red = (pixel shr RED_SHIFT and CHANNEL_MASK).toFloat()
                    val green = (pixel shr GREEN_SHIFT and CHANNEL_MASK).toFloat()
                    val blue = (pixel and CHANNEL_MASK).toFloat()

                    inputBuffer.putFloat(normalize(red))
                    inputBuffer.putFloat(normalize(green))
                    inputBuffer.putFloat(normalize(blue))
                }

                inputBuffer.rewind()
                inputBuffer
            } finally {
                if (resizedBitmap != null && resizedBitmap !== faceBitmap && !resizedBitmap.isRecycled) {
                    resizedBitmap.recycle()
                }
            }
        }.recoverCatching { error ->
            throw IllegalStateException("Failed to preprocess face bitmap for embedding.", error)
        }
    }

    private fun normalize(value: Float): Float {
        return when (pixelNormalization) {
            PixelNormalization.ZERO_TO_ONE -> value / MAX_COLOR_VALUE
            PixelNormalization.MINUS_ONE_TO_ONE -> (value - COLOR_OFFSET) / COLOR_OFFSET
        }
    }

    companion object {
        private const val CHANNEL_COUNT = 3
        private const val BYTES_PER_FLOAT = 4
        private const val CHANNEL_MASK = 0xFF
        private const val RED_SHIFT = 16
        private const val GREEN_SHIFT = 8
        private const val MAX_COLOR_VALUE = 255f
        private const val COLOR_OFFSET = 127.5f
    }
}
