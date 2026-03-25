package com.aipet.brain.perception.audio.keyword

import android.content.Context
import android.util.Log
import com.aipet.brain.perception.audio.command.OfflineCommandRecognizer
import com.aipet.brain.perception.audio.command.OfflineCommandRecognizerListener
import com.aipet.brain.perception.audio.command.OfflineCommandRecognizerState
import com.aipet.brain.perception.audio.command.VoskCommandRecognizer
import com.aipet.brain.perception.audio.model.AudioFrame
import com.aipet.brain.perception.audio.model.KeywordDetectionResult
import com.aipet.brain.perception.audio.model.KeywordDetectionType
import com.aipet.brain.perception.audio.model.KeywordSpotterState

class VoskKeywordSpotter(
    appContext: Context,
    private val commandRecognizer: OfflineCommandRecognizer = VoskCommandRecognizer(appContext)
) : KeywordSpotter {
    override val spotterId: String = SPOTTER_ID

    @Volatile
    private var detectionListener: KeywordDetectionListener? = null

    init {
        commandRecognizer.setListener(
            object : OfflineCommandRecognizerListener {
                override fun onPartialText(text: String) {
                    Log.d(TAG, "Partial command: $text")
                }

                override fun onFinalText(text: String) {
                    Log.d(TAG, "Final command: $text")
                }

                override fun onError(message: String, cause: Throwable?) {
                    if (cause == null) {
                        Log.e(TAG, message)
                    } else {
                        Log.e(TAG, message, cause)
                    }
                }
            }
        )
    }

    override fun start() {
        commandRecognizer.initialize()
        commandRecognizer.startListening()
    }

    override fun stop() {
        commandRecognizer.stopListening()
    }

    override fun reset() {
        commandRecognizer.stopListening()
    }

    override fun state(): KeywordSpotterState {
        return when (commandRecognizer.state()) {
            OfflineCommandRecognizerState.IDLE -> KeywordSpotterState.IDLE
            OfflineCommandRecognizerState.INITIALIZING -> KeywordSpotterState.STARTING
            OfflineCommandRecognizerState.READY -> KeywordSpotterState.IDLE
            OfflineCommandRecognizerState.LISTENING -> KeywordSpotterState.RUNNING
            OfflineCommandRecognizerState.FAILED -> KeywordSpotterState.FAILED
            OfflineCommandRecognizerState.RELEASED -> KeywordSpotterState.IDLE
        }
    }

    override fun processFrame(frame: AudioFrame): KeywordDetectionResult? {
        val output = commandRecognizer.processFrame(frame) ?: return null
        val normalizedCommand = output.normalizedCommand ?: return null
        val detection = KeywordDetectionResult(
            detectionType = KeywordDetectionType.KEYWORD,
            keywordId = normalizedCommand,
            keywordText = output.finalText ?: normalizedCommand,
            confidence = COMMAND_CONFIDENCE,
            timestampMs = System.currentTimeMillis(),
            engineName = spotterId
        )
        detectionListener?.onKeywordDetected(detection)
        return detection
    }

    override fun setDetectionListener(listener: KeywordDetectionListener?) {
        detectionListener = listener
    }

    override fun release() {
        detectionListener = null
        commandRecognizer.release()
    }

    companion object {
        private const val TAG = "VoskKeywordSpotter"
        private const val SPOTTER_ID = "vosk_command_spotter"
        // Vosk grammar mode does not provide calibrated confidence; keep this conservative.
        private const val COMMAND_CONFIDENCE = 0.35f
    }
}

