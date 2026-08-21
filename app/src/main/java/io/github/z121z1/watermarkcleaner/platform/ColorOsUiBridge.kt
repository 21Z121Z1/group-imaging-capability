package io.github.z121z1.watermarkcleaner.platform

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import java.lang.reflect.Modifier
import java.util.WeakHashMap

enum class ColorOsSurfaceRole {
    CARD,
    TOP_BAR,
    BOTTOM_BAR,
    PRIMARY_BUTTON,
    SECONDARY_BUTTON,
    TRANSPARENT_BUTTON,
    CHIP,
    CHIP_SELECTED,
    CIRCLE_ACTION,
    NAVIGATION,
}

data class ColorOsMaterialSpec(
    val blur: String? = null,
    val stroke: String? = null,
    val spotLight: String? = null,
    val toolbarCategory: String? = null,
)

data class ColorOsPalette(
    val background: Int,
    val surface: Int,
    val card: Int,
    val bar: Int,
    val primary: Int,
    val onPrimary: Int,
    val labelPrimary: Int,
    val labelSecondary: Int,
    val divider: Int,
)

data class ColorOsRuntimeInfo(
    val available: Boolean,
    val installedVersion: String,
    val smoothCornerAvailable: Boolean,
    val transitionAvailable: Boolean,
    val paletteAvailable: Boolean,
)

/**
 * Production bridge for ColorOS 17 UXDesign. It only exposes capabilities that
 * were recovered from com.oplus.uxdesign 17.0.11 and verified by the standalone
 * material playground. No vendor bytecode is bundled into this app.
 */
class ColorOsUiBridge(appContext: Context) {
    companion object {
        const val UX_PACKAGE = "com.oplus.uxdesign"

        private const val BLUR_CLASS = "com.coui.appcompat.COUIMaterialBlurEffect"
        private const val BLUR_ENUM = "com.coui.appcompat.COUIMaterialBlurEffect\$BlurEffectType"
        private const val STROKE_CLASS = "com.coui.appcompat.COUIMaterialStrokeEffect"
        private const val STROKE_ENUM = "com.coui.appcompat.COUIMaterialStrokeEffect\$StrokeEffectType"
        private const val SPOTLIGHT_ENUM = "com.coui.appcompat.spotlight.COUISpotLightEffect\$SpotLightType"
        private const val SPOTLIGHT_DRAWABLE = "com.coui.appcompat.spotlight.a"
        private const val TOOLBAR_DELEGATE = "com.coui.appcompat.toolbar.ToolbarMaterialEffectDelegate"
        private const val TOOLBAR_CATEGORY = "com.coui.appcompat.toolbar.ToolbarMaterialEffectDelegate\$ViewCategory"
        private const val CORNER_PARAMS = "com.oplus.view.material.OplusMaterialCornerParams"
        private const val MATERIAL_UTIL = "com.oplus.view.material.OplusMaterialUtil"
        private const val GROUP_TRANSITION = "com.coui.appcompat.toolbar.ToolbarGroupTransitionController"
    }

    private val hostContext = appContext.applicationContext

    @Suppress("DEPRECATION")
    val materialContext: Context = runCatching {
        hostContext.createPackageContext(
            UX_PACKAGE,
            Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY,
        )
    }.getOrElse { hostContext }

    private val loader: ClassLoader = materialContext.classLoader
    private val retained = WeakHashMap<View, MutableList<Any>>()

    val runtimeInfo: ColorOsRuntimeInfo by lazy {
        val codeLoaded = materialContext.packageName == UX_PACKAGE && runCatching { load(BLUR_CLASS) }.isSuccess
        ColorOsRuntimeInfo(
            available = codeLoaded,
            installedVersion = runCatching {
                val info = hostContext.packageManager.getPackageInfo(UX_PACKAGE, 0)
                "${info.versionName ?: "?"} (${info.longVersionCode})"
            }.getOrElse { "not installed / not visible" },
            smoothCornerAvailable = codeLoaded && runCatching { load(CORNER_PARAMS); load(MATERIAL_UTIL) }.isSuccess,
            transitionAvailable = codeLoaded && runCatching { load(GROUP_TRANSITION) }.isSuccess,
            paletteAvailable = codeLoaded && palette() != null,
        )
    }

