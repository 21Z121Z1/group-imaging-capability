package io.github.z121z1.colorosmaterialplayground

import org.junit.Assert.assertTrue
import org.junit.Test

class ExperienceSceneContractTest {
    @Test
    fun sceneBlurPresets_existInExtractedCatalog() {
        val used = setOf(
            "TYPE_FRAMEWORK_BOTTOM_BAR",
            "TYPE_FRAMEWORK_TOP_BAR_BLUR",
            "TYPE_CONTENT_SEGMENT_BUTTON",
            "TYPE_CONTENT_SECONDARY_BUTTON",
            "TYPE_CONTENT_CHIP_TAB",
            "TYPE_CONTENT_TRANSPARENT_BUTTON",
        )
        assertTrue(ApkMaterialCatalog.blur.containsAll(used))
    }

    @Test
    fun sceneStrokePresets_existInExtractedCatalog() {
        val used = setOf(
            "TYPE_FRAMEWORK_CAPSULE_6",
            "TYPE_FRAMEWORK_CAPSULE_10",
            "TYPE_CONTENT_CAPSULE_3",
            "TYPE_CONTENT_CAPSULE_4",
            "TYPE_FRAMEWORK_CAPSULE_7",
            "TYPE_FRAMEWORK_CIRCLE_1",
            "TYPE_FRAMEWORK_CIRCLE_3",
        )
        assertTrue(ApkMaterialCatalog.stroke.containsAll(used))
    }

    @Test
    fun sceneSpotLightPresets_existInExtractedCatalog() {
        val used = setOf(
            "TYPE_BOTTOM_NAVIGATION",
            "TYPE_TRANSLUCENT_LARGE_1",
            "TYPE_SEGMENT_BUTTON",
            "TYPE_CAPSULE_1",
            "TYPE_CHIP_TAB",
            "TYPE_CIRCLE_1",
            "TYPE_TRANSLUCENT_SMALL_1",
        )
        assertTrue(ApkMaterialCatalog.spotLight.containsAll(used))
    }

    @Test
    fun sceneToolbarCategories_existInExtractedCatalog() {
        val used = setOf("TOOLBAR_BUTTON", "MENU_ITEM", "MENU_OVERFLOW_BUTTON")
        assertTrue(ApkMaterialCatalog.toolbarCategories.containsAll(used))
    }
}
