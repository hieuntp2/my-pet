package com.aipet.brain.perception.vision.objectdetection.model

data class ObjectDetectionModelConfig(
    val modelAssetPath: String = DEFAULT_MODEL_ASSET_PATH,
    val numThreads: Int = DEFAULT_NUM_THREADS,
    val maxResults: Int = DEFAULT_MAX_RESULTS,
    val minConfidenceThreshold: Float = DEFAULT_MIN_CONFIDENCE_THRESHOLD
) {
    init {
        require(modelAssetPath.isNotBlank()) {
            "Object detection model asset path must not be blank."
        }
        require(numThreads > 0) {
            "Object detection interpreter thread count must be positive."
        }
        require(maxResults > 0) {
            "Object detection maxResults must be positive."
        }
        require(minConfidenceThreshold in 0f..1f) {
            "Object detection minConfidenceThreshold must be in [0, 1]."
        }
    }

    companion object {
        const val DEFAULT_MODEL_ASSET_PATH = "lite-model_efficientdet_lite0_detection_metadata_1.tflite"
        const val DEFAULT_NUM_THREADS = 2
        const val DEFAULT_MAX_RESULTS = 5
        const val DEFAULT_MIN_CONFIDENCE_THRESHOLD = 0.5f
    }
}