    fun spec(role: ColorOsSurfaceRole): ColorOsMaterialSpec = when (role) {
        ColorOsSurfaceRole.CARD -> ColorOsMaterialSpec(
            blur = "TYPE_FRAMEWORK_TOP_BAR_BLUR",
            spotLight = "TYPE_TRANSLUCENT_LARGE_1",
        )
        ColorOsSurfaceRole.TOP_BAR -> ColorOsMaterialSpec(
            blur = "TYPE_FRAMEWORK_TOP_BAR_BLUR",
            spotLight = "TYPE_TRANSLUCENT_LARGE_2",
        )
        ColorOsSurfaceRole.BOTTOM_BAR -> ColorOsMaterialSpec(
            blur = "TYPE_FRAMEWORK_BOTTOM_BAR",
            stroke = "TYPE_FRAMEWORK_CAPSULE_6",
            spotLight = "TYPE_BOTTOM_NAVIGATION",
        )
        ColorOsSurfaceRole.PRIMARY_BUTTON -> ColorOsMaterialSpec(
            blur = "TYPE_CONTENT_SEGMENT_BUTTON",
            stroke = "TYPE_CONTENT_CAPSULE_3",
            spotLight = "TYPE_SEGMENT_BUTTON",
        )
        ColorOsSurfaceRole.SECONDARY_BUTTON -> ColorOsMaterialSpec(
            blur = "TYPE_CONTENT_SECONDARY_BUTTON",
            stroke = "TYPE_CONTENT_CAPSULE_4",
            spotLight = "TYPE_CAPSULE_1",
        )
        ColorOsSurfaceRole.TRANSPARENT_BUTTON -> ColorOsMaterialSpec(
            blur = "TYPE_CONTENT_TRANSPARENT_BUTTON",
            stroke = "TYPE_CONTENT_CAPSULE_2",
            spotLight = "TYPE_TRANSPARENT_MEDIUM_1",
        )
        ColorOsSurfaceRole.CHIP -> ColorOsMaterialSpec(
            blur = "TYPE_CONTENT_CHIP_TAB",
            stroke = "TYPE_CONTENT_CAPSULE_1",
            spotLight = "TYPE_CHIP_TAB",
        )
        ColorOsSurfaceRole.CHIP_SELECTED -> ColorOsMaterialSpec(
            blur = "TYPE_CONTENT_CHIP_TAB_SELECTED",
            stroke = "TYPE_CONTENT_CAPSULE_5",
            spotLight = "TYPE_CHIP_TAB",
        )
        ColorOsSurfaceRole.CIRCLE_ACTION -> ColorOsMaterialSpec(
            blur = "TYPE_CONTENT_TRANSPARENT_BUTTON",
            stroke = "TYPE_FRAMEWORK_CIRCLE_1",
            spotLight = "TYPE_CIRCLE_1",
        )
        ColorOsSurfaceRole.NAVIGATION -> ColorOsMaterialSpec(
            toolbarCategory = "TOOLBAR_BUTTON",
        )
    }

    fun applySurface(view: View, role: ColorOsSurfaceRole): Result<Unit> = runCatching {
        val spec = spec(role)
        clear(view)
        spec.blur?.let { applyCompanionPreset(BLUR_CLASS, BLUR_ENUM, view, it).getOrThrow() }
        spec.stroke?.let { applyCompanionPreset(STROKE_CLASS, STROKE_ENUM, view, it).getOrThrow() }
        spec.spotLight?.let { applySpotLight(view, it).getOrThrow() }
        spec.toolbarCategory?.let {
            applyToolbarStack(view, it).getOrThrow()
        }
    }

    /**
     * ColorOS 17 SDF/G2 corner path recovered from OplusMaterialCornerParams.
     * radius is expressed in physical pixels. weight controls the SDF exponent;
     * 4.0 gives the squircle-like profile used by the app for large containers.
     */
    fun applySmoothCorner(view: View, radiusPx: Float, weight: Float): Result<Unit> = runCatching {
        val paramsClass = load(CORNER_PARAMS)
        val params = paramsClass.getDeclaredConstructor(
            Float::class.javaPrimitiveType!!,
            Float::class.javaPrimitiveType!!,
        ).newInstance(radiusPx, weight)
        val utilClass = load(MATERIAL_UTIL)
        val method = utilClass.getDeclaredMethod("setCornerParams", View::class.java, paramsClass)
        method.isAccessible = true
        val ok = method.invoke(null, view, params) as? Boolean ?: true
        check(ok) { "ColorOS rejected smooth corner params" }
        retain(view, params)
    }

