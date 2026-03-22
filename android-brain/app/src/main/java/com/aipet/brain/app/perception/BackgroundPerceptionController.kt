package com.aipet.brain.app.perception

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.aipet.brain.perception.camera.FrameAnalyzer
import com.aipet.brain.perception.vision.FaceDetectionPipeline
import com.aipet.brain.perception.vision.model.FaceDetectionResult
import com.aipet.brain.perception.vision.objectdetection.ObjectDetectionEngine
import com.aipet.brain.perception.vision.objectdetection.model.ObjectDetectionResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runs camera analysis in the background (no preview surface) bound to the given
 * LifecycleOwner. Uses front camera and STRATEGY_KEEP_ONLY_LATEST to minimise load.
 *
 * Call [start] once camera permission is granted. Call [stop] to unbind and release.
 * The camera also stops automatically when the LifecycleOwner is destroyed.
 *
 * The base scan interval is [BASE_OBJECT_DETECTION_INTERVAL_MS] for object detection
 * and [BASE_FACE_CROP_INTERVAL_MS] for face crop emissions. Both can be temporarily
 * reduced to [ACTIVE_SCAN_INTERVAL_MS] by calling [boostScanRate] (e.g. when a person
 * is known to be nearby). The boost lasts for [BOOST_DURATION_MS].
 */
class BackgroundPerceptionController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val objectDetectionEngine: ObjectDetectionEngine?,
    private val onFaceDetectionResult: (FaceDetectionResult) -> Unit,
    private val onObjectDetectionResult: (Result<ObjectDetectionResult>) -> Unit,
    private val onLiveFaceCropReady: ((android.graphics.Bitmap, Long, Int) -> Unit)?
) {

    private val started = AtomicBoolean(false)
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    /** The most recent full-frame bitmap from object detection. Caller may copy it. */
    @Volatile var latestFrameSnapshot: android.graphics.Bitmap? = null
        private set

    @Volatile private var boostUntilMs: Long = 0L

    /** Temporarily boost scan rate (called when a person event is seen). */
    fun boostScanRate() {
        boostUntilMs = System.currentTimeMillis() + BOOST_DURATION_MS
        Log.d(TAG, "Scan rate boosted for ${BOOST_DURATION_MS}ms.")
    }

    private val currentFaceCropIntervalMs: Long
        get() = if (System.currentTimeMillis() < boostUntilMs) {
            ACTIVE_SCAN_INTERVAL_MS
        } else {
            BASE_FACE_CROP_INTERVAL_MS
        }

    private val currentObjectDetectionIntervalMs: Long
        get() = if (System.currentTimeMillis() < boostUntilMs) {
            ACTIVE_SCAN_INTERVAL_MS
        } else {
            BASE_OBJECT_DETECTION_INTERVAL_MS
        }

    private var faceDetectionPipeline: FaceDetectionPipeline? = null
    private var cameraProvider: ProcessCameraProvider? = null

    fun start() {
        if (!isCameraPermissionGranted()) {
            Log.w(TAG, "Camera permission not granted. BackgroundPerceptionController not started.")
            return
        }
        if (!started.compareAndSet(false, true)) {
            Log.d(TAG, "Already started.")
            return
        }

        val pipeline = FaceDetectionPipeline(
            onFacesDetected = { result -> onFaceDetectionResult(result) },
            onLiveFaceCropReady = onLiveFaceCropReady,
            liveFaceCropIntervalMs = currentFaceCropIntervalMs
        )
        faceDetectionPipeline = pipeline

        val frameAnalyzer = FrameAnalyzer(
            faceDetectionPipeline = pipeline,
            objectDetectionEngine = objectDetectionEngine,
            minObjectDetectionIntervalMs = currentObjectDetectionIntervalMs,
            onObjectDetectionResult = { result -> onObjectDetectionResult(result) },
            onFrameSnapshotCaptured = { snapshot ->
                latestFrameSnapshot?.recycle()
                latestFrameSnapshot = snapshot
            }
        )

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            runCatching {
                val provider = cameraProviderFuture.get()

                val analysisUseCase = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setResolutionSelector(
                        androidx.camera.core.resolutionselector.ResolutionSelector.Builder()
                            .setResolutionStrategy(
                                androidx.camera.core.resolutionselector.ResolutionStrategy(
                                    Size(640, 480),
                                    androidx.camera.core.resolutionselector.ResolutionStrategy
                                        .FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                )
                            )
                            .build()
                    )
                    .build()
                    .also { ia -> ia.setAnalyzer(analysisExecutor, frameAnalyzer) }

                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    analysisUseCase
                )
                cameraProvider = provider
                Log.i(TAG, "Background camera analysis started (no preview).")
            }.onFailure { error ->
                Log.e(TAG, "Failed to bind background camera: ${error.message}", error)
                started.set(false)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stop() {
        if (!started.compareAndSet(true, false)) return
        runCatching {
            cameraProvider?.unbindAll()
        }.onFailure { error: Throwable -> Log.w(TAG, "Error unbinding camera: ${error.message}") }
        faceDetectionPipeline?.close()
        faceDetectionPipeline = null
        cameraProvider = null
        latestFrameSnapshot?.recycle()
        latestFrameSnapshot = null
        Log.i(TAG, "Background camera analysis stopped.")
    }

    fun release() {
        stop()
        analysisExecutor.shutdown()
    }

    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "BackgroundPerception"

        /** Base interval between face-crop emissions when idle. */
        const val BASE_FACE_CROP_INTERVAL_MS = 5_000L

        /** Base interval between object detection runs when idle. */
        const val BASE_OBJECT_DETECTION_INTERVAL_MS = 5_000L

        /** Reduced interval when scan boost is active (person nearby etc.). */
        const val ACTIVE_SCAN_INTERVAL_MS = 1_000L

        /** How long a boost from [boostScanRate] lasts. */
        const val BOOST_DURATION_MS = 30_000L
    }
}
