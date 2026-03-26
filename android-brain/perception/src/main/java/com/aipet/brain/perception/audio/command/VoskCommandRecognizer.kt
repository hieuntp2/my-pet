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
    private var modelInitStartTimeMs: Long = 0L
    // Reusable buffer to avoid per-frame ByteArray allocation; only accessed on processing thread.
    private var pcmConversionBuffer: ByteArray = ByteArray(0)

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
            modelInitStartTimeMs = System.currentTimeMillis()
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
            if (state == OfflineCommandRecognizerState.LISTENING && listeningRequested) {
                Log.d(TAG, "Vosk already listening; ignoring duplicate startListening().")
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
                Log.i(TAG, "Vosk recognizer created. sampleRate=${frame.sampleRate}Hz")
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
            val pcmBytes = frame.toPcmBytesReusing()
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
        val loadDurationMs = System.currentTimeMillis() - modelInitStartTimeMs
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
            Log.i(TAG, "Vosk model init completed in ${loadDurationMs}ms.")
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
        val isTrueFailure = synchronized(lock) {
            if (state == OfflineCommandRecognizerState.RELEASED) {
                return
            }
            // If state is no longer LISTENING, a concurrent stopListening()/release() already
            // cleaned up the recognizer. The exception is a benign race — do not escalate to FAILED.
            if (state != OfflineCommandRecognizerState.LISTENING) {
                Log.w(TAG, "Vosk frame exception after stop (benign race): ${cause.message}")
                false
            } else {
                state = OfflineCommandRecognizerState.FAILED
                listeningRequested = false
                closeRecognizerLocked()
                true
            }
        }
        if (isTrueFailure) {
            Log.e(TAG, message, cause)
            listener?.onError(message, cause)
        }
    }

    private fun closeRecognizerLocked() {
        recognizer?.let { activeRecognizer ->
            runCatching { activeRecognizer.close() }
                .onSuccess { Log.d(TAG, "Vosk recognizer closed.") }
                .onFailure { error ->
                    Log.w(TAG, "Failed to close Vosk recognizer: ${error.message}")
                }
        }
        recognizer = null
    }

    private fun closeModelLocked() {
        model?.let { activeModel ->
            runCatching { activeModel.close() }
                .onSuccess { Log.i(TAG, "Vosk model closed.") }
                .onFailure { error ->
                    Log.w(TAG, "Failed to close Vosk model: ${error.message}")
                }
        }
        model = null
    }

    /**
     * Converts [AudioFrame] PCM data to a little-endian byte array, reusing [pcmConversionBuffer]
     * to avoid per-frame heap allocation. Safe to call only from the single processing thread.
     */
    private fun AudioFrame.toPcmBytesReusing(): ByteArray {
        val validSamples = sampleCount.coerceAtMost(pcmData.size)
        val requiredSize = validSamples * SHORT_BYTES
        if (pcmConversionBuffer.size < requiredSize) {
            pcmConversionBuffer = ByteArray(requiredSize)
        }
        var sampleIndex = 0
        var outputIndex = 0
        while (sampleIndex < validSamples) {
            val sample = pcmData[sampleIndex].toInt()
            pcmConversionBuffer[outputIndex] = (sample and 0xFF).toByte()
            pcmConversionBuffer[outputIndex + 1] = ((sample shr 8) and 0xFF).toByte()
            sampleIndex += 1
            outputIndex += SHORT_BYTES
        }
        return pcmConversionBuffer
    }

    companion object {
        private const val TAG = "VoskCommandRecognizer"
        private const val MODEL_ASSET_PATH = "model-en-us"
        private const val MODEL_STORAGE_PATH = "model"
        private const val SHORT_BYTES = 2
    }
}
