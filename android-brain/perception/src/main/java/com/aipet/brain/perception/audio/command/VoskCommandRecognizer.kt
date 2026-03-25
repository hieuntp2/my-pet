package com.aipet.brain.perception.audio.command

import android.content.Context
import android.util.Log
import com.aipet.brain.perception.audio.model.AudioFrame
import java.io.IOException
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService

class VoskCommandRecognizer(
    appContext: Context
) : OfflineCommandRecognizer {
    private val context = appContext.applicationContext
    private val lock = Any()

    @Volatile
    private var listener: OfflineCommandRecognizerListener? = null
    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var state: OfflineCommandRecognizerState = OfflineCommandRecognizerState.IDLE
    private var listeningRequested: Boolean = false
    private var lastPartialText: String? = null

    override fun initialize() {
        synchronized(lock) {
            if (state == OfflineCommandRecognizerState.RELEASED) {
                return
            }
            if (model != null) {
                if (state != OfflineCommandRecognizerState.LISTENING) {
                    state = OfflineCommandRecognizerState.READY
                }
                return
            }
            if (state == OfflineCommandRecognizerState.INITIALIZING) {
                return
            }
            state = OfflineCommandRecognizerState.INITIALIZING
            Log.i(TAG, "Vosk model init started.")
        }

        StorageService.unpack(
            context,
            MODEL_ASSET_PATH,
            MODEL_STORAGE_PATH,
            { loadedModel -> onModelLoaded(loadedModel) },
            { error -> onModelLoadFailed(error) }
        )
    }

    override fun startListening() {
        synchronized(lock) {
            if (state == OfflineCommandRecognizerState.RELEASED) {
                return
            }
            listeningRequested = true
            if (model == null && state != OfflineCommandRecognizerState.INITIALIZING) {
                initialize()
                return
            }
            if (model != null) {
                state = OfflineCommandRecognizerState.LISTENING
                Log.i(TAG, "Vosk listening started.")
            }
        }
    }

    override fun stopListening() {
        synchronized(lock) {
            listeningRequested = false
            recognizer?.let { activeRecognizer ->
                runCatching {
                    val finalJson = activeRecognizer.finalResult
                    val finalText = VoskCommandParsing.parseFinalText(finalJson)
                    if (!finalText.isNullOrBlank()) {
                        listener?.onFinalText(finalText)
                        Log.d(TAG, "Vosk final result (stop): $finalText")
                    }
                }.onFailure { error ->
                    Log.w(TAG, "Vosk final result flush failed: ${error.message}")
                }
            }
            closeRecognizerLocked()
            if (state != OfflineCommandRecognizerState.FAILED &&
                state != OfflineCommandRecognizerState.RELEASED
            ) {
                state = if (model != null) {
                    OfflineCommandRecognizerState.READY
                } else {
                    OfflineCommandRecognizerState.IDLE
                }
            }
            lastPartialText = null
            Log.i(TAG, "Vosk listening stopped.")
        }
    }

    override fun release() {
        synchronized(lock) {
            listeningRequested = false
            closeRecognizerLocked()
            closeModelLocked()
            state = OfflineCommandRecognizerState.RELEASED
            lastPartialText = null
            Log.i(TAG, "Vosk recognizer released.")
        }
    }

    override fun processFrame(frame: AudioFrame): OfflineCommandRecognitionOutput? {
        val activeRecognizer = synchronized(lock) {
            if (state != OfflineCommandRecognizerState.LISTENING || !listeningRequested) {
                return@synchronized null
            }
            val existingRecognizer = recognizer
            if (existingRecognizer != null) {
                return@synchronized existingRecognizer
            }
            val loadedModel = model ?: return@synchronized null
            return@synchronized try {
                val createdRecognizer = Recognizer(
                    loadedModel,
                    frame.sampleRate.toFloat(),
                    VoskCommandParsing.buildRestrictedGrammarJson()
                )
                recognizer = createdRecognizer
                createdRecognizer
            } catch (error: Throwable) {
                onRecognizerRuntimeFailure(
                    message = "Failed to create Vosk recognizer.",
                    cause = error
                )
                null
            }
        } ?: return null

        return runCatching {
            val pcmBytes = frame.toLittleEndianPcmBytes()
            val finalized = activeRecognizer.acceptWaveForm(pcmBytes, pcmBytes.size)
            if (finalized) {
                val finalJson = activeRecognizer.result
                val finalText = VoskCommandParsing.parseFinalText(finalJson)
                if (finalText.isNullOrBlank()) {
                    return@runCatching null
                }
                val normalized = VoskCommandParsing.normalizeCommand(finalText)
                listener?.onFinalText(finalText)
                synchronized(lock) {
                    lastPartialText = null
                }
                Log.d(TAG, "Vosk final result: raw=\"$finalText\", normalized=${normalized ?: "-"}")
                return@runCatching OfflineCommandRecognitionOutput(
                    finalText = finalText,
                    normalizedCommand = normalized
                )
            }

            val partialJson = activeRecognizer.partialResult
            val partialText = VoskCommandParsing.parsePartialText(partialJson)
            if (partialText.isNullOrBlank()) {
                return@runCatching null
            }
            val shouldEmitPartial = synchronized(lock) {
                if (partialText == lastPartialText) {
                    false
                } else {
                    lastPartialText = partialText
                    true
                }
            }
            if (!shouldEmitPartial) {
                return@runCatching null
            }
            listener?.onPartialText(partialText)
            Log.d(TAG, "Vosk partial result: $partialText")
            OfflineCommandRecognitionOutput(partialText = partialText)
        }.onFailure { error ->
            onRecognizerRuntimeFailure(
                message = "Failed to process Vosk audio frame.",
                cause = error
            )
        }.getOrNull()
    }

    override fun state(): OfflineCommandRecognizerState {
        synchronized(lock) {
            return state
        }
    }

    override fun setListener(listener: OfflineCommandRecognizerListener?) {
        this.listener = listener
    }

    private fun onModelLoaded(loadedModel: Model) {
        synchronized(lock) {
            if (state == OfflineCommandRecognizerState.RELEASED) {
                runCatching { loadedModel.close() }
                return
            }
            closeModelLocked()
            model = loadedModel
            state = if (listeningRequested) {
                Log.i(TAG, "Vosk listening started.")
                OfflineCommandRecognizerState.LISTENING
            } else {
                OfflineCommandRecognizerState.READY
            }
            Log.i(TAG, "Vosk model init completed.")
        }
    }

    private fun onModelLoadFailed(error: IOException) {
        synchronized(lock) {
            if (state == OfflineCommandRecognizerState.RELEASED) {
                return
            }
            state = OfflineCommandRecognizerState.FAILED
            listeningRequested = false
            closeRecognizerLocked()
        }
        val message = "Vosk model init failed: ${error.message ?: "unknown"}"
        Log.e(TAG, message, error)
        listener?.onError(message, error)
    }

    private fun onRecognizerRuntimeFailure(
        message: String,
        cause: Throwable
    ) {
        synchronized(lock) {
            if (state == OfflineCommandRecognizerState.RELEASED) {
                return
            }
            state = OfflineCommandRecognizerState.FAILED
            listeningRequested = false
            closeRecognizerLocked()
        }
        Log.e(TAG, message, cause)
        listener?.onError(message, cause)
    }

    private fun closeRecognizerLocked() {
        recognizer?.let { activeRecognizer ->
            runCatching { activeRecognizer.close() }
                .onFailure { error ->
                    Log.w(TAG, "Failed to close Vosk recognizer: ${error.message}")
                }
        }
        recognizer = null
    }

    private fun closeModelLocked() {
        model?.let { activeModel ->
            runCatching { activeModel.close() }
                .onFailure { error ->
                    Log.w(TAG, "Failed to close Vosk model: ${error.message}")
                }
        }
        model = null
    }

    private fun AudioFrame.toLittleEndianPcmBytes(): ByteArray {
        val validSamples = sampleCount.coerceAtMost(pcmData.size)
        val output = ByteArray(validSamples * SHORT_BYTES)
        var sampleIndex = 0
        var outputIndex = 0
        while (sampleIndex < validSamples) {
            val sample = pcmData[sampleIndex].toInt()
            output[outputIndex] = (sample and 0xFF).toByte()
            output[outputIndex + 1] = ((sample shr 8) and 0xFF).toByte()
            sampleIndex += 1
            outputIndex += SHORT_BYTES
        }
        return output
    }

    companion object {
        private const val TAG = "VoskCommandRecognizer"
        private const val MODEL_ASSET_PATH = "model-en-us"
        private const val MODEL_STORAGE_PATH = "model"
        private const val SHORT_BYTES = 2
    }
}
