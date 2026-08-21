package io.github.z121z1.colorosmaterialplayground

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExperienceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color.White,
                    onPrimary = Color.Black,
                    background = Color.Black,
                    onBackground = Color.White,
                    surface = Color(0xFF111318),
                    onSurface = Color.White,
                ),
            ) {
                ExperienceApp()
            }
        }
    }
}

private enum class ExperienceScene(val label: String) {
    Gallery("相册"),
    ActionSheet("浮层"),
    Camera("相机"),
}

private data class SurfaceStack(
    val blur: String? = null,
    val stroke: String? = null,
    val spotLight: String? = null,
)

@Composable
private fun ExperienceApp() {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 600.dp
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val bridge = remember { ColorOsMaterialBridge(context.applicationContext) }
        val catalog = remember { bridge.catalog() }
        var scene by remember { mutableStateOf(ExperienceScene.Gallery) }
        var photo by remember { mutableStateOf<Bitmap?>(null) }

        val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                    }
                }.onSuccess { photo = it }
            }
        }

        Box(Modifier.fillMaxSize()) {
            when (scene) {
                ExperienceScene.Gallery -> GalleryScene(bridge, photo)
                ExperienceScene.ActionSheet -> ActionSheetScene(bridge, photo)
                ExperienceScene.Camera -> CameraScene(bridge, photo)
            }

            ExperienceTopBar(
                bridge = bridge,
                live = catalog.runtimeBacked,
                compact = compact,
                onPickPhoto = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onDiagnostics = { context.startActivity(Intent(context, MainActivity::class.java)) },
                modifier = Modifier.align(Alignment.TopCenter),
            )

            ExperienceDock(
                bridge = bridge,
                selected = scene,
                onSceneSelected = { scene = it },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun ExperienceTopBar(
    bridge: ColorOsMaterialBridge,
    live: Boolean,
    compact: Boolean,
    onPickPhoto: () -> Unit,
    onDiagnostics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (compact) "Material" else "ColorOS Material",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (live) "●  ColorOS 17 · LIVE" else "●  vendor runtime unavailable",
                color = if (live) Color(0xFF7BE495) else Color(0xFFFFC46B),
                style = MaterialTheme.typography.labelSmall,
            )
        }

        VendorToolbarButton(
            bridge = bridge,
            label = "背景",
            category = "TOOLBAR_BUTTON",
            onClick = onPickPhoto,
            width = 64.dp,
            height = 42.dp,
        )
        VendorToolbarButton(
            bridge = bridge,
            label = "诊断",
            category = "MENU_OVERFLOW_BUTTON",
            onClick = onDiagnostics,
            width = 64.dp,
            height = 42.dp,
        )
    }
}

