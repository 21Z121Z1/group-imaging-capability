package io.github.z121z1.colorosmaterialplayground

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorSpace
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.view.Window
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.min

class MaterialHostView(context: android.content.Context) : FrameLayout(context) {
    private val label = TextView(context).apply {
        setTextColor(Color.WHITE)
        textSize = 11f
        gravity = Gravity.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.MEDIUM)
        setShadowLayer(3f, 0f, 1f, Color.argb(140, 0, 0, 0))
        setPadding(dp(12), dp(8), dp(12), dp(8))
    }

    var shapeKind: MaterialShapeKind = MaterialShapeKind.Generic
        set(value) {
            field = value
            outlineProvider = shapeOutlineProvider(value)
            invalidateOutline()
        }

    init {
        setWillNotDraw(false)
        background = ColorDrawable(Color.TRANSPARENT)
        isClickable = true
        isFocusable = true
        addView(
            label,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
        )
        shapeKind = MaterialShapeKind.Generic
    }

    fun setLabel(text: String) {
        label.text = text
    }

    private fun shapeOutlineProvider(kind: MaterialShapeKind) = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            val w = view.width.coerceAtLeast(1)
            val h = view.height.coerceAtLeast(1)
            val radius = when (kind) {
                MaterialShapeKind.Circle -> min(w, h) / 2f
                MaterialShapeKind.Capsule -> h / 2f
                MaterialShapeKind.RoundRect -> dp(28).toFloat()
                MaterialShapeKind.Bar -> dp(26).toFloat()
                MaterialShapeKind.Generic -> dp(22).toFloat()
            }
            outline.setRoundRect(0, 0, w, h, radius)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

class DemoBackdropView(context: android.content.Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val gradient = LinearGradient(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            intArrayOf(
                Color.rgb(22, 38, 83),
                Color.rgb(119, 75, 162),
                Color.rgb(32, 150, 194),
                Color.rgb(247, 183, 51),
            ),
            floatArrayOf(0f, 0.34f, 0.7f, 1f),
            Shader.TileMode.CLAMP,
        )
        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        paint.color = Color.argb(170, 255, 255, 255)
        val r = min(width, height) * 0.16f
        canvas.drawCircle(width * 0.20f, height * 0.26f, r, paint)
        paint.color = Color.argb(180, 255, 80, 110)
        canvas.drawCircle(width * 0.82f, height * 0.42f, r * 1.15f, paint)
        paint.color = Color.argb(175, 70, 245, 210)
        canvas.drawCircle(width * 0.36f, height * 0.78f, r * 0.95f, paint)
    }
}

/**
 * Extended-sRGB test pattern. Values above 1.0 are intentionally emitted as
 * ColorLongs so an HDR-capable HWUI window can preserve highlight headroom.
 */
class HdrTestPatternView(context: android.content.Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 34f * resources.displayMetrics.scaledDensity
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val extendedSrgb = ColorSpace.get(ColorSpace.Named.EXTENDED_SRGB)
    private val levels = floatArrayOf(1f, 1.35f, 1.75f, 2.2f)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(10, 12, 18))
        val stripeWidth = width / levels.size.toFloat()
        levels.forEachIndexed { index, level ->
            val colorLong = Color.pack(level, level * 0.97f, level * 0.88f, 1f, extendedSrgb)
            paint.setColor(colorLong)
            val left = stripeWidth * index
            canvas.drawRect(left, 0f, left + stripeWidth, height.toFloat(), paint)
        }
        paint.color = Color.argb(150, 0, 0, 0)
        canvas.drawRect(0f, height * 0.76f, width.toFloat(), height.toFloat(), paint)
        levels.forEachIndexed { index, level ->
            val left = stripeWidth * index + stripeWidth * 0.08f
            canvas.drawText("${level}×", left, height * 0.91f, textPaint)
        }
    }
}

data class HdrWindowState(
    val requestedHdr: Boolean,
    val photoHasGainmap: Boolean,
    val photoWideGamut: Boolean,
    val headroom: Float,
)

fun updateHdrWindow(
    window: Window,
    bitmap: Bitmap?,
    forceHdr: Boolean,
    hdrPattern: Boolean,
    desiredHeadroom: Float,
): HdrWindowState {
    val gainmap = bitmap?.hasGainmap() == true
    val wide = bitmap?.colorSpace?.isWideGamut == true
    val requestHdr = forceHdr || hdrPattern || gainmap

    window.colorMode = when {
        requestHdr -> ActivityInfo.COLOR_MODE_HDR
        wide -> ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT
        else -> ActivityInfo.COLOR_MODE_DEFAULT
    }
    if (Build.VERSION.SDK_INT >= 35) {
        window.desiredHdrHeadroom = if (requestHdr) desiredHeadroom.coerceAtLeast(1f) else 0f
    }
    return HdrWindowState(
        requestedHdr = requestHdr,
        photoHasGainmap = gainmap,
        photoWideGamut = wide,
        headroom = if (requestHdr) desiredHeadroom else 1f,
    )
}

fun hdrDisplaySummary(activity: Activity): List<String> {
    val display = activity.display
    val caps = display?.hdrCapabilities
    val types = caps?.supportedHdrTypes?.joinToString(prefix = "[", postfix = "]") ?: "[]"
    return listOf(
        "screen wide gamut: ${activity.resources.configuration.isScreenWideColorGamut}",
        "window wide gamut: ${activity.window.isWideColorGamut}",
        "HDR types: $types",
        "hardware accelerated: ${activity.window.decorView.isHardwareAccelerated}",
    )
}
