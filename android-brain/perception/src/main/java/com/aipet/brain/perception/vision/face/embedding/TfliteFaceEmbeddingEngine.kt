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
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter

class TfliteFaceEmbeddingEngine(
    private val assetManager: AssetManager,
    private val modelConfig: FaceEmbeddingModelConfig = FaceEmbeddingModelConfig()
) : FaceEmbeddingEngine {

    private val closed = AtomicBoolean(false)
    private val interpreterLock = Any()
    private val interpreter = createInterpreter(
        assetManager = assetManager,
        modelAssetPath = modelConfig.modelAssetPath,
        numThreads = modelConfig.numThreads
    )
    private val inputTensorSpec = resolveInputTensorSpec(interpreter)
    private val outputTensorSpec = resolveOutputTensorSpec(interpreter)
    private val preprocessor = FaceEmbeddingPreprocessor(
        inputWidth = inputTensorSpec.width,
        inputHeight = inputTensorSpec.height,
        inputDataType = inputTensorSpec.dataType,
        pixelNormalization = modelConfig.pixelNormalization
    )

    override val embeddingDimension: Int = outputTensorSpec.embeddingDimension

    init {
        Log.i(
            TAG,
            "Face embedding model loaded asset=${modelConfig.modelAssetPath}, " +
                "input=${inputTensorSpec.width}x${inputTensorSpec.height} " +
                "${inputTensorSpec.dataType} batch=${inputTensorSpec.batchSize} " +
                "(${inputTensorSpec.byteCount} bytes), " +
                "outputDim=$embeddingDimension outputBatch=${outputTensorSpec.batchSize}"
        )
        if (inputTensorSpec.batchSize > 1) {
            Log.w(
                TAG,
                "Face embedding model input batch=${inputTensorSpec.batchSize}. " +
                    "Engine duplicates each face sample across batch and uses the first embedding."
            )
        }
    }

    override fun generateEmbedding(faceBitmap: Bitmap): Result<FloatArray> {
        if (closed.get()) {
            return Result.failure(IllegalStateException("Face embedding engine is closed."))
        }

        val singleSampleInput = preprocessor.toModelInput(faceBitmap).getOrElse { error ->
            Log.w(TAG, "Face embedding preprocessing failed: ${error.message}", error)
            return Result.failure(error)
        }
        if (singleSampleInput.capacity() != inputTensorSpec.singleSampleByteCount) {
            return Result.failure(
                IllegalStateException(
                    "Face embedding input buffer size mismatch: expectedSingleSample=" +
                        "${inputTensorSpec.singleSampleByteCount}, actual=${singleSampleInput.capacity()}"
                )
            )
        }
        val modelInputBuffer = buildModelInputBuffer(singleSampleInput)
        if (modelInputBuffer.capacity() != inputTensorSpec.byteCount) {
            return Result.failure(
                IllegalStateException(
                    "Face embedding model input size mismatch: expected=${inputTensorSpec.byteCount}, " +
                        "actual=${modelInputBuffer.capacity()}"
                )
            )
        }

        return runCatching {
            val outputTensor = Array(outputTensorSpec.batchSize) { FloatArray(embeddingDimension) }
            val inferenceStartedNs = SystemClock.elapsedRealtimeNanos()

            synchronized(interpreterLock) {
                modelInputBuffer.rewind()
                interpreter.run(modelInputBuffer, outputTensor)
            }

            val normalized = VectorMath.l2Normalize(outputTensor.first())
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

    private fun buildModelInputBuffer(singleSampleInput: ByteBuffer): ByteBuffer {
        if (inputTensorSpec.batchSize == 1) {
            singleSampleInput.rewind()
            return singleSampleInput
        }

        val sampleBytes = ByteArray(inputTensorSpec.singleSampleByteCount)
        val sampleReader = singleSampleInput.duplicate()
        sampleReader.rewind()
        sampleReader.get(sampleBytes)

        val batchedInput = ByteBuffer.allocateDirect(inputTensorSpec.byteCount)
            .order(ByteOrder.nativeOrder())
        repeat(inputTensorSpec.batchSize) {
            batchedInput.put(sampleBytes)
        }
        batchedInput.rewind()
        return batchedInput
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
            setUseNNAPI(true)
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

    private fun resolveOutputTensorSpec(interpreter: Interpreter): OutputTensorSpec {
        val outputShape = interpreter.getOutputTensor(OUTPUT_TENSOR_INDEX).shape()
        if (outputShape.isEmpty()) {
            throw IllegalStateException("Face embedding model output tensor shape is empty.")
        }
        val batchSize = if (outputShape.size > 1) {
            outputShape[BATCH_DIMENSION_INDEX]
        } else {
            1
        }
        require(batchSize > 0) {
            "Face embedding model output batch size is invalid: $batchSize"
        }
        val candidateDimension = outputShape.last()
        require(candidateDimension > 0) {
            "Face embedding model output dimension is invalid: $candidateDimension"
        }

        val outputDataType = interpreter.getOutputTensor(OUTPUT_TENSOR_INDEX).dataType()
        require(outputDataType == DataType.FLOAT32) {
            "Unsupported face embedding output data type: $outputDataType"
        }

        return OutputTensorSpec(
            batchSize = batchSize,
            embeddingDimension = candidateDimension
        )
    }

    private fun resolveInputTensorSpec(interpreter: Interpreter): InputTensorSpec {
        val tensor = interpreter.getInputTensor(INPUT_TENSOR_INDEX)
        val shape = tensor.shape()
        require(shape.size == INPUT_DIMENSION_COUNT) {
            "Face embedding model input must be 4D but was ${shape.contentToString()}"
        }
        val batchSize = shape[BATCH_DIMENSION_INDEX]
        require(batchSize > 0) {
            "Face embedding model batch size is invalid: $batchSize"
        }
        require(shape[CHANNEL_DIMENSION_INDEX] == RGB_CHANNEL_COUNT) {
            "Face embedding model must use RGB input channels but was ${shape[CHANNEL_DIMENSION_INDEX]}"
        }

        val width = shape[WIDTH_DIMENSION_INDEX]
        val height = shape[HEIGHT_DIMENSION_INDEX]
        require(width > 0 && height > 0) {
            "Face embedding input dimensions are invalid: ${shape.contentToString()}"
        }

        val dataType = tensor.dataType()
        require(dataType == DataType.FLOAT32 || dataType == DataType.UINT8) {
            "Unsupported face embedding input data type: $dataType"
        }

        val byteCount = tensor.numBytes()
        require(byteCount > 0) {
            "Face embedding model input byte size is invalid: $byteCount"
        }
        require(byteCount % batchSize == 0) {
            "Face embedding model input byte size is not divisible by batch size: bytes=$byteCount, batch=$batchSize"
        }

        return InputTensorSpec(
            batchSize = batchSize,
            width = width,
            height = height,
            dataType = dataType,
            byteCount = byteCount,
            singleSampleByteCount = byteCount / batchSize
        )
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
        private const val INPUT_TENSOR_INDEX = 0
        private const val OUTPUT_TENSOR_INDEX = 0
        private const val INPUT_DIMENSION_COUNT = 4
        private const val BATCH_DIMENSION_INDEX = 0
        private const val HEIGHT_DIMENSION_INDEX = 1
        private const val WIDTH_DIMENSION_INDEX = 2
        private const val CHANNEL_DIMENSION_INDEX = 3
        private const val RGB_CHANNEL_COUNT = 3
        private const val NS_PER_MS = 1_000_000L
        private const val PREVIEW_VALUE_COUNT = 3
    }

    private data class InputTensorSpec(
        val batchSize: Int,
        val width: Int,
        val height: Int,
        val dataType: DataType,
        val byteCount: Int,
        val singleSampleByteCount: Int
    )

    private data class OutputTensorSpec(
        val batchSize: Int,
        val embeddingDimension: Int
    )
}
