package com.oplus.groupimaging.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class OplusInsightShapes(
    val heroCorner: Dp = 32.dp,
    val cardCorner: Dp = 24.dp,
    val chipCorner: Dp = 999.dp,
)

@Immutable
data class OplusInsightSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
)

@Immutable
data class OplusInsightElevation(
    val card: Dp = 2.dp,
    val hero: Dp = 6.dp,
    val sheet: Dp = 8.dp,
)

@Immutable
data class OplusInsightMotion(
    val quickMillis: Int = 180,
    val regularMillis: Int = 260,
    val emphasisMillis: Int = 380,
)

@Immutable
data class OplusInsightChartColors(
    val series: List<Color>,
)

@Immutable
data class OplusInsightDangerSemantics(
    val danger: Color,
    val dangerContainer: Color,
    val onDanger: Color,
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1F6A57),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC1F0DE),
    secondary = Color(0xFF525F7A),
    background = Color(0xFFF7F3EA),
    surface = Color(0xFFFFFBF5),
    surfaceVariant = Color(0xFFE7E0D3),
    outline = Color(0xFF82796B),
    error = Color(0xFFB3261E),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8ED4BA),
    onPrimary = Color(0xFF00382B),
    primaryContainer = Color(0xFF00513E),
    secondary = Color(0xFFBCC7E8),
    background = Color(0xFF121412),
    surface = Color(0xFF191C19),
    surfaceVariant = Color(0xFF3D443D),
    outline = Color(0xFF89938A),
    error = Color(0xFFF2B8B5),
)

val LocalOplusInsightShapes = staticCompositionLocalOf { OplusInsightShapes() }
val LocalOplusInsightSpacing = staticCompositionLocalOf { OplusInsightSpacing() }
val LocalOplusInsightElevation = staticCompositionLocalOf { OplusInsightElevation() }
val LocalOplusInsightMotion = staticCompositionLocalOf { OplusInsightMotion() }
val LocalOplusInsightChartColors = staticCompositionLocalOf {
    OplusInsightChartColors(
        series = listOf(
            Color(0xFF1F6A57),
            Color(0xFF5B7CFA),
            Color(0xFFE89F43),
            Color(0xFFB45BD4),
            Color(0xFFE06464),
            Color(0xFF5A8E2F),
        ),
    )
}
val LocalOplusInsightDanger = staticCompositionLocalOf {
    OplusInsightDangerSemantics(
        danger = Color(0xFFB3261E),
        dangerContainer = Color(0xFFF9DEDC),
        onDanger = Color(0xFFFFFFFF),
    )
}

object OplusInsightTheme {
    val shapes: OplusInsightShapes
        @Composable get() = LocalOplusInsightShapes.current
    val spacing: OplusInsightSpacing
        @Composable get() = LocalOplusInsightSpacing.current
    val elevation: OplusInsightElevation
        @Composable get() = LocalOplusInsightElevation.current
    val motion: OplusInsightMotion
        @Composable get() = LocalOplusInsightMotion.current
    val chartColors: OplusInsightChartColors
        @Composable get() = LocalOplusInsightChartColors.current
    val danger: OplusInsightDangerSemantics
        @Composable get() = LocalOplusInsightDanger.current
}

@Composable
fun OplusInsightTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme: ColorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
            dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(context)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = expressiveTypography(),
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalOplusInsightShapes provides OplusInsightShapes(),
            LocalOplusInsightSpacing provides OplusInsightSpacing(),
            LocalOplusInsightElevation provides OplusInsightElevation(),
            LocalOplusInsightMotion provides OplusInsightMotion(),
            LocalOplusInsightChartColors provides LocalOplusInsightChartColors.current,
            LocalOplusInsightDanger provides LocalOplusInsightDanger.current,
            content = content,
        )
    }
}

@Composable
private fun expressiveTypography() = androidx.compose.material3.Typography().copy(
    displayLarge = TextStyle(fontFamily = FontFamily.Serif),
    displayMedium = TextStyle(fontFamily = FontFamily.Serif),
    headlineLarge = TextStyle(fontFamily = FontFamily.Serif),
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif),
    labelMedium = TextStyle(fontFamily = FontFamily.Monospace),
)