    /**
     * Uses ColorOS' ToolbarGroupTransitionController. The vendor controller
     * drives alpha/scale/blur spring channels together, which is the same
     * seamless group transition path used by COUI toolbars.
     */
    fun animateGroup(views: List<View>, entering: Boolean, onEnd: (() -> Unit)? = null): Result<Unit> = runCatching {
        if (views.isEmpty()) {
            onEnd?.invoke()
            return@runCatching
        }
        val controllerClass = load(GROUP_TRANSITION)
        val controller = controllerClass.getDeclaredConstructor().apply { isAccessible = true }.newInstance()
        val method = controllerClass.getDeclaredMethod(
            "startTransition",
            List::class.java,
            Boolean::class.javaPrimitiveType!!,
            Runnable::class.java,
        ).apply { isAccessible = true }
        method.invoke(controller, views, entering, Runnable { onEnd?.invoke() })
        views.forEach { retain(it, controller) }
    }

    fun palette(): ColorOsPalette? = runCatching {
        fun c(name: String): Int {
            val id = materialContext.resources.getIdentifier(name, "color", UX_PACKAGE)
            require(id != 0) { "ColorOS color resource missing: $name" }
            return materialContext.resources.getColor(id, materialContext.theme)
        }
        ColorOsPalette(
            background = c("coui_color_background"),
            surface = c("coui_color_surface"),
            card = c("coui_color_card"),
            bar = c("coui_color_bar"),
            primary = c("coui_color_controls"),
            onPrimary = c("coui_color_on_primary"),
            labelPrimary = c("coui_color_label_primary"),
            labelSecondary = c("coui_color_label_secondary"),
            divider = c("coui_color_divider"),
        )
    }.getOrNull()

    fun clear(view: View) {
        runCatching { applyCompanionPreset(BLUR_CLASS, BLUR_ENUM, view, "TYPE_NO_EFFECT").getOrThrow() }
        runCatching { applyCompanionPreset(STROKE_CLASS, STROKE_ENUM, view, "TYPE_NO_EFFECT").getOrThrow() }
        retained.remove(view)?.forEach { retainedObject ->
            runCatching {
                retainedObject.javaClass.declaredMethods
                    .firstOrNull { it.name == "removeAllEffects" && it.parameterCount == 0 }
                    ?.apply { isAccessible = true }
                    ?.invoke(retainedObject)
            }
            runCatching {
                retainedObject.javaClass.declaredMethods
                    .firstOrNull { it.name == "release" && it.parameterCount == 0 }
                    ?.apply { isAccessible = true }
                    ?.invoke(retainedObject)
            }
        }
        view.foreground = null
        view.setOnTouchListener(null)
    }

    fun diagnostics(): List<String> = listOf(
        "uxdesign=${runtimeInfo.installedVersion}",
        "runtime=${runtimeInfo.available}",
        "sdfCorner=${runtimeInfo.smoothCornerAvailable}",
        "groupTransition=${runtimeInfo.transitionAvailable}",
        "palette=${runtimeInfo.paletteAvailable}",
    )

