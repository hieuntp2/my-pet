package com.aipet.brain.perception.audio.keyword

import android.content.Context
import android.content.ContextWrapper
import com.aipet.brain.core.common.config.KeywordSpottingProvider
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class KeywordSpotterFactoryTest {
    @Test
    fun `create returns null for provider none`() {
        val factory = KeywordSpotterFactory(appContext = ContextWrapper(null))
        assertNull(factory.create(KeywordSpottingProvider.NONE))
    }

    @Test
    fun `create returns injected vosk spotter for provider vosk`() {
        val expectedSpotter = AcousticPatternKeywordSpotter()
        val factory = KeywordSpotterFactory(
            appContext = ContextWrapper(null),
            voskSpotterBuilder = { _: Context -> expectedSpotter }
        )

        val resolvedSpotter = factory.create(KeywordSpottingProvider.VOSK)

        assertSame(expectedSpotter, resolvedSpotter)
    }
}

