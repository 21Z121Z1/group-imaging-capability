package io.github.z121z1.watermarkcleaner

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import io.github.z121z1.watermarkcleaner.core.CalibrationOrientation
import io.github.z121z1.watermarkcleaner.ui.WatermarkCleanerApp
import io.github.z121z1.watermarkcleaner.ui.WatermarkCleanerTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        acceptShareIntent(intent)
        lifecycleScope.launch {
            viewModel.state
                .map { state -> state.calibration.takeIf { it.active }?.target?.orientation }
                .distinctUntilChanged()
                .collect { orientation ->
                    requestedOrientation = when (orientation) {
                        CalibrationOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        CalibrationOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        null -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                }
        }
        setContent {
            WatermarkCleanerTheme {
                WatermarkCleanerApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptShareIntent(intent)
    }

    @Suppress("DEPRECATION")
    private fun acceptShareIntent(intent: Intent?) {
        if (intent?.type?.startsWith("image/") != true) return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                if (uri != null) viewModel.enqueue(listOf(uri))
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
                viewModel.enqueue(uris)
            }
        }
    }
}
