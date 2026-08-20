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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
                    background = Color(0xFF080B12),
                    surface = Color(0xE8212630),
                ),
            ) {
                ExperienceApp()
            }
        }
    }
}

private enum class ExperienceScene(val label: String) {
    Gallery("相册查看器"),
    ActionSheet("浮动操作面板"),
    Camera("相机控制条"),
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
        var photoError by remember { mutableStateOf<String?>(null) }

        val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                    }
                }
                result.onSuccess {
                    photo = it
                    photoError = null
                }.onFailure {
                    photoError = "图片解码失败：${it.javaClass.simpleName}: ${it.message.orEmpty()}"
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = if (compact) 12.dp else 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (compact) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ColorOS Material", style = MaterialTheme.typography.titleMedium)
                        RuntimeLine(catalog.runtimeBacked)
                    }
                    TextButton(onClick = { context.startActivity(Intent(context, MainActivity::class.java)) }) {
                        Text("单项实验室")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ColorOS Material · 真实组合场景", style = MaterialTheme.typography.titleLarge)
                        RuntimeLine(catalog.runtimeBacked)
                    }
                    TextButton(onClick = { context.startActivity(Intent(context, MainActivity::class.java)) }) {
                        Text("打开单项实验室")
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ExperienceScene.entries.forEach { item ->
                    FilterChip(
                        selected = scene == item,
                        onClick = { scene = item },
                        label = { Text(item.label) },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                ) {
                    Text(if (photo == null) "选择照片背景" else "更换照片背景")
                }
                if (photo != null) {
                    FilterChip(
                        selected = true,
                        onClick = { photo = null },
                        label = { Text("恢复测试背景") },
                    )
                }
                photoError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(if (compact) 28.dp else 36.dp),
                color = Color.Transparent,
            ) {
                when (scene) {
                    ExperienceScene.Gallery -> GalleryScene(bridge, photo, catalog.runtimeBacked)
                    ExperienceScene.ActionSheet -> ActionSheetScene(bridge, photo, catalog.runtimeBacked)
                    ExperienceScene.Camera -> CameraScene(bridge, photo, catalog.runtimeBacked)
                }
            }
        }
    }
}

@Composable
private fun RuntimeLine(runtimeBacked: Boolean) {
    Text(
        if (runtimeBacked) {
            "组合场景 · LIVE ColorOS vendor material"
        } else {
            "组合场景 · 当前设备无 ColorOS vendor runtime"
        },
        color = if (runtimeBacked) Color(0xFF9BE7A5) else Color(0xFFFFCC80),
        style = MaterialTheme.typography.bodySmall,
    )
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
            factory = {
                ImageView(it).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
            },
            update = { it.setImageBitmap(photo) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun GalleryScene(
    bridge: ColorOsMaterialBridge,
    photo: Bitmap?,
    runtimeBacked: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp)),
    ) {
        SceneBackdrop(photo)

        VendorGradientBar(
            bridge = bridge,
            label = "‹              相册              ⋯",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(76.dp),
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 105.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            VendorToolbarButton(bridge, "自动增强", "TOOLBAR_BUTTON", width = 88.dp, height = 50.dp)
            VendorToolbarButton(bridge, "更多", "MENU_OVERFLOW_BUTTON", width = 82.dp, height = 50.dp)
        }

        VendorSurface(
            bridge = bridge,
            label = "分享        收藏        编辑        删除",
            stack = SurfaceStack(
                blur = "TYPE_FRAMEWORK_BOTTOM_BAR",
                stroke = "TYPE_FRAMEWORK_CAPSULE_6",
                spotLight = "TYPE_BOTTOM_NAVIGATION",
            ),
            shape = MaterialShapeKind.Bar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 14.dp, end = 14.dp, bottom = 18.dp)
                .fillMaxWidth()
                .height(68.dp),
        )

        RuntimeBadge(runtimeBacked, Modifier.align(Alignment.TopEnd).padding(10.dp))
    }
}

