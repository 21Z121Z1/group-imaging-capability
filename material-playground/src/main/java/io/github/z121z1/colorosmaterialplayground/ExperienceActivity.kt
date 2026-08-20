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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("ColorOS Material · 组合场景", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (catalog.runtimeBacked) {
                        "LIVE · 多层同时调用真实 ColorOS 材质"
                    } else {
                        "CATALOG · 当前设备没有可调用的 ColorOS vendor code"
                    },
                    color = if (catalog.runtimeBacked) Color(0xFF9BE7A5) else Color(0xFFFFCC80),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(onClick = { context.startActivity(Intent(context, MainActivity::class.java)) }) {
                Text("单项实验室")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
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
            modifier = Modifier.fillMaxWidth(),
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
            shape = RoundedCornerShape(34.dp),
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
            .clip(RoundedCornerShape(34.dp)),
    ) {
        SceneBackdrop(photo)

        VendorGradientBar(
            bridge = bridge,
            label = "‹   8月21日  02:05                                      ⋯",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(86.dp),
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                color = Color(0x52000000),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text(
                    "照片内容区域\n顶部是 Gradient Blur，底部同时叠加 Bottom Bar + Stroke + SpotLight",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
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
                .padding(start = 18.dp, end = 18.dp, bottom = 22.dp)
                .fillMaxWidth()
                .height(76.dp),
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 112.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VendorToolbarButton(bridge, "自动增强", "TOOLBAR_BUTTON")
            VendorToolbarButton(bridge, "更多", "MENU_OVERFLOW_BUTTON")
        }

        RuntimeBadge(runtimeBacked, Modifier.align(Alignment.TopEnd).padding(12.dp))
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
            .clip(RoundedCornerShape(34.dp)),
    ) {
        SceneBackdrop(photo)

        VendorSurface(
            bridge = bridge,
            label = "快速操作\n\n这是一块 Blur + Stroke/SDF + SpotLight 同时存在的主面板",
            stack = SurfaceStack(
                blur = "TYPE_FRAMEWORK_TOP_BAR_BLUR",
                stroke = "TYPE_FRAMEWORK_CAPSULE_10",
                spotLight = "TYPE_TRANSLUCENT_LARGE_1",
            ),
            shape = MaterialShapeKind.RoundRect,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 22.dp)
                .fillMaxWidth()
                .height(250.dp),
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 148.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VendorSurface(
                    bridge = bridge,
                    label = "复制",
                    stack = SurfaceStack(
                        blur = "TYPE_CONTENT_SEGMENT_BUTTON",
                        stroke = "TYPE_CONTENT_CAPSULE_3",
                        spotLight = "TYPE_SEGMENT_BUTTON",
                    ),
                    shape = MaterialShapeKind.Capsule,
                    modifier = Modifier.width(116.dp).height(54.dp),
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
                    modifier = Modifier.width(116.dp).height(54.dp),
                )
            }
        }

        VendorToolbarButton(
            bridge = bridge,
            label = "完成",
            category = "MENU_ITEM",
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp),
        )

        RuntimeBadge(runtimeBacked, Modifier.align(Alignment.TopEnd).padding(12.dp))
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
            .clip(RoundedCornerShape(34.dp)),
    ) {
        SceneBackdrop(photo)

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VendorToolbarButton(bridge, "HDR", "TOOLBAR_BUTTON")
            VendorToolbarButton(bridge, "闪光", "MENU_ITEM")
            VendorToolbarButton(bridge, "⋯", "MENU_OVERFLOW_BUTTON")
        }

        VendorSurface(
            bridge = bridge,
            label = "0.6×        1×        2×        3×",
            stack = SurfaceStack(
                blur = "TYPE_CONTENT_CHIP_TAB",
                stroke = "TYPE_FRAMEWORK_CAPSULE_7",
                spotLight = "TYPE_CHIP_TAB",
            ),
            shape = MaterialShapeKind.Capsule,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 140.dp)
                .width(286.dp)
                .height(58.dp),
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
                .padding(bottom = 34.dp)
                .size(92.dp),
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
                .padding(start = 30.dp, bottom = 48.dp)
                .size(64.dp),
        )

        VendorToolbarButton(
            bridge = bridge,
            label = "切换",
            category = "TOOLBAR_BUTTON",
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 30.dp, bottom = 48.dp),
        )

        RuntimeBadge(runtimeBacked, Modifier.align(Alignment.TopEnd).padding(12.dp))
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
                host.setLabel(if (result.isSuccess) label else "$label\nGradient blur unavailable")
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
                host.setLabel(
                    if (failures.isEmpty()) label else "$label\nUNAVAILABLE: ${failures.joinToString("+")}",
                )
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
                host.setLabel(if (result.isSuccess) label else "$label\nstack unavailable")
                host.appliedKey = key
            }
        },
        modifier = modifier.width(96.dp).height(54.dp),
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
            if (runtimeBacked) "REAL COLOROS STACK" else "GENERIC DEVICE · EFFECTS UNAVAILABLE",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
