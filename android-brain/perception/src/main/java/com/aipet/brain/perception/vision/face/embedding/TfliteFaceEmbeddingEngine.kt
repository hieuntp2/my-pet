package com.aipet.brain.perception.vision.face.embedding

import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.aipet.brain.core.common.math.VectorMath
import com.aipet.brain.perception.vision.face.embedding.model.FaceEmbeddingModelConfig
import com.aipet.brain.perception.vision.face.embedding.preprocess.FaceEmbeddingPreprocessor
import java.io.FileInputStream
import java.nio.channels.FileChannel
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import org.tensorflow.lite.Interpreter

class TfliteFaceEmbeddingEngine(
    private val assetManager: AssetManager,
    private val modelConfig: FaceEmbeddingModelConfig = FaceEmbeddingModelConfig(),
    private val preprocessor: FaceEmbeddingPreprocessor = FaceEmbeddingPreprocessor(
        inputWidth = modelConfig.inputWidth,
        inputHeight = modelConfig.inputHeight,
        pixelNormalization = modelConfig.pixelNormalization
    )
) : FaceEmbeddingEngine {

    private val closed = AtomicBoolean(false)
    private val interpreterLock = Any()
    private val interpreter = createInterpreter(
        assetManager = assetManager,
        modelAssetPath = modelConfig.modelAssetPath,
        numThreads = modelConfig.numThreads
    )

    override val embeddingDimension: Int = resolveEmbeddingDimension(interpreter)

    override fun generateEmbedding(faceBitmap: Bitmap): Result<FloatArray> {
        if (closed.get()) {
            return Result.failure(IllegalStateException("Face embedding engine is closed."))
        }

        val inputBuffer = preprocessor.toModelInput(faceBitmap).getOrElse { error ->
            Log.w(TAG, "Face embedding preprocessing failed: ${error.message}", error)
            return Result.failure(error)
        }

        return runCatching {
            val outputTensor = Array(1) { FloatArray(embeddingDimension) }
            val inferenceStartedNs = SystemClock.elapsedRealtimeNanos()

            synchronized(interpreterLock) {
                inputBuffer.rewind()
                interpreter.run(inputBuffer, outputTensor)
            }

            val normalized = VectorMath.l2Normalize(outputTensor[0])
            val inferenceMs = (SystemClock.elapsedRealtimeNanos() - inferenceStartedNs) / NS_PER_MS
            Log.d(
                TAG,
                "Embedding generated dim=$embeddingDimension, inferenceMs=$inferenceMs, " +
                    "head=${normalized.previewValues()}"
            )
            normalized
        }.onFailure { error ->
            Log.e(TAG, "Face embedding inference failed: ${error.message}", error)
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) {
            return
        }
        synchronized(interpreterLock) {
            runCatching {
                interpreter.close()
            }.onFailure { error ->
                Log.w(TAG, "Failed to close face embedding interpreter safely.", error)
            }
        }
    }

    private fun createInterpreter(
        assetManager: AssetManager,
        modelAssetPath: String,
        numThreads: Int
    ): Interpreter {
        val modelBuffer = loadModelBuffer(
            assetManager = assetManager,
            modelAssetPath = modelAssetPath
        )
        val options = Interpreter.Options().apply {
            setNumThreads(numThreads)
        }
        return Interpreter(modelBuffer, options)
    }

    private fun loadModelBuffer(
        assetManager: AssetManager,
        modelAssetPath: String
    ) = assetManager.openFd(modelAssetPath).use { descriptor ->
        FileInputStream(descriptor.fileDescriptor).channel.use { channel ->
            channel.map(
                FileChannel.MapMode.READ_ONLY,
                descriptor.startOffset,
                descriptor.declaredLength
            )
        }
    }

    private fun resolveEmbeddingDimension(interpreter: Interpreter): Int {
        val outputShape = interpreter.getOutputTensor(OUTPUT_TENSOR_INDEX).shape()
        if (outputShape.isEmpty()) {
            throw IllegalStateException("Face embedding model output tensor shape is empty.")
        }
        val candidateDimension = outputShape.last()
        require(candidateDimension > 0) {
            "Face embedding model output dimension is invalid: $candidateDimension"
        }
        return candidateDimension
    }

    private fun FloatArray.previewValues(): String {
        if (isEmpty()) {
            return "[]"
        }
        val previewCount = minOf(size, PREVIEW_VALUE_COUNT)
        val preview = buildString {
            append("[")
            for (index in 0 until previewCount) {
                if (index > 0) {
                    append(", ")
                }
                append(String.format(Locale.US, "%.4f", this@previewValues[index]))
            }
            append("]")
        }
        return preview
    }

    private inline fun <R> AssetFileDescriptor.use(block: (AssetFileDescriptor) -> R): R {
        return try {
            block(this)
        } finally {
            runCatching {
                close()
            }
        }
    }

    companion object {
        private const val TAG = "FaceEmbeddingEngine"
        private const val OUTPUT_TENSOR_INDEX = 0
        private const val NS_PER_MS = 1_000_000L
        private const val PREVIEW_VALUE_COUNT = 3
    }
}
