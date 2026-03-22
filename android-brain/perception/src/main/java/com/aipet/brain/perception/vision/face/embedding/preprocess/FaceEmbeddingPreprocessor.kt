package com.aipet.brain.perception.vision.face.embedding.preprocess

import android.graphics.Bitmap
import com.aipet.brain.perception.vision.face.embedding.model.PixelNormalization
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.tensorflow.lite.DataType

class FaceEmbeddingPreprocessor(
    private val inputWidth: Int,
    private val inputHeight: Int,
    private val inputDataType: DataType,
    private val pixelNormalization: PixelNormalization
) {
    private val inputPixelCount = inputWidth * inputHeight

    init {
        require(inputWidth > 0 && inputHeight > 0) {
            "Input dimensions must be positive."
        }
        require(
            inputDataType == DataType.FLOAT32 || inputDataType == DataType.UINT8
        ) {
            "Unsupported face embedding input data type: $inputDataType"
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
                    inputPixelCount * CHANNEL_COUNT * bytesPerChannel(inputDataType)
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

                    when (inputDataType) {
                        DataType.FLOAT32 -> {
                            inputBuffer.putFloat(normalize(red))
                            inputBuffer.putFloat(normalize(green))
                            inputBuffer.putFloat(normalize(blue))
                        }

                        DataType.UINT8 -> {
                            inputBuffer.put(red.toInt().toByte())
                            inputBuffer.put(green.toInt().toByte())
                            inputBuffer.put(blue.toInt().toByte())
                        }

                        else -> error("Unsupported input type: $inputDataType")
                    }
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
        private const val COLOR_OFFSET = 127.5f
        private const val FLOAT_BYTES = 4
        private const val UINT8_BYTES = 1
    }
}
