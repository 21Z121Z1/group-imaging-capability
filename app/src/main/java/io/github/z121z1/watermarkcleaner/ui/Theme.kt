package io.github.z121z1.watermarkcleaner.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun WatermarkCleanerTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        dark -> darkColorScheme(
            primary = Color(0xFFAFC6FF),
            surface = Color(0xFF111318),
            surfaceContainer = Color(0xFF1D2026),
        )
        else -> expressiveLightColorScheme()
    }
    val shapes = Shapes(
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(28.dp),
        largeIncreased = RoundedCornerShape(36.dp),
        extraLarge = RoundedCornerShape(44.dp),
    )
    MaterialExpressiveTheme(colorScheme = colorScheme, shapes = shapes, content = content)
}
