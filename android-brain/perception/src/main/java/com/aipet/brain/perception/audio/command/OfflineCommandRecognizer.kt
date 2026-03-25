package com.aipet.brain.perception.audio.command

import com.aipet.brain.perception.audio.model.AudioFrame

interface OfflineCommandRecognizer {
    fun initialize()

    fun startListening()

    fun stopListening()

    fun release()

    fun processFrame(frame: AudioFrame): OfflineCommandRecognitionOutput?

    fun state(): OfflineCommandRecognizerState

    fun setListener(listener: OfflineCommandRecognizerListener?)
}

enum class OfflineCommandRecognizerState {
    IDLE,
    INITIALIZING,
    READY,
    LISTENING,
    FAILED,
    RELEASED
}

data class OfflineCommandRecognitionOutput(
    val partialText: String? = null,
    val finalText: String? = null,
    val normalizedCommand: String? = null
)

interface OfflineCommandRecognizerListener {
    fun onPartialText(text: String)

    fun onFinalText(text: String)

    fun onError(message: String, cause: Throwable? = null)
}

