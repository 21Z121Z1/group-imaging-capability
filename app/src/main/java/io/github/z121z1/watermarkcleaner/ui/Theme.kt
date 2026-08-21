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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.z121z1.watermarkcleaner.platform.ColorOsUiBridge

val LocalColorOsUiBridge = staticCompositionLocalOf<ColorOsUiBridge?> { null }

@Composable
fun WatermarkCleanerTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val colorOs = remember(context.applicationContext) { ColorOsUiBridge(context.applicationContext) }

    val base = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        dark -> darkColorScheme(
            primary = Color(0xFFAFC6FF),
            surface = Color(0xFF111318),
            surfaceContainer = Color(0xFF1D2026),
        )
        else -> expressiveLightColorScheme()
    }

    val vendorPalette = remember(colorOs, dark, configuration.uiMode, configuration.colorMode) {
        if (colorOs.runtimeInfo.available) colorOs.palette() else null
    }

    /*
     * UXDesign's package-local surface/label colors are authored for COUI's own
     * themed Context. Reading every role through a foreign package Context can
     * produce a dark surface with a light-theme host (or the inverse). That is
     * visually invalid even though the resource lookup technically succeeds.
     *
     * Keep Android's host semantic colors for background/surface/text contrast,
     * and import only the ColorOS control accent + divider roles. Vendor Views
     * still render the real ColorOS material; Compose remains responsible for
     * accessible foreground/background semantics.
     */
    val colorScheme = vendorPalette?.let { palette ->
        base.copy(
            primary = Color(palette.primary),
            outline = Color(palette.divider),
            outlineVariant = Color(palette.divider),
        )
    } ?: base

    // Compose uses these radii for fallback layout. On ColorOS the visible edge
    // is additionally produced by OplusMaterialCornerParams' SDF/G2 profile.
    val shapes = Shapes(
        medium = RoundedCornerShape(18.dp),
        large = RoundedCornerShape(24.dp),
        largeIncreased = RoundedCornerShape(30.dp),
        extraLarge = RoundedCornerShape(36.dp),
    )

    CompositionLocalProvider(LocalColorOsUiBridge provides colorOs) {
        MaterialExpressiveTheme(colorScheme = colorScheme, shapes = shapes, content = content)
    }
}
