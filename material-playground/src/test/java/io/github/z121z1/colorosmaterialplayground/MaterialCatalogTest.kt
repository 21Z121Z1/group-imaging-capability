package io.github.z121z1.colorosmaterialplayground

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MaterialCatalogTest {
    @Test
    fun fallbackCatalogHasUniqueRealPresetNames() {
        assertEquals(ApkMaterialCatalog.blur.size, ApkMaterialCatalog.blur.toSet().size)
        assertEquals(ApkMaterialCatalog.stroke.size, ApkMaterialCatalog.stroke.toSet().size)
        assertEquals(ApkMaterialCatalog.spotLight.size, ApkMaterialCatalog.spotLight.toSet().size)
        assertTrue(ApkMaterialCatalog.blur.size >= 10)
        assertTrue(ApkMaterialCatalog.stroke.size >= 30)
        assertTrue(ApkMaterialCatalog.spotLight.size >= 20)
        assertTrue("TYPE_FRAMEWORK_TOP_BAR_BLUR" in ApkMaterialCatalog.blur)
        assertTrue("TYPE_FRAMEWORK_CAPSULE_12" in ApkMaterialCatalog.stroke)
        assertTrue("TYPE_BOTTOM_NAVIGATION" in ApkMaterialCatalog.spotLight)
    }

    @Test
    fun shapeInferenceMatchesColorOsPresetFamilies() {
        assertEquals(MaterialShapeKind.Circle, inferMaterialShape("TYPE_FRAMEWORK_CIRCLE_3"))
        assertEquals(MaterialShapeKind.Circle, inferMaterialShape("TYPE_FAB"))
        assertEquals(MaterialShapeKind.Capsule, inferMaterialShape("TYPE_CONTENT_CAPSULE_2"))
        assertEquals(MaterialShapeKind.Capsule, inferMaterialShape("TYPE_SEGMENT_BUTTON"))
        assertEquals(MaterialShapeKind.Bar, inferMaterialShape("TYPE_FRAMEWORK_TOP_BAR_BLUR"))
        assertEquals(MaterialShapeKind.Bar, inferMaterialShape("TYPE_BOTTOM_NAVIGATION"))
        assertEquals(MaterialShapeKind.RoundRect, inferMaterialShape("TYPE_ROUNDRECT_1"))
    }
}
