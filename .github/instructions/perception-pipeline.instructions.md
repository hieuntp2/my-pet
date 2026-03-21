---
description: "Use when writing, editing, or reviewing perception code: CameraX pipeline, FrameAnalyzer, face detection, face embedding, object detection, audio capture, VAD, or keyword spotting."
applyTo: "perception/src/**/*.kt"
---

# Perception Pipeline Guidelines

## Ownership Boundary

- `:perception` owns: frame analysis, ML inference engines, audio capture, VAD, keyword spotting.
- `:perception` must **not**: publish to `EventBus`, read from `:brain` or `:memory`, make behavior decisions.
- Results are surfaced as **callbacks** from `:perception` classes.
- The **`:app` layer** bridges callbacks to `EventBus.publish(...)` — this is intentional and correct.

## CameraX Setup

- CameraX provider binding and lifecycle management live in `:app` (`CameraScreen`), **not** in `:perception`.
- `:perception` provides `FrameAnalyzer` which implements `ImageAnalysis.Analyzer`.
- `ImageAnalysis` must use `STRATEGY_KEEP_ONLY_LATEST` backpressure — never buffer frames.
- Always call `imageProxy.close()` in a `finally` block inside `analyze()`.
- Cleanup in `:app`: `clearAnalyzer()`, `unbindAll()`, executor shutdown, and pipeline `close()` inside `DisposableEffect`.

```kotlin
// FrameAnalyzer contract — always close in finally
override fun analyze(imageProxy: ImageProxy) {
    try {
        // process
    } finally {
        imageProxy.close()
    }
}
```

## Face Detection

- ML Kit detector created with `FaceDetection.getClient(FaceDetectorOptions)`.
- Required options: `PERFORMANCE_MODE_FAST`, `CLASSIFICATION_MODE_ALL`, `enableTracking()`.
- Disable unnecessary modes: `LANDMARK_MODE_NONE`, `CONTOUR_MODE_NONE`.
- `FaceDetectionPipeline` runs on a **single-thread executor** — one frame in flight at a time.
- Converts `ImageProxy` → NV21 `InputImage` before passing to ML Kit.
- Emits `FaceDetectionResult` callback with `DetectedFace` (bbox, trackingId, euler angles, smile prob, timestamp).
- `captureLargestFaceSample()` crops a face bitmap for embedding — call only when teaching.

## Face Embedding (TFLite)

- Contract: `FaceEmbeddingEngine` interface.
- Production impl: `TfliteFaceEmbeddingEngine` — uses `org.tensorflow.lite.Interpreter`.
- Unavailable state: `UnavailableFaceEmbeddingEngine` — returned when model file is missing; surfaces a clear error, does not crash.
- Preprocessing lives in `FaceEmbeddingPreprocessor` — do not inline preprocessing in the engine.
- Model config (path, input size, normalization) lives in `FaceEmbeddingModelConfig` — do not hardcode in engine.

## Object Detection (TFLite)

- Contract: `ObjectDetectionEngine` interface.
- Stable entry: `RealObjectDetectionEngine` (wraps TFLite impl with lifecycle safety).
- TFLite impl: `TfliteObjectDetectionEngine`.
- Object detection in `FrameAnalyzer` is **throttled** — not every frame. Respect the existing throttle.
- Input preprocessing: rotate/resize/normalize YUV/NV21 `Bitmap` to tensor buffer — logic stays in `ObjectDetectionPreprocessor`.
- Model config (path, labels, threshold) in `ObjectDetectionModelConfig`.

## Inference Engine Rules

- Engines are instantiated once and reused — do not create a new `Interpreter` per frame.
- Model files are loaded from assets — log a clear error and return an `UnavailableFaceEmbeddingEngine` / `null` if asset is missing.
- Preprocessing (resize, normalize, type convert) belongs in a dedicated `*Preprocessor` class.
- All inference runs on a background dispatcher or executor — never on the main thread.

## Audio Pipeline

- `AudioCaptureController` owns start/stop of audio capture.
- `AudioRecordFrameSource` implements `AudioFrameSource` (raw PCM frames from `AudioRecord`).
- `AudioProcessingDispatcher` routes frames to consumers: `EnergyEstimator`, `VoiceActivityDetector`, `KeywordSpotter`.
- `VadLightStateMachine` holds VAD state across frames — do not duplicate state in `VoiceActivityDetector`.
- `KeywordSpotter` interface; `AcousticPatternKeywordSpotter` is the production impl.
- Audio results surface as callbacks — same rule applies: `:app` bridges to `EventBus`.

## Event Types Owned by This Pipeline (bridged in `:app`)

| Event | Payload | Source |
|-------|---------|--------|
| `CAMERA_FRAME_RECEIVED` | `CameraFrameReceivedPayload` | `FrameAnalyzer` diagnostics callback |
| `FACE_DETECTED` | `FacesDetectedEventPayload` | `FaceDetectionPipeline` callback |
| `OBJECT_DETECTED` | `ObjectDetectedEventPayload` | `FrameAnalyzer` object detection result |

Do not add new event types inside `:perception`. Define them in `:brain`'s `EventType` enum instead.