@Composable
private fun ExperienceDock(
    bridge: ColorOsMaterialBridge,
    selected: ExperienceScene,
    onSceneSelected: (ExperienceScene) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, bottom = 10.dp)
            .height(66.dp),
    ) {
        VendorSurface(
            bridge = bridge,
            label = "",
            stack = SurfaceStack(
                blur = "TYPE_FRAMEWORK_BOTTOM_BAR",
                stroke = "TYPE_FRAMEWORK_CAPSULE_6",
                spotLight = "TYPE_BOTTOM_NAVIGATION",
            ),
            shape = MaterialShapeKind.Bar,
            modifier = Modifier.fillMaxSize(),
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ExperienceScene.entries.forEach { item ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { onSceneSelected(item) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected == item) {
                        VendorSurface(
                            bridge = bridge,
                            label = "",
                            stack = SurfaceStack(
                                blur = "TYPE_CONTENT_CHIP_TAB_SELECTED",
                                stroke = "TYPE_CONTENT_CAPSULE_3",
                                spotLight = "TYPE_CHIP_TAB",
                            ),
                            shape = MaterialShapeKind.Capsule,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Text(
                        text = item.label,
                        color = Color.White.copy(alpha = if (selected == item) 1f else 0.68f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected == item) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun SceneBackdrop(photo: Bitmap?) {
    if (photo == null) {
        AndroidView(
            factory = { DemoBackdropView(it) },
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        AndroidView(
            factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
            update = { it.setImageBitmap(photo) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun GalleryScene(bridge: ColorOsMaterialBridge, photo: Bitmap?) {
    Box(Modifier.fillMaxSize()) {
        SceneBackdrop(photo)

        VendorGradientBar(
            bridge = bridge,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(112.dp),
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 58.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("‹", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("8月21日", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("09:13", color = Color.White.copy(alpha = 0.66f), style = MaterialTheme.typography.labelSmall)
            }
            Text("⋯", color = Color.White, style = MaterialTheme.typography.titleLarge)
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 18.dp, end = 18.dp, bottom = 94.dp)
                .fillMaxWidth()
                .height(68.dp),
        ) {
            VendorSurface(
                bridge = bridge,
                label = "",
                stack = SurfaceStack(
                    blur = "TYPE_FRAMEWORK_BOTTOM_BAR",
                    stroke = "TYPE_FRAMEWORK_CAPSULE_6",
                    spotLight = "TYPE_BOTTOM_NAVIGATION",
                ),
                shape = MaterialShapeKind.Bar,
                modifier = Modifier.fillMaxSize(),
            )
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                listOf("分享", "收藏", "编辑", "删除").forEach { label ->
                    VendorToolbarButton(
                        bridge = bridge,
                        label = label,
                        category = "TOOLBAR_BUTTON",
                        width = 68.dp,
                        height = 46.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionSheetScene(bridge: ColorOsMaterialBridge, photo: Bitmap?) {
    Box(Modifier.fillMaxSize()) {
        SceneBackdrop(photo)

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 18.dp, end = 18.dp, bottom = 172.dp)
                .fillMaxWidth()
                .height(238.dp),
        ) {
            VendorSurface(
                bridge = bridge,
                label = "",
                stack = SurfaceStack(
                    blur = "TYPE_FRAMEWORK_TOP_BAR_BLUR",
                    stroke = "TYPE_FRAMEWORK_CAPSULE_10",
                    spotLight = "TYPE_TRANSLUCENT_LARGE_1",
                ),
                shape = MaterialShapeKind.RoundRect,
                modifier = Modifier.fillMaxSize(),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("快速操作", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "对当前内容执行常用操作",
                    color = Color.White.copy(alpha = 0.62f),
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    listOf(
                        Triple("复制", "TYPE_CONTENT_SEGMENT_BUTTON", "TYPE_SEGMENT_BUTTON"),
                        Triple("分享", "TYPE_CONTENT_SECONDARY_BUTTON", "TYPE_CAPSULE_1"),
                        Triple("收藏", "TYPE_CONTENT_TRANSPARENT_BUTTON", "TYPE_TRANSLUCENT_SMALL_1"),
                    ).forEach { (label, blur, spot) ->
                        VendorSurface(
                            bridge = bridge,
                            label = label,
                            stack = SurfaceStack(
                                blur = blur,
                                stroke = "TYPE_CONTENT_CAPSULE_3",
                                spotLight = spot,
                            ),
                            shape = MaterialShapeKind.Capsule,
                            modifier = Modifier.weight(1f).height(54.dp),
                        )
                    }
                }
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    VendorToolbarButton(
                        bridge = bridge,
                        label = "完成",
                        category = "MENU_ITEM",
                        width = 82.dp,
                        height = 46.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraScene(bridge: ColorOsMaterialBridge, photo: Bitmap?) {
    Box(Modifier.fillMaxSize()) {
        SceneBackdrop(photo)

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 58.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VendorToolbarButton(bridge, "HDR", "TOOLBAR_BUTTON", width = 64.dp, height = 42.dp)
            VendorToolbarButton(bridge, "闪光", "MENU_ITEM", width = 64.dp, height = 42.dp)
            VendorToolbarButton(bridge, "⋯", "MENU_OVERFLOW_BUTTON", width = 58.dp, height = 42.dp)
        }

        VendorSurface(
            bridge = bridge,
            label = "0.6×     1×     2×     3×",
            stack = SurfaceStack(
                blur = "TYPE_CONTENT_CHIP_TAB",
                stroke = "TYPE_FRAMEWORK_CAPSULE_7",
                spotLight = "TYPE_CHIP_TAB",
            ),
            shape = MaterialShapeKind.Capsule,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 205.dp)
                .width(248.dp)
                .height(52.dp),
        )

        VendorSurface(
            bridge = bridge,
            label = "●",
            stack = SurfaceStack(
                blur = "TYPE_CONTENT_TRANSPARENT_BUTTON",
                stroke = "TYPE_FRAMEWORK_CIRCLE_1",
                spotLight = "TYPE_CIRCLE_1",
            ),
            shape = MaterialShapeKind.Circle,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 112.dp)
                .size(78.dp),
        )

        VendorSurface(
            bridge = bridge,
            label = "最近",
            stack = SurfaceStack(
                blur = "TYPE_CONTENT_TRANSPARENT_BUTTON",
                stroke = "TYPE_FRAMEWORK_CIRCLE_3",
                spotLight = "TYPE_TRANSLUCENT_SMALL_1",
            ),
            shape = MaterialShapeKind.Circle,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 34.dp, bottom = 122.dp)
                .size(56.dp),
        )

        VendorToolbarButton(
            bridge = bridge,
            label = "切换",
            category = "TOOLBAR_BUTTON",
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 30.dp, bottom = 126.dp),
            width = 66.dp,
            height = 46.dp,
        )
    }
}

@Composable
private fun VendorGradientBar(
    bridge: ColorOsMaterialBridge,
    modifier: Modifier,
) {
    AndroidView(
        factory = {
            MaterialHostView(bridge.materialContext).apply {
                shapeKind = MaterialShapeKind.Bar
                setLabel("")
            }
        },
        update = { host ->
            val key = "scene-gradient"
            if (host.appliedKey != key) {
                bridge.clear(host)
                val result = bridge.applyGradientBlur(host, 1f)
                host.setLabel("")
                host.contentDescription = if (result.isSuccess) {
                    "ColorOS gradient blur active"
                } else {
                    "ColorOS gradient blur unavailable"
                }
                host.appliedKey = key
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun VendorSurface(
    bridge: ColorOsMaterialBridge,
    label: String,
    stack: SurfaceStack,
    shape: MaterialShapeKind,
    modifier: Modifier,
) {
    AndroidView(
        factory = {
            MaterialHostView(bridge.materialContext).apply {
                shapeKind = shape
                setLabel(label)
            }
        },
        update = { host ->
            val key = "scene:${stack.blur}:${stack.stroke}:${stack.spotLight}:$label"
            if (host.appliedKey != key) {
                bridge.clear(host)
                val failures = buildList {
                    stack.blur?.let { name ->
                        bridge.applyBlur(host, name).exceptionOrNull()?.let { add("blur") }
                    }
                    stack.stroke?.let { name ->
                        bridge.applyStroke(host, name).exceptionOrNull()?.let { add("stroke") }
                    }
                    stack.spotLight?.let { name ->
                        bridge.applySpotLight(host, name).exceptionOrNull()?.let { add("spot") }
                    }
                }
                host.setLabel(label)
                host.contentDescription = if (failures.isEmpty()) {
                    "$label · ColorOS material active"
                } else {
                    "$label · unavailable: ${failures.joinToString()}"
                }
                host.appliedKey = key
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun VendorToolbarButton(
    bridge: ColorOsMaterialBridge,
    label: String,
    category: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    width: Dp = 96.dp,
    height: Dp = 54.dp,
) {
    AndroidView(
        factory = {
            MaterialHostView(bridge.materialContext).apply {
                shapeKind = MaterialShapeKind.Capsule
                setLabel(label)
            }
        },
        update = { host ->
            val key = "scene-toolbar:$category:$label"
            if (host.appliedKey != key) {
                bridge.clear(host)
                val result = bridge.applyToolbarStack(
                    view = host,
                    categoryName = category,
                    blur = true,
                    stroke = true,
                    spotLight = true,
                    caustic = true,
                    forceEnable = true,
                )
                host.setLabel(label)
                host.contentDescription = if (result.isSuccess) {
                    "$label · ColorOS toolbar material active"
                } else {
                    "$label · ColorOS toolbar material unavailable"
                }
                host.appliedKey = key
            }
            if (onClick != null) {
                host.isClickable = true
                host.setOnClickListener { onClick() }
            } else {
                host.isClickable = false
                host.setOnClickListener(null)
            }
        },
        modifier = modifier.width(width).height(height),
    )
}
