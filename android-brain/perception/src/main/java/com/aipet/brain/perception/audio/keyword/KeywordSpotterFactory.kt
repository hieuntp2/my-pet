package com.aipet.brain.perception.audio.keyword

import com.aipet.brain.core.common.config.KeywordSpottingProvider

class KeywordSpotterFactory {
    fun create(provider: KeywordSpottingProvider): KeywordSpotter? {
        // C121 integrates the first real on-device adapter while keeping provider-based wiring extensible.
        return when (provider) {
            KeywordSpottingProvider.NONE -> null
            KeywordSpottingProvider.PORCUPINE -> null
            KeywordSpottingProvider.TFLITE -> null
            KeywordSpottingProvider.CUSTOM -> AcousticPatternKeywordSpotter()
        }
    }
}
