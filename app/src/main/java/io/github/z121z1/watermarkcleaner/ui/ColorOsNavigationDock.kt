package io.github.z121z1.watermarkcleaner.ui

import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.z121z1.watermarkcleaner.platform.ColorOsSurfaceRole

private val FloatingBarHeight = 64.dp
private val FloatingBarItemHeight = 52.dp

/**
 * ColorOS-style floating bottom navigation.
 *
 * The bar is one capsule material surface. Items live inside that surface and
 * only their local selected/unselected material changes; no item is allowed to
 * own or measure the window-sized parent surface.
 */
@Composable
fun ColorOsNavigationDock(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bridge = LocalColorOsUiBridge.current
    val nativeViews = remember { mutableStateMapOf<Int, View>() }
    val previous = remember { mutableIntStateOf(selectedIndex) }

    LaunchedEffect(selectedIndex, nativeViews.size) {
        if (bridge?.runtimeInfo?.transitionAvailable != true || nativeViews.size < labels.size) return@LaunchedEffect
        val old = previous.intValue
        if (old != selectedIndex) {
            nativeViews[old]?.let { bridge.animateGroup(listOf(it), entering = false) }
            nativeViews[selectedIndex]?.let { bridge.animateGroup(listOf(it), entering = true) }
            previous.intValue = selectedIndex
        }
    }

    ColorOsMaterialSurface(
        modifier = modifier.height(FloatingBarHeight),
        role = ColorOsSurfaceRole.BOTTOM_BAR,
        corner = ColorOsCornerProfile.CAPSULE,
        contentPadding = PaddingValues(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(FloatingBarItemHeight),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            labels.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                ColorOsActionButton(
                    text = label,
                    onClick = { onSelect(index) },
                    role = if (selected) ColorOsSurfaceRole.CHIP_SELECTED else ColorOsSurfaceRole.CHIP,
                    fallbackOutlined = !selected,
                    modifier = Modifier.weight(1f).height(FloatingBarItemHeight),
                    onNativeView = { nativeViews[index] = it },
                )
            }
        }
    }
}
