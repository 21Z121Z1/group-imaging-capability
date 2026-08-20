package dev.groupimaging.unmark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.groupimaging.unmark.ui.UnmarkApp
import dev.groupimaging.unmark.ui.UnmarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            UnmarkTheme {
                UnmarkApp()
            }
        }
    }
}