    private fun applySpotLight(view: View, typeName: String): Result<Unit> = runCatching {
        val enumClass = load(SPOTLIGHT_ENUM)
        val constant = enumConstant(enumClass, typeName)
            ?: error("Unknown ColorOS spotlight preset: $typeName")
        val drawableClass = load(SPOTLIGHT_DRAWABLE)
        val drawable = drawableClass.getDeclaredConstructor().apply { isAccessible = true }.newInstance()
        require(drawable is Drawable) { "$SPOTLIGHT_DRAWABLE is not a Drawable" }

        val configure = drawableClass.declaredMethods.firstOrNull { method ->
            val p = method.parameterTypes
            p.size == 2 && Context::class.java.isAssignableFrom(p[0]) && p[1] == enumClass
        } ?: error("ColorOS spotlight configure method not found")
        configure.isAccessible = true
        val configured = configure.invoke(drawable, materialContext, constant) as? Drawable ?: drawable
        val motionMethod = drawableClass.declaredMethods.firstOrNull { method ->
            val p = method.parameterTypes
            p.size == 1 && p[0] == MotionEvent::class.java && method.returnType == Void.TYPE
        }?.apply { isAccessible = true }

        view.foreground = configured
        view.setOnTouchListener { touchedView, event ->
            configured.setHotspot(event.x, event.y)
            runCatching { motionMethod?.invoke(configured, event) }
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> touchedView.isPressed = true
                MotionEvent.ACTION_UP -> {
                    touchedView.isPressed = false
                    touchedView.performClick()
                }
                MotionEvent.ACTION_CANCEL -> touchedView.isPressed = false
            }
            true
        }
        retain(view, configured)
    }

    private fun applyToolbarStack(view: View, categoryName: String): Result<Unit> = runCatching {
        val delegateClass = load(TOOLBAR_DELEGATE)
        val categoryClass = load(TOOLBAR_CATEGORY)
        val category = enumConstant(categoryClass, categoryName)
            ?: error("Unknown ColorOS toolbar category: $categoryName")
        val create = delegateClass.declaredMethods.firstOrNull { method ->
            val p = method.parameterTypes
            method.name == "create" && p.size == 2 && View::class.java.isAssignableFrom(p[0]) && p[1] == categoryClass
        } ?: error("ToolbarMaterialEffectDelegate.create not found")
        create.isAccessible = true
        val receiver = if (Modifier.isStatic(create.modifiers)) null else {
            delegateClass.getDeclaredConstructor().apply { isAccessible = true }.newInstance()
        }
        val delegate = create.invoke(receiver, view, category)
            ?: error("ColorOS toolbar delegate returned null")
        val forceValue = runCatching {
            delegateClass.getDeclaredField("MATERIAL_EFFECT_FORCE_ENABLE").apply { isAccessible = true }.getInt(null)
        }.getOrDefault(2)
        invokeNamed(delegate, "setUseMaterialEffectAttr", forceValue)
        invokeNamed(delegate, "setEnabled", true)
        invokeNamed(delegate, "setBlurOn", true)
        invokeNamed(delegate, "setStrokeOn", true)
        invokeNamed(delegate, "setSpotLightOn", true)
        invokeNamed(delegate, "setCausticShadowOn", true)
        invokeNamed(delegate, "reapplyAll")
        retain(view, delegate)
    }

    private fun applyCompanionPreset(
        effectClassName: String,
        enumClassName: String,
        view: View,
        typeName: String,
    ): Result<Unit> = runCatching {
        val effectClass = load(effectClassName)
        val enumClass = load(enumClassName)
        val constant = enumConstant(enumClass, typeName)
            ?: error("Unknown ColorOS material preset: $typeName")
        val companion = effectClass.getDeclaredField("Companion").apply { isAccessible = true }.get(null)
            ?: error("$effectClassName.Companion is null")
        val apply = companion.javaClass.declaredMethods.firstOrNull { method ->
            val p = method.parameterTypes
            p.size == 2 && View::class.java.isAssignableFrom(p[0]) && p[1] == enumClass && method.returnType == Void.TYPE
        } ?: error("Preset apply method not found for $effectClassName")
        apply.isAccessible = true
        apply.invoke(companion, view, constant)
    }

    private fun retain(view: View, value: Any) {
        retained.getOrPut(view) { mutableListOf() }.add(value)
    }

    private fun invokeNamed(receiver: Any, name: String, vararg args: Any) {
        val method = receiver.javaClass.declaredMethods.firstOrNull { it.name == name && it.parameterCount == args.size }
            ?: error("${receiver.javaClass.name}.$name not found")
        method.isAccessible = true
        method.invoke(receiver, *args)
    }

    private fun enumConstant(enumClass: Class<*>, name: String): Any? =
        enumClass.enumConstants?.firstOrNull { (it as Enum<*>).name == name }

    private fun load(className: String): Class<*> = loader.loadClass(className)
}
