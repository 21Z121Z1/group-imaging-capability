package io.github.z121z1.watermarkcleaner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import io.github.z121z1.watermarkcleaner.platform.ColorOsPredictiveBackDiagnostics
import io.github.z121z1.watermarkcleaner.platform.OplusViewSeamlessCompat
import io.github.z121z1.watermarkcleaner.ui.WatermarkCleanerApp
import io.github.z121z1.watermarkcleaner.ui.WatermarkCleanerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        acceptShareIntent(intent)

        if (BuildConfig.DEBUG) {
            val predictive = ColorOsPredictiveBackDiagnostics.inspect(this)
            Log.i(
                "WatermarkCleaner/ColorOS",
                "predictiveContinuous=${predictive.predictiveContinuousProperty} " +
                    "thirdPartyPredictive=${predictive.thirdPartyPredictiveBackSetting} " +
                    "oplusViewSeamless=${OplusViewSeamlessCompat.isAvailable()}",
            )
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
