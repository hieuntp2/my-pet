package com.aipet.brain.brain.logic.audio

import android.util.Log
import com.aipet.brain.brain.events.EventBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect

class AudioStimulusObserver(
    private val eventBus: EventBus,
    private val stimulusMapper: AudioStimulusMapper = AudioStimulusMapper()
) {
    private val latestStimulus = MutableStateFlow<AudioStimulus?>(null)

    fun currentLatestStimulus(): AudioStimulus? = latestStimulus.value

    fun observeLatestStimulus(): StateFlow<AudioStimulus?> = latestStimulus.asStateFlow()

    suspend fun observeEventsAndMapStimuli() {
        eventBus.observe().collect { event ->
            val stimulus = stimulusMapper.map(event) ?: return@collect
            latestStimulus.value = stimulus
            Log.d(
                TAG,
                "Mapped ${event.type.name} into stimulus: ${stimulus.toDebugSummary()}"
            )
        }
    }

    companion object {
        private const val TAG = "AudioStimulusObserver"
    }
}
