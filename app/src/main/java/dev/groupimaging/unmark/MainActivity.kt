package dev.groupimaging.unmark

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.groupimaging.unmark.ui.UnmarkApp
import dev.groupimaging.unmark.ui.UnmarkTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: UnmarkViewModel by viewModels()

    private val processPicker = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(20),
    ) { uris -> viewModel.addImages(uris) }

    private val calibrationPicker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> viewModel.onCalibrationImageSelected(uri) }

    private val outputTreePicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching { contentResolver.takePersistableUriPermission(uri, flags) }
                .onSuccess { viewModel.setOutputTree(uri) }
                .onFailure { viewModel.setOutputTree(null) }
        }
    }

    private val mediaPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.refreshMediaAccess() }

    private val captureCallback = Activity.ScreenCaptureCallback {
        viewModel.onSystemScreenshotDetected()
    }
    private var captureCallbackRegistered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleIncomingIntent(intent)
        collectEffects()
        collectCaptureLifecycle()

        setContent {
            UnmarkTheme {
                UnmarkApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun collectEffects() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        UnmarkViewModel.Effect.PickProcessingImages -> processPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                        UnmarkViewModel.Effect.PickCalibrationImage -> calibrationPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                        UnmarkViewModel.Effect.PickOutputDirectory -> outputTreePicker.launch(
                            viewModel.uiState.value.outputTree,
                        )
                        UnmarkViewModel.Effect.RequestFullMediaAccess -> mediaPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun collectCaptureLifecycle() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    viewModel.uiState
                        .map { state ->
                            (state.calibration as? UnmarkViewModel.CalibrationState.Capturing)?.mode
                        }
                        .distinctUntilChanged()
                        .collect(::setCaptureMode)
                } finally {
                    setCaptureMode(null)
                }
            }
        }
    }

    private fun setCaptureMode(mode: UnmarkViewModel.CalibrationMode?) {
        val active = mode != null
        if (active && !captureCallbackRegistered) {
            registerScreenCaptureCallback(mainExecutor, captureCallback)
            captureCallbackRegistered = true
        } else if (!active && captureCallbackRegistered) {
            unregisterScreenCaptureCallback(captureCallback)
            captureCallbackRegistered = false
        }
        updateCalibrationChrome(active = active, hdr = mode == UnmarkViewModel.CalibrationMode.HDR)
    }

    private fun updateCalibrationChrome(active: Boolean, hdr: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        window.colorMode = if (active && hdr) {
            ActivityInfo.COLOR_MODE_HDR
        } else {
            ActivityInfo.COLOR_MODE_DEFAULT
        }

        if (active) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun handleIncomingIntent(incoming: Intent) {
        if (incoming.action != Intent.ACTION_SEND && incoming.action != Intent.ACTION_SEND_MULTIPLE) return
        if (incoming.type?.startsWith("image/") != true) return

        val uris = LinkedHashSet<Uri>()
        incoming.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) clip.getItemAt(i).uri?.let(uris::add)
        }
        if (incoming.action == Intent.ACTION_SEND) {
            incoming.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)?.let(uris::add)
        } else {
            incoming.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)?.let(uris::addAll)
        }
        viewModel.addImages(uris.toList())
    }
}
