package com.aipet.brain.perception.audio.keyword

import android.content.Context
import com.aipet.brain.core.common.config.KeywordSpottingProvider

class KeywordSpotterFactory(
    private val appContext: Context,
    private val voskSpotterBuilder: (Context) -> KeywordSpotter = { context ->
        VoskKeywordSpotter(context)
    }
) {
    fun create(provider: KeywordSpottingProvider): KeywordSpotter? {
        return when (provider) {
            KeywordSpottingProvider.NONE -> null
            KeywordSpottingProvider.PORCUPINE -> null
            KeywordSpottingProvider.TFLITE -> null
            KeywordSpottingProvider.VOSK -> voskSpotterBuilder(appContext)
            KeywordSpottingProvider.CUSTOM -> AcousticPatternKeywordSpotter()
        }
    }
}
