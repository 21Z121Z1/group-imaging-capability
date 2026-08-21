package io.github.z121z1.colorosmaterialplayground

enum class MaterialFamily {
    Blur,
    GradientBlur,
    Stroke,
    SpotLight,
    ToolbarStack,
}

enum class MaterialShapeKind {
    Capsule,
    Circle,
    RoundRect,
    Bar,
    Generic,
}

data class MaterialPreset(
    val family: MaterialFamily,
    val name: String,
) {
    val shape: MaterialShapeKind = inferMaterialShape(name)
}

data class RuntimeCatalog(
    val blur: List<String>,
    val stroke: List<String>,
    val spotLight: List<String>,
    val toolbarCategories: List<String>,
    val runtimeBacked: Boolean,
)

/**
 * Preset names extracted from com.oplus.uxdesign 17.0.11 / ColorOS 17.
 * Runtime enumeration takes precedence. These lists are a transparent fallback
 * so the catalog can still explain what was found in the supplied APK even if
 * the firmware package cannot be code-loaded by a third-party UID.
 */
object ApkMaterialCatalog {
    const val sourcePackage = "com.oplus.uxdesign"
    const val sourceVersion = "17.0.11"

    val blur = listOf(
        "TYPE_CONTENT_CHIP_TAB",
        "TYPE_CONTENT_CHIP_TAB_SELECTED",
        "TYPE_CONTENT_SECONDARY_BUTTON",
        "TYPE_CONTENT_SEGMENT_BUTTON",
        "TYPE_CONTENT_TRANSPARENT_BUTTON",
        "TYPE_FRAMEWORK_BOTTOM_BAR",
        "TYPE_FRAMEWORK_TOP_BAR",
        "TYPE_FRAMEWORK_TOP_BAR1",
        "TYPE_FRAMEWORK_TOP_BAR_BLUR",
        "TYPE_NO_EFFECT",
    )

    val stroke = listOf(
        "TYPE_CONTENT_CAPSULE_1",
        "TYPE_CONTENT_CAPSULE_2",
        "TYPE_CONTENT_CAPSULE_3",
        "TYPE_CONTENT_CAPSULE_4",
        "TYPE_CONTENT_CAPSULE_5",
        "TYPE_CONTENT_CAPSULE_6",
        "TYPE_CONTENT_CAPSULE_DISABLED_1",
        "TYPE_CONTENT_CAPSULE_DISABLED_2",
        "TYPE_CONTENT_CAPSULE_DISABLED_3",
        "TYPE_CONTENT_CAPSULE_DISABLED_4",
        "TYPE_CONTENT_CAPSULE_DISABLED_5",
        "TYPE_CONTENT_CAPSULE_DISABLED_6",
        "TYPE_FRAMEWORK_CAPSULE_1",
        "TYPE_FRAMEWORK_CAPSULE_2",
        "TYPE_FRAMEWORK_CAPSULE_3",
        "TYPE_FRAMEWORK_CAPSULE_4",
        "TYPE_FRAMEWORK_CAPSULE_5",
        "TYPE_FRAMEWORK_CAPSULE_6",
        "TYPE_FRAMEWORK_CAPSULE_7",
        "TYPE_FRAMEWORK_CAPSULE_8",
        "TYPE_FRAMEWORK_CAPSULE_9",
        "TYPE_FRAMEWORK_CAPSULE_10",
        "TYPE_FRAMEWORK_CAPSULE_11",
        "TYPE_FRAMEWORK_CAPSULE_12",
        "TYPE_FRAMEWORK_CAPSULE_DISABLED_1",
        "TYPE_FRAMEWORK_CAPSULE_DISABLED_2",
        "TYPE_FRAMEWORK_CAPSULE_DISABLED_3",
        "TYPE_FRAMEWORK_CAPSULE_DISABLED_4",
        "TYPE_FRAMEWORK_CAPSULE_DISABLED_5",
        "TYPE_FRAMEWORK_CAPSULE_DISABLED_6",
        "TYPE_FRAMEWORK_CIRCLE_1",
        "TYPE_FRAMEWORK_CIRCLE_2",
        "TYPE_FRAMEWORK_CIRCLE_3",
        "TYPE_FRAMEWORK_CIRCLE_4",
        "TYPE_FRAMEWORK_CIRCLE_5",
        "TYPE_FRAMEWORK_CIRCLE_DISABLED_1",
        "TYPE_FRAMEWORK_CIRCLE_DISABLED_2",
        "TYPE_NO_EFFECT",
        "TYPE_NO_EFFECT_BASE_1",
        "TYPE_NO_EFFECT_BASE_2",
    )

    val spotLight = listOf(
        "TYPE_BOTTOM_NAVIGATION",
        "TYPE_CAPSULE_1",
        "TYPE_CAPSULE_2",
        "TYPE_CHIP_TAB",
        "TYPE_CIRCLE_1",
        "TYPE_FAB",
        "TYPE_OPAQUE_LARGE_1",
        "TYPE_OPAQUE_LARGE_2",
        "TYPE_OPAQUE_MEDIUM_1",
        "TYPE_OPAQUE_MEDIUM_2",
        "TYPE_OPAQUE_SMALL_1",
        "TYPE_OPAQUE_SMALL_2",
        "TYPE_ROUNDRECT_1",
        "TYPE_SEEKBAR",
        "TYPE_SEGMENT_BUTTON",
        "TYPE_SWITCH",
        "TYPE_TRANSLUCENT_LARGE_1",
        "TYPE_TRANSLUCENT_LARGE_2",
        "TYPE_TRANSLUCENT_MEDIUM_2",
        "TYPE_TRANSLUCENT_SMALL_1",
        "TYPE_TRANSLUCENT_SMALL_2",
        "TYPE_TRANSPARENT_LARGE_1",
        "TYPE_TRANSPARENT_MEDIUM_1",
        "TYPE_TRANSPARENT_MEDIUM_2",
        "TYPE_TRANSPARENT_SMALL_1",
        "TYPE_TRANSPARENT_SMALL_2",
    )

    val toolbarCategories = listOf(
        "TOOLBAR_BUTTON",
        "MENU_ITEM",
        "MENU_OVERFLOW_BUTTON",
    )
}

fun inferMaterialShape(typeName: String): MaterialShapeKind = when {
    "CIRCLE" in typeName || "FAB" in typeName -> MaterialShapeKind.Circle
    "CAPSULE" in typeName ||
        "BUTTON" in typeName ||
        "CHIP" in typeName ||
        "SWITCH" in typeName ||
        "SEEKBAR" in typeName -> MaterialShapeKind.Capsule
    "TOP_BAR" in typeName ||
        "BOTTOM_BAR" in typeName ||
        "BOTTOM_NAVIGATION" in typeName -> MaterialShapeKind.Bar
    "ROUNDRECT" in typeName -> MaterialShapeKind.RoundRect
    else -> MaterialShapeKind.Generic
}
