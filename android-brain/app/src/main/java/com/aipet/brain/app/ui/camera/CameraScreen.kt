package com.aipet.brain.app.ui.camera

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.aipet.brain.perception.camera.FrameAnalyzer
import com.aipet.brain.perception.camera.FrameDiagnostics
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    hasRequestedPermission: Boolean,
    onPermissionRequestTracked: () -> Unit,
    onFrameDiagnostics: (FrameDiagnostics) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionState by remember(context, hasRequestedPermission) {
        mutableStateOf(resolveCameraPermissionState(context, hasRequestedPermission))
    }

    LaunchedEffect(context, hasRequestedPermission) {
        permissionState = resolveCameraPermissionState(context, hasRequestedPermission)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        permissionState = resolveCameraPermissionState(context, hasRequestedPermission = true)
    }

    val requestPermission = {
        onPermissionRequestTracked()
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    DisposableEffect(lifecycleOwner, context, hasRequestedPermission) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionState = resolveCameraPermissionState(context, hasRequestedPermission)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Camera")
        Text(
            text = "Phase 1 Camera preview (C17)",
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )

        when (val state = permissionState) {
            CameraPermissionUiState.NotRequested -> {
                Text(text = "Camera permission has not been granted yet.")
                Button(
                    onClick = requestPermission,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                ) {
                    Text(text = "Request Camera Permission")
                }
            }

            is CameraPermissionUiState.Denied -> {
                if (state.canRequestAgain) {
                    Text(text = "Camera permission was denied.")
                    Text(
                        text = "Please allow camera access to continue.",
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Button(
                        onClick = requestPermission,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                    ) {
                        Text(text = "Request Again")
                    }
                } else {
                    Text(text = "Camera permission is permanently denied.")
                    Text(
                        text = "Enable camera permission in app settings, then return.",
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Button(
                        onClick = { openAppSettings(context) },
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                    ) {
                        Text(text = "Open App Settings")
                    }
                }
            }

            CameraPermissionUiState.Granted -> {
                Text(text = "Camera permission granted.")
                Text(
                    text = "Live preview is running.",
                    modifier = Modifier.padding(top = 4.dp)
                )
                CameraPreview(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    onFrameDiagnostics = onFrameDiagnostics
                )
            }
        }

        Button(onClick = onNavigateBack, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
            Text(text = "Back to Home")
        }
    }
}

@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    onFrameDiagnostics: (FrameDiagnostics) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val onFrameDiagnosticsState = rememberUpdatedState(onFrameDiagnostics)
    val frameAnalyzer = remember {
        FrameAnalyzer(
            onDiagnostics = { diagnostics ->
                onFrameDiagnosticsState.value(diagnostics)
            }
        )
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var imageAnalysis by remember { mutableStateOf<ImageAnalysis?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(analysisExecutor) {
        onDispose {
            analysisExecutor.shutdownSafely()
        }
    }

    DisposableEffect(lifecycleOwner, previewView, context, analysisExecutor, frameAnalyzer) {
        var disposed = false
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                if (disposed) {
                    return@addListener
                }
                try {
                    val provider = cameraProviderFuture.get()
                    val previewUseCase = Preview.Builder().build().also { preview ->
                        preview.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analysisUseCase = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { useCase ->
                            useCase.setAnalyzer(analysisExecutor, frameAnalyzer)
                        }

                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        previewUseCase,
                        analysisUseCase
                    )
                    cameraProvider = provider
                    imageAnalysis = analysisUseCase
                    errorMessage = null
                } catch (_: Exception) {
                    errorMessage = "Unable to start camera preview on this device."
                }
            },
            ContextCompat.getMainExecutor(context)
        )

        onDispose {
            disposed = true
            imageAnalysis?.clearAnalyzer()
            cameraProvider?.unbindAll()
            imageAnalysis = null
        }
    }

    if (errorMessage == null) {
        AndroidView(
            factory = { previewView },
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(text = errorMessage.orEmpty())
        }
    }
}

private fun resolveCameraPermissionState(
    context: Context,
    hasRequestedPermission: Boolean
): CameraPermissionUiState {
    val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
    if (granted) {
        return CameraPermissionUiState.Granted
    }
    if (!hasRequestedPermission) {
        return CameraPermissionUiState.NotRequested
    }

    val activity = context.findActivity()
    val canRequestAgain = activity?.let {
        ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
    } ?: false

    return CameraPermissionUiState.Denied(canRequestAgain = canRequestAgain)
}

private fun openAppSettings(context: Context) {
    val detailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(detailsIntent)
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(fallbackIntent)
        } catch (_: ActivityNotFoundException) {
            // No available settings handler on this device.
        }
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private fun ExecutorService.shutdownSafely() {
    if (!isShutdown) {
        shutdown()
    }
}

private sealed interface CameraPermissionUiState {
    object NotRequested : CameraPermissionUiState
    data class Denied(val canRequestAgain: Boolean) : CameraPermissionUiState
    object Granted : CameraPermissionUiState
}
