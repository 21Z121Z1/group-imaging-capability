package io.github.z121z1.watermarkcleaner.ui

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.z121z1.watermarkcleaner.platform.ColorOsSurfaceRole
import io.github.z121z1.watermarkcleaner.platform.ColorOsUiBridge
import io.github.z121z1.watermarkcleaner.platform.OplusViewSeamlessCompat
import kotlin.math.min

enum class ColorOsCornerProfile(val radius: Dp?, val weight: Float) {
    SMALL(14.dp, 3.6f),
    MEDIUM(18.dp, 3.8f),
    LARGE(24.dp, 4.0f),
    EXTRA_LARGE(32.dp, 4.2f),
    CAPSULE(null, 2.0f),
    CIRCLE(null, 2.0f),
}

private class ColorOsMaterialLayerView(
    context: Context,
    private val bridge: ColorOsUiBridge,
) : View(context) {
    private var role: ColorOsSurfaceRole? = null
    private var corner: ColorOsCornerProfile? = null

    init {
        setBackgroundColor(AndroidColor.TRANSPARENT)
        isClickable = false
        isFocusable = false
    }

    fun configure(newRole: ColorOsSurfaceRole, newCorner: ColorOsCornerProfile) {
        val changed = role != newRole || corner != newCorner
        role = newRole
        corner = newCorner
        if (changed) applyVendorStyle()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw || h != oldh) applyCorner()
    }

    private fun applyVendorStyle() {
        val currentRole = role ?: return
        bridge.applySurface(this, currentRole)
        applyCorner()
    }

    private fun applyCorner() {
        if (width <= 0 || height <= 0) return
        val profile = corner ?: return
        val radiusPx = when (profile) {
            ColorOsCornerProfile.CAPSULE,
            ColorOsCornerProfile.CIRCLE -> min(width, height) / 2f
            else -> (profile.radius ?: 24.dp).value * resources.displayMetrics.density
        }
        bridge.applySmoothCorner(this, radiusPx, profile.weight)
    }

    override fun onDetachedFromWindow() {
        bridge.clear(this)
        super.onDetachedFromWindow()
    }
}

private class ColorOsTextButtonView(
    context: Context,
    private val bridge: ColorOsUiBridge,
) : FrameLayout(context) {
    private val label = TextView(context).apply {
        gravity = Gravity.CENTER
        textSize = 14f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        isClickable = false
        isFocusable = false
        setPadding(dp(14), dp(8), dp(14), dp(8))
    }

    private var appliedRole: ColorOsSurfaceRole? = null
    private var currentEnabled = true

    init {
        setBackgroundColor(AndroidColor.TRANSPARENT)
        addView(label, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        isClickable = true
        isFocusable = true
    }

    fun configure(
        text: String,
        role: ColorOsSurfaceRole,
        enabled: Boolean,
        textColor: Int,
        click: () -> Unit,
    ) {
        label.text = text
        label.setTextColor(textColor)
        isEnabled = enabled
        alpha = if (enabled) 1f else 0.42f
        setOnClickListener(if (enabled) View.OnClickListener { click() } else null)
        if (appliedRole != role || currentEnabled != enabled) {
            appliedRole = role
            currentEnabled = enabled
            bridge.applySurface(this, role)
            applyCorner()
        }
        if (enabled) OplusViewSeamlessCompat.registerWhenLaidOut(this)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw || h != oldh) {
            applyCorner()
            if (isEnabled) OplusViewSeamlessCompat.registerWhenLaidOut(this)
        }
    }

    private fun applyCorner() {
        if (width <= 0 || height <= 0) return
        bridge.applySmoothCorner(this, min(width, height) / 2f, 2f)
    }

    override fun onDetachedFromWindow() {
        OplusViewSeamlessCompat.clear(this)
        bridge.clear(this)
        super.onDetachedFromWindow()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

/**
 * A production surface whose visual layer is ColorOS' actual material View.
 * Compose remains responsible for semantics/layout/content while the vendor
 * View provides blur, material stroke and SDF corner rendering underneath.
 */
@Composable
fun ColorOsMaterialSurface(
    modifier: Modifier = Modifier,
    role: ColorOsSurfaceRole = ColorOsSurfaceRole.CARD,
    corner: ColorOsCornerProfile = ColorOsCornerProfile.LARGE,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val activeBridge = LocalColorOsUiBridge.current?.takeIf { it.runtimeInfo.available }
    val radius = when (corner) {
        ColorOsCornerProfile.CAPSULE, ColorOsCornerProfile.CIRCLE -> 999.dp
        else -> corner.radius ?: 24.dp
    }
    val composeShape = RoundedCornerShape(radius)

    if (activeBridge == null) {
        Surface(
            modifier = modifier,
            shape = composeShape,
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
            tonalElevation = 1.dp,
        ) {
            Column(Modifier.padding(contentPadding), content = content)
        }
        return
    }

    Box(modifier.clip(composeShape)) {
        AndroidView(
            factory = { ColorOsMaterialLayerView(activeBridge.materialContext, activeBridge) },
            update = { it.configure(role, corner) },
            modifier = Modifier.fillMaxSize(),
        )
        Column(Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun ColorOsActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    role: ColorOsSurfaceRole = ColorOsSurfaceRole.PRIMARY_BUTTON,
    enabled: Boolean = true,
    fallbackOutlined: Boolean = false,
    onNativeView: ((View) -> Unit)? = null,
) {
    val activeBridge = LocalColorOsUiBridge.current?.takeIf { it.runtimeInfo.available }
    val textColor = when (role) {
        ColorOsSurfaceRole.PRIMARY_BUTTON, ColorOsSurfaceRole.CHIP_SELECTED -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    if (activeBridge == null) {
        if (fallbackOutlined) {
            OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier) { Text(text) }
        } else {
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = modifier,
                colors = ButtonDefaults.buttonColors(),
            ) { Text(text) }
        }
        return
    }

    val stableClick = remember(onClick) { onClick }
    AndroidView(
        factory = {
            ColorOsTextButtonView(activeBridge.materialContext, activeBridge).also { view ->
                onNativeView?.invoke(view)
            }
        },
        update = { view ->
            view.configure(
                text = text,
                role = role,
                enabled = enabled,
                textColor = textColor.toArgb(),
                click = stableClick,
            )
            onNativeView?.invoke(view)
        },
        modifier = modifier,
    )
}
