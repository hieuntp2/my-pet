package com.aipet.brain.perception.vision.objectdetection

import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.aipet.brain.perception.vision.objectdetection.model.DetectedObject
import com.aipet.brain.perception.vision.objectdetection.model.ObjectBoundingBox
import com.aipet.brain.perception.vision.objectdetection.model.ObjectDetectionModelConfig
import com.aipet.brain.perception.vision.objectdetection.model.ObjectDetectionResult
import com.aipet.brain.perception.vision.objectdetection.utils.CocoLabelMapper
import com.aipet.brain.perception.vision.objectdetection.utils.ObjectDetectionPreprocessor
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter

class TfliteObjectDetectionEngine(
    private val assetManager: AssetManager,
    private val modelConfig: ObjectDetectionModelConfig = ObjectDetectionModelConfig()
) : ObjectDetectionEngine {

    private val closed = AtomicBoolean(false)
    private val interpreterLock = Any()
    private val interpreter = createInterpreter(
        assetManager = assetManager,
        modelAssetPath = modelConfig.modelAssetPath,
        numThreads = modelConfig.numThreads
    )
    private val inputTensorSpec = resolveInputSpec(interpreter)
    private val outputTensorSpecs = resolveOutputSpecs(interpreter)
    private val outputMapping = resolveOutputMapping(outputTensorSpecs)
    private val preprocessor = ObjectDetectionPreprocessor(
        inputWidth = inputTensorSpec.width,
        inputHeight = inputTensorSpec.height,
        inputDataType = inputTensorSpec.dataType
    )

    init {
        Log.i(
            TAG,
            "Object detection model loaded asset=${modelConfig.modelAssetPath}, " +
                "input=${inputTensorSpec.width}x${inputTensorSpec.height} ${inputTensorSpec.dataType}, " +
                "outputs=${outputTensorSpecs.joinToString { it.describe() }}, " +
                "minConfidence=${formatScore(modelConfig.minConfidenceThreshold)}"
        )
    }

    override fun detectObjects(
        frameBitmap: Bitmap,
        timestampMs: Long,
        rotationDegrees: Int
    ): Result<ObjectDetectionResult> {
        if (closed.get()) {
            return Result.failure(IllegalStateException("Object detection engine is closed."))
        }

        val preprocessedFrame = preprocessor.toModelInput(
            frameBitmap = frameBitmap,
            rotationDegrees = rotationDegrees
        ).getOrElse { error ->
            Log.w(TAG, "Object detection preprocessing failed: ${error.message}", error)
            return Result.failure(error)
        }

        return runCatching {
            val outputs = allocateOutputBuffers(outputTensorSpecs)
            val inferenceStartedNs = SystemClock.elapsedRealtimeNanos()
            synchronized(interpreterLock) {
                preprocessedFrame.inputBuffer.rewind()
                outputs.values.forEach { output ->
                    (output as ByteBuffer).rewind()
                }
                interpreter.runForMultipleInputsOutputs(
                    arrayOf(preprocessedFrame.inputBuffer),
                    outputs
                )
            }

            val detections = mapOutputsToDetections(
                outputs = outputs,
                sourceWidth = preprocessedFrame.sourceFrameWidth,
                sourceHeight = preprocessedFrame.sourceFrameHeight
            )
            val topConfidence = detections.maxOfOrNull { it.confidence } ?: 0f
            val inferenceMs = (SystemClock.elapsedRealtimeNanos() - inferenceStartedNs) / NS_PER_MS
            Log.d(
                TAG,
                "Object inference frame=${preprocessedFrame.sourceFrameWidth}x" +
                    "${preprocessedFrame.sourceFrameHeight}, detections=${detections.size}, " +
                    "topConfidence=${formatScore(topConfidence)}, " +
                    "threshold=${formatScore(modelConfig.minConfidenceThreshold)}, " +
                    "inferenceMs=$inferenceMs"
            )
            ObjectDetectionResult(
                timestampMs = timestampMs,
                detections = detections
            )
        }.onFailure { error ->
            Log.e(TAG, "Object detection inference failed: ${error.message}", error)
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
                Log.w(TAG, "Failed to close object detection interpreter safely.", error)
            }
        }
    }

    private fun mapOutputsToDetections(
        outputs: Map<Int, Any>,
        sourceWidth: Int,
        sourceHeight: Int
    ): List<DetectedObject> {
        val boxes = readTensorAsFloats(outputs, outputMapping.boxesTensorIndex)
        val scores = readTensorAsFloats(outputs, outputMapping.scoresTensorIndex)
        val classIds = readTensorAsFloats(outputs, outputMapping.classesTensorIndex)
        val reportedCount = outputMapping.countTensorIndex?.let { countIndex ->
            readTensorAsFloats(outputs, countIndex).firstOrNull()?.roundToInt()
        }

        if (boxes.isEmpty() || scores.isEmpty() || classIds.isEmpty()) {
            return emptyList()
        }

        val maxCandidateCount = minOf(scores.size, classIds.size, boxes.size / BOX_VALUE_COUNT)
        if (maxCandidateCount <= 0) {
            return emptyList()
        }

        val candidateCount = (reportedCount ?: maxCandidateCount).coerceIn(0, maxCandidateCount)
        val detections = ArrayList<DetectedObject>(candidateCount)
        for (index in 0 until candidateCount) {
            val confidence = scores[index]
            if (!confidence.isFinite() || confidence <= 0f) {
                continue
            }
            if (confidence < modelConfig.minConfidenceThreshold) {
                continue
            }

            val classId = classIds[index].roundToInt()
            val boundingBox = toBoundingBox(
                boxes = boxes,
                detectionIndex = index,
                sourceWidth = sourceWidth,
                sourceHeight = sourceHeight
            ) ?: continue
            if (!boundingBox.isValidDetection(sourceWidth = sourceWidth, sourceHeight = sourceHeight)) {
                continue
            }
            detections += DetectedObject(
                label = CocoLabelMapper.labelForClassId(classId),
                confidence = confidence.coerceIn(0f, 1f),
                boundingBox = boundingBox
            )
        }

        return detections
            .sortedByDescending { detection -> detection.confidence }
            .take(modelConfig.maxResults)
    }

    private fun toBoundingBox(
        boxes: FloatArray,
        detectionIndex: Int,
        sourceWidth: Int,
        sourceHeight: Int
    ): ObjectBoundingBox? {
        val offset = detectionIndex * BOX_VALUE_COUNT
        if (offset + (BOX_VALUE_COUNT - 1) >= boxes.size) {
            return null
        }

        val top = (boxes[offset] * sourceHeight).roundToInt().coerceIn(0, sourceHeight)
        val left = (boxes[offset + 1] * sourceWidth).roundToInt().coerceIn(0, sourceWidth)
        val bottom = (boxes[offset + 2] * sourceHeight).roundToInt().coerceIn(0, sourceHeight)
        val right = (boxes[offset + 3] * sourceWidth).roundToInt().coerceIn(0, sourceWidth)
        if (bottom <= top || right <= left) {
            return null
        }
        return ObjectBoundingBox(
            left = left,
            top = top,
            right = right,
            bottom = bottom
        )
    }

    private fun readTensorAsFloats(
        outputs: Map<Int, Any>,
        tensorIndex: Int
    ): FloatArray {
        val spec = outputTensorSpecs.firstOrNull { it.index == tensorIndex } ?: return FloatArray(0)
        val rawBuffer = outputs[tensorIndex] as? ByteBuffer ?: return FloatArray(0)
        rawBuffer.rewind()
        val values = FloatArray(spec.elementCount)
        when (spec.dataType) {
            DataType.FLOAT32 -> {
                for (index in 0 until spec.elementCount) {
                    values[index] = rawBuffer.float
                }
            }
            DataType.UINT8 -> {
                for (index in 0 until spec.elementCount) {
                    values[index] = (rawBuffer.get().toInt() and BYTE_MASK).toFloat()
                }
            }
            DataType.INT32 -> {
                for (index in 0 until spec.elementCount) {
                    values[index] = rawBuffer.int.toFloat()
                }
            }
            DataType.INT64 -> {
                for (index in 0 until spec.elementCount) {
                    values[index] = rawBuffer.long.toFloat()
                }
            }
            else -> {
                throw IllegalStateException(
                    "Unsupported output tensor data type at index $tensorIndex: ${spec.dataType}"
                )
            }
        }
        return values
    }

    private fun allocateOutputBuffers(
        outputSpecs: List<TensorSpec>
    ): MutableMap<Int, Any> {
        return outputSpecs.associate { spec ->
            spec.index to ByteBuffer
                .allocateDirect(spec.byteCount)
                .order(ByteOrder.nativeOrder())
        }.toMutableMap()
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
    ): ByteBuffer {
        return assetManager.openFd(modelAssetPath).use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).channel.use { channel ->
                channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    descriptor.startOffset,
                    descriptor.declaredLength
                )
            }
        }
    }

    private fun resolveInputSpec(interpreter: Interpreter): InputTensorSpec {
        val tensor = interpreter.getInputTensor(INPUT_TENSOR_INDEX)
        val shape = tensor.shape()
        require(shape.size == INPUT_DIMENSION_COUNT) {
            "Object detection model input must be 4D but was ${shape.contentToString()}"
        }
        require(shape[BATCH_DIMENSION_INDEX] == EXPECTED_BATCH_SIZE) {
            "Object detection model batch size must be 1 but was ${shape[BATCH_DIMENSION_INDEX]}"
        }
        require(shape[CHANNEL_DIMENSION_INDEX] == RGB_CHANNEL_COUNT) {
            "Object detection model must use RGB input channels but was ${shape[CHANNEL_DIMENSION_INDEX]}"
        }

        val width = shape[WIDTH_DIMENSION_INDEX]
        val height = shape[HEIGHT_DIMENSION_INDEX]
        require(width > 0 && height > 0) {
            "Object detection input dimensions are invalid: ${shape.contentToString()}"
        }

        return InputTensorSpec(
            width = width,
            height = height,
            dataType = tensor.dataType()
        )
    }

    private fun resolveOutputSpecs(interpreter: Interpreter): List<TensorSpec> {
        val outputCount = interpreter.outputTensorCount
        require(outputCount > 0) {
            "Object detection model has no output tensors."
        }

        return buildList(outputCount) {
            for (index in 0 until outputCount) {
                val tensor = interpreter.getOutputTensor(index)
                add(
                    TensorSpec(
                        index = index,
                        name = tensor.name(),
                        shape = tensor.shape(),
                        dataType = tensor.dataType(),
                        elementCount = tensor.numElements(),
                        byteCount = tensor.numBytes()
                    )
                )
            }
        }
    }

    private fun resolveOutputMapping(outputSpecs: List<TensorSpec>): OutputMapping {
        val boxes = outputSpecs.firstOrNull { spec ->
            spec.shape.lastOrNull() == BOX_VALUE_COUNT ||
                spec.name.contains("box", ignoreCase = true) ||
                spec.name.contains("location", ignoreCase = true)
        } ?: outputSpecs.firstOrNull()
        require(boxes != null) {
            "Unable to resolve object detection boxes tensor."
        }

        val score = outputSpecs.firstOrNull { spec ->
            spec.index != boxes.index &&
                (spec.name.contains("score", ignoreCase = true) ||
                    spec.name.contains("confidence", ignoreCase = true))
        } ?: outputSpecs.getOrNull(DEFAULT_SCORE_INDEX)
        require(score != null) {
            "Unable to resolve object detection scores tensor."
        }

        val classes = outputSpecs.firstOrNull { spec ->
            spec.index != boxes.index &&
                spec.index != score.index &&
                spec.name.contains("class", ignoreCase = true)
        } ?: outputSpecs.getOrNull(DEFAULT_CLASS_INDEX)
        require(classes != null) {
            "Unable to resolve object detection classes tensor."
        }

        val count = outputSpecs.firstOrNull { spec ->
            spec.index != boxes.index &&
                spec.index != score.index &&
                spec.index != classes.index &&
                (spec.name.contains("count", ignoreCase = true) ||
                    spec.name.contains("num", ignoreCase = true) ||
                    spec.elementCount == 1)
        } ?: outputSpecs.getOrNull(DEFAULT_COUNT_INDEX)

        return OutputMapping(
            boxesTensorIndex = boxes.index,
            classesTensorIndex = classes.index,
            scoresTensorIndex = score.index,
            countTensorIndex = count?.index
        )
    }

    private fun formatScore(score: Float): String {
        return String.format(Locale.US, "%.4f", score)
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

    private data class TensorSpec(
        val index: Int,
        val name: String,
        val shape: IntArray,
        val dataType: DataType,
        val elementCount: Int,
        val byteCount: Int
    ) {
        fun describe(): String {
            return "[$index:${shape.contentToString()} $dataType]"
        }
    }

    private data class InputTensorSpec(
        val width: Int,
        val height: Int,
        val dataType: DataType
    )

    private data class OutputMapping(
        val boxesTensorIndex: Int,
        val classesTensorIndex: Int,
        val scoresTensorIndex: Int,
        val countTensorIndex: Int?
    )

    companion object {
        private const val TAG = "ObjectDetectEngine"
        private const val INPUT_TENSOR_INDEX = 0
        private const val INPUT_DIMENSION_COUNT = 4
        private const val BATCH_DIMENSION_INDEX = 0
        private const val HEIGHT_DIMENSION_INDEX = 1
        private const val WIDTH_DIMENSION_INDEX = 2
        private const val CHANNEL_DIMENSION_INDEX = 3
        private const val EXPECTED_BATCH_SIZE = 1
        private const val RGB_CHANNEL_COUNT = 3
        private const val BOX_VALUE_COUNT = 4
        private const val DEFAULT_CLASS_INDEX = 1
        private const val DEFAULT_SCORE_INDEX = 2
        private const val DEFAULT_COUNT_INDEX = 3
        private const val BYTE_MASK = 0xFF
        private const val NS_PER_MS = 1_000_000L
    }
}


private fun ObjectBoundingBox.isValidDetection(
    sourceWidth: Int,
    sourceHeight: Int
): Boolean {
    val width = right - left
    val height = bottom - top
    if (width < MIN_OBJECT_SIDE_PX || height < MIN_OBJECT_SIDE_PX) {
        return false
    }
    val areaRatio = (width * height).toFloat() / (sourceWidth * sourceHeight).toFloat().coerceAtLeast(1f)
    if (areaRatio < MIN_OBJECT_AREA_RATIO) {
        return false
    }
    val aspectRatio = width.toFloat() / height.toFloat().coerceAtLeast(1f)
    return aspectRatio in MIN_OBJECT_ASPECT_RATIO..MAX_OBJECT_ASPECT_RATIO
}

private const val MIN_OBJECT_SIDE_PX = 36
private const val MIN_OBJECT_AREA_RATIO = 0.018f
private const val MIN_OBJECT_ASPECT_RATIO = 0.30f
private const val MAX_OBJECT_ASPECT_RATIO = 3.50f
