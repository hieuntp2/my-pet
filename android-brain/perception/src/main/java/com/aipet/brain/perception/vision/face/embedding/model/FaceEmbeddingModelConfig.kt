package com.aipet.brain.perception.vision.face.embedding.model

data class FaceEmbeddingModelConfig(
    val modelAssetPath: String = DEFAULT_MODEL_ASSET_PATH,
    val inputWidth: Int = DEFAULT_INPUT_WIDTH,
    val inputHeight: Int = DEFAULT_INPUT_HEIGHT,
    val pixelNormalization: PixelNormalization = PixelNormalization.MINUS_ONE_TO_ONE,
    val numThreads: Int = DEFAULT_NUM_THREADS
) {
    init {
        require(modelAssetPath.isNotBlank()) {
            "Model asset path must not be blank."
        }
        require(inputWidth > 0 && inputHeight > 0) {
            "Model input size must be positive."
        }
        require(numThreads > 0) {
            "Interpreter thread count must be positive."
        }
    }

    companion object {
        const val DEFAULT_MODEL_ASSET_PATH = "mobile_face_net.tflite"
        const val DEFAULT_INPUT_WIDTH = 112
        const val DEFAULT_INPUT_HEIGHT = 112
        const val DEFAULT_NUM_THREADS = 2
    }
}

enum class PixelNormalization {
    ZERO_TO_ONE,
    MINUS_ONE_TO_ONE
}