@Composable
private fun ActionSheetScene(
    bridge: ColorOsMaterialBridge,
    photo: Bitmap?,
    runtimeBacked: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp)),
    ) {
        SceneBackdrop(photo)

        VendorSurface(
            bridge = bridge,
            label = "快速操作",
            stack = SurfaceStack(
                blur = "TYPE_FRAMEWORK_TOP_BAR_BLUR",
                stroke = "TYPE_FRAMEWORK_CAPSULE_10",
                spotLight = "TYPE_TRANSLUCENT_LARGE_1",
            ),
            shape = MaterialShapeKind.RoundRect,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .height(230.dp),
        )

        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 92.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            VendorSurface(
                bridge = bridge,
                label = "复制",
                stack = SurfaceStack(
                    blur = "TYPE_CONTENT_SEGMENT_BUTTON",
                    stroke = "TYPE_CONTENT_CAPSULE_3",
                    spotLight = "TYPE_SEGMENT_BUTTON",
                ),
                shape = MaterialShapeKind.Capsule,
                modifier = Modifier.width(108.dp).height(52.dp),
            )
            VendorSurface(
                bridge = bridge,
                label = "分享",
                stack = SurfaceStack(
                    blur = "TYPE_CONTENT_SECONDARY_BUTTON",
                    stroke = "TYPE_CONTENT_CAPSULE_4",
                    spotLight = "TYPE_CAPSULE_1",
                ),
                shape = MaterialShapeKind.Capsule,
                modifier = Modifier.width(108.dp).height(52.dp),
            )
        }

        VendorToolbarButton(
            bridge = bridge,
            label = "完成",
            category = "MENU_ITEM",
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
            width = 88.dp,
            height = 50.dp,
        )

        RuntimeBadge(runtimeBacked, Modifier.align(Alignment.TopEnd).padding(10.dp))
    }
}

@Composable
private fun CameraScene(
    bridge: ColorOsMaterialBridge,
    photo: Bitmap?,
    runtimeBacked: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp)),
    ) {
        SceneBackdrop(photo)

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VendorToolbarButton(bridge, "HDR", "TOOLBAR_BUTTON", width = 72.dp, height = 46.dp)
            VendorToolbarButton(bridge, "闪光", "MENU_ITEM", width = 72.dp, height = 46.dp)
            VendorToolbarButton(bridge, "⋯", "MENU_OVERFLOW_BUTTON", width = 64.dp, height = 46.dp)
        }

        VendorSurface(
            bridge = bridge,
            label = "0.6×      1×      2×      3×",
            stack = SurfaceStack(
                blur = "TYPE_CONTENT_CHIP_TAB",
                stroke = "TYPE_FRAMEWORK_CAPSULE_7",
                spotLight = "TYPE_CHIP_TAB",
            ),
            shape = MaterialShapeKind.Capsule,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 120.dp)
                .width(250.dp)
                .height(54.dp),
        )

        VendorSurface(
            bridge = bridge,
            label = "快门",
            stack = SurfaceStack(
                blur = "TYPE_CONTENT_TRANSPARENT_BUTTON",
                stroke = "TYPE_FRAMEWORK_CIRCLE_1",
                spotLight = "TYPE_CIRCLE_1",
            ),
            shape = MaterialShapeKind.Circle,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp)
                .size(86.dp),
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
                .padding(start = 24.dp, bottom = 42.dp)
                .size(60.dp),
        )

        VendorToolbarButton(
            bridge = bridge,
            label = "切换",
            category = "TOOLBAR_BUTTON",
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 46.dp),
            width = 72.dp,
            height = 48.dp,
        )

        RuntimeBadge(runtimeBacked, Modifier.align(Alignment.TopEnd).padding(10.dp))
    }
}

@Composable
private fun VendorGradientBar(
    bridge: ColorOsMaterialBridge,
    label: String,
    modifier: Modifier,
) {
    AndroidView(
        factory = {
            MaterialHostView(bridge.materialContext).apply {
                shapeKind = MaterialShapeKind.Bar
                setLabel(label)
            }
        },
        update = { host ->
            val key = "scene-gradient:$label"
            if (host.appliedKey != key) {
                bridge.clear(host)
                val result = bridge.applyGradientBlur(host, 1f)
                host.setLabel(label)
                host.contentDescription = if (result.isSuccess) {
                    "$label · ColorOS gradient blur active"
                } else {
                    "$label · ColorOS gradient blur unavailable: ${result.exceptionOrNull()?.javaClass?.simpleName}"
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
                        bridge.applyBlur(host, name).exceptionOrNull()?.let { add("blur:${it.javaClass.simpleName}") }
                    }
                    stack.stroke?.let { name ->
                        bridge.applyStroke(host, name).exceptionOrNull()?.let { add("stroke:${it.javaClass.simpleName}") }
                    }
                    stack.spotLight?.let { name ->
                        bridge.applySpotLight(host, name).exceptionOrNull()?.let { add("spot:${it.javaClass.simpleName}") }
                    }
                }
                host.setLabel(label)
                host.contentDescription = if (failures.isEmpty()) {
                    "$label · ColorOS material stack active"
                } else {
                    "$label · ColorOS material unavailable: ${failures.joinToString() }"
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
                    "$label · ColorOS blur stroke spotlight caustic stack active"
                } else {
                    "$label · ColorOS toolbar stack unavailable: ${result.exceptionOrNull()?.javaClass?.simpleName}"
                }
                host.appliedKey = key
            }
        },
        modifier = modifier.width(width).height(height),
    )
}

@Composable
private fun RuntimeBadge(runtimeBacked: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color(0xA0000000),
        contentColor = Color.White,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            if (runtimeBacked) "COLOROS RUNTIME" else "VENDOR API UNAVAILABLE",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
