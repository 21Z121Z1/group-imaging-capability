package io.github.z121z1.watermarkcleaner.platform

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.view.View
import java.lang.reflect.Proxy
import java.util.WeakHashMap

/**
 * ColorOS 17 framework seamless-view registration.
 *
 * The contract below is recovered from ColorOS Settings 17.0.0's
 * COUIFloatingButtonSeamlessImpl, which delegates to the framework class
 * com.oplus.animation.OplusViewSeamless. The parameter array and bundle keys
 * intentionally match the system implementation rather than approximating it.
 *
 * Note that ColorOS material Views are intentionally constructed with the
 * foreign UXDesign package Context. That Context cannot resolve our host
 * Activity, so callers must explicitly provide the host Context when
 * registering seamless animation.
 */
object OplusViewSeamlessCompat {
    private const val CLASS_NAME = "com.oplus.animation.OplusViewSeamless"
    private const val CALLBACK_NAME = "com.oplus.animation.OplusViewSeamless\$AnimationCallback"

    private const val KEY_COLOR = "view_seamless_color"
    private const val KEY_OPEN = "view_seamless_open"
    private const val KEY_RADIUS = "view_seamless_radius"
    private const val KEY_PARAM = "view_seamless_param"

    private val SYSTEM_PARAMS = floatArrayOf(400f, 1.1f, 250f, 0.85f)
    private val callbacks = WeakHashMap<View, Any>()
    private val registeredWidths = WeakHashMap<View, Int>()

    fun isAvailable(): Boolean = runCatching {
        Class.forName(CLASS_NAME)
        Class.forName(CALLBACK_NAME)
    }.isSuccess

    fun register(view: View, hostContext: Context, color: Int = 0): Result<Boolean> = runCatching {
        if (view.width <= 0 || !view.isAttachedToWindow) return@runCatching false
        if (registeredWidths[view] == view.width) return@runCatching true

        val activity = findActivity(hostContext) ?: return@runCatching false
        val seamlessClass = Class.forName(CLASS_NAME)
        val callbackClass = Class.forName(CALLBACK_NAME)
        val callback = Proxy.newProxyInstance(
            callbackClass.classLoader,
            arrayOf(callbackClass),
        ) { _, method, _ ->
            when (method.name) {
                "onAnimationEnd" -> null
                "toString" -> "WatermarkCleanerOplusSeamlessCallback"
                "hashCode" -> System.identityHashCode(view)
                "equals" -> false
                else -> null
            }
        }

        val params = Bundle().apply {
            if (color != 0) putInt(KEY_COLOR, color)
            putBoolean(KEY_OPEN, true)
            putFloat(KEY_RADIUS, view.width / 2f)
            putFloatArray(KEY_PARAM, SYSTEM_PARAMS.clone())
        }

        val method = seamlessClass.getDeclaredMethod(
            "setSeamlessView",
            View::class.java,
            Context::class.java,
            Bundle::class.java,
            callbackClass,
        ).apply { isAccessible = true }

        val accepted = method.invoke(null, view, activity, params, callback) as? Boolean ?: false
        if (accepted) {
            callbacks[view] = callback
            registeredWidths[view] = view.width
        }
        accepted
    }

    fun registerWhenLaidOut(view: View, hostContext: Context, color: Int = 0) {
        if (view.width > 0 && view.isAttachedToWindow) {
            register(view, hostContext, color)
        } else {
            view.post { register(view, hostContext, color) }
        }
    }

    fun clear(view: View) {
        callbacks.remove(view)
        registeredWidths.remove(view)
    }

    private fun findActivity(context: Context?): Activity? {
        var current = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return current as? Activity
    }
}
