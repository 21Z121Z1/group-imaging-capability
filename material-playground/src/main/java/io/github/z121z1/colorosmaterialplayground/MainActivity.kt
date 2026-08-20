package io.github.z121z1.colorosmaterialplayground

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
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
                Playground(this)
            }
        }
    }
}

private enum class BackgroundMode(val label: String) {
    Generated("彩色测试背景"),
    Photo("Photo Picker"),
    HdrPattern("HDR UI 测试图"),
}

@Composable
private fun Playground(activity: MainActivity) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bridge = remember { ColorOsMaterialBridge(context.applicationContext) }
    val catalog = remember { bridge.catalog() }

    var family by remember { mutableStateOf(MaterialFamily.Blur) }
    var backgroundMode by remember { mutableStateOf(BackgroundMode.Generated) }
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var photoError by remember { mutableStateOf<String?>(null) }
    var forceHdr by remember { mutableStateOf(false) }
    var desiredHeadroom by remember { mutableStateOf(2.2f) }
    var hdrState by remember { mutableStateOf(HdrWindowState(false, false, false, 1f)) }
    var gradientFraction by remember { mutableStateOf(1f) }

    var toolbarBlur by remember { mutableStateOf(true) }
    var toolbarStroke by remember { mutableStateOf(true) }
    var toolbarSpot by remember { mutableStateOf(true) }
    var toolbarCaustic by remember { mutableStateOf(true) }
    var toolbarForce by remember { mutableStateOf(true) }

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
                backgroundMode = BackgroundMode.Photo
            }.onFailure {
                photoError = "图片解码失败：${it.javaClass.simpleName}: ${it.message.orEmpty()}"
            }
        }
    }

    LaunchedEffect(backgroundMode, photo, forceHdr, desiredHeadroom) {
        hdrState = updateHdrWindow(
            window = activity.window,
            bitmap = if (backgroundMode == BackgroundMode.Photo) photo else null,
            forceHdr = forceHdr,
            hdrPattern = backgroundMode == BackgroundMode.HdrPattern,
            desiredHeadroom = desiredHeadroom,
        )
    }

    Box(Modifier.fillMaxSize()) {
        BackgroundLayer(backgroundMode, photo)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                HeaderCard(
                    runtimeBacked = catalog.runtimeBacked,
                    installedVersion = bridge.installedVersion,
                )
            }

            item {
                BackgroundControls(
                    selected = backgroundMode,
                    onSelected = { backgroundMode = it },
                    onPickPhoto = {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    forceHdr = forceHdr,
                    onForceHdr = { forceHdr = it },
                    headroom = desiredHeadroom,
                    onHeadroom = { desiredHeadroom = it },
                    hdrState = hdrState,
                    displaySummary = hdrDisplaySummary(activity),
                    photoError = photoError,
                )
            }

            item {
                FamilySelector(
                    selected = family,
                    onSelected = { family = it },
                )
            }

            when (family) {
                MaterialFamily.Blur -> {
                    item { SectionTitle("COUIMaterialBlurEffect", "运行时 BlurEffectType 枚举；逐项调用真实 ColorOS preset") }
                    items(catalog.blur, key = { "blur:$it" }) { name ->
                        MaterialPresetCard(bridge, family, name, inferMaterialShape(name))
                    }
                }

                MaterialFamily.GradientBlur -> {
                    item {
                        SectionTitle(
                            "AppBar Gradient Blur",
                            "调用 ColorOS AppBarBlurHelper；滑块直接驱动 updateGradientBlurFraction",
                        )
                    }
                    item {
                        ControlCard {
                            Text("Gradient fraction  ${"%.2f".format(gradientFraction)}")
                            Slider(
                                value = gradientFraction,
                                onValueChange = { gradientFraction = it },
                                valueRange = 0f..1f,
                            )
                        }
                    }
                    item { GradientBlurPreview(bridge, gradientFraction) }
                }

                MaterialFamily.Stroke -> {
                    item {
                        SectionTitle(
                            "COUIMaterialStrokeEffect",
                            "Capsule / Circle / Disabled / Framework / Content 全部来自 StrokeEffectType",
                        )
                    }
                    items(catalog.stroke, key = { "stroke:$it" }) { name ->
                        MaterialPresetCard(bridge, family, name, inferMaterialShape(name))
                    }
                }

                MaterialFamily.SpotLight -> {
                    item {
                        SectionTitle(
                            "COUISpotLightEffect",
                            "真实 ColorOS Spotlight drawable；按住并拖动查看材质光源响应",
                        )
                    }
                    items(catalog.spotLight, key = { "spot:$it" }) { name ->
                        MaterialPresetCard(bridge, family, name, inferMaterialShape(name))
                    }
                }

                MaterialFamily.ToolbarStack -> {
                    item {
                        SectionTitle(
                            "Toolbar Material Stack",
                            "Blur + Stroke + SpotLight + Caustic Shadow，由 ToolbarMaterialEffectDelegate 原生组合",
                        )
                    }
                    item {
                        ControlCard {
                            ToggleRow("Blur", toolbarBlur) { toolbarBlur = it }
                            ToggleRow("Stroke / SDF edge", toolbarStroke) { toolbarStroke = it }
                            ToggleRow("SpotLight", toolbarSpot) { toolbarSpot = it }
                            ToggleRow("Caustic Shadow", toolbarCaustic) { toolbarCaustic = it }
                            ToggleRow("Force vendor material", toolbarForce) { toolbarForce = it }
                        }
                    }
                    items(catalog.toolbarCategories, key = { "toolbar:$it" }) { category ->
                        ToolbarStackCard(
                            bridge = bridge,
                            category = category,
                            blur = toolbarBlur,
                            stroke = toolbarStroke,
                            spot = toolbarSpot,
                            caustic = toolbarCaustic,
                            force = toolbarForce,
                        )
                    }
                }
            }

            item {
                DiagnosticsCard(
                    diagnostics = bridge.diagnostics(),
                    runtimeBacked = catalog.runtimeBacked,
                )
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun BackgroundLayer(mode: BackgroundMode, photo: Bitmap?) {
    when (mode) {
        BackgroundMode.Generated -> AndroidView(
            factory = { DemoBackdropView(it) },
            modifier = Modifier.matchParentSize(),
        )

        BackgroundMode.HdrPattern -> AndroidView(
            factory = { HdrTestPatternView(it) },
            modifier = Modifier.matchParentSize(),
        )

        BackgroundMode.Photo -> if (photo != null) {
            AndroidView(
                factory = {
                    ImageView(it).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
                },
                update = { it.setImageBitmap(photo) },
                modifier = Modifier.matchParentSize(),
            )
        } else {
            AndroidView(
                factory = { DemoBackdropView(it) },
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

@Composable
private fun HeaderCard(runtimeBacked: Boolean, installedVersion: String) {
    ControlCard {
        Text("ColorOS Material Playground", style = MaterialTheme.typography.headlineSmall)
        Text(
            if (runtimeBacked) {
                "LIVE · 运行时枚举 com.oplus.uxdesign $installedVersion"
            } else {
                "APK CATALOG · 显示 17.0.11 提取出的真实 preset 名；运行时 vendor code 尚不可用"
            },
            color = if (runtimeBacked) Color(0xFF9BE7A5) else Color(0xFFFFCC80),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            "预览不会用自制 Liquid Glass shader 冒充 ColorOS。调用失败会直接显示异常。",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun BackgroundControls(
    selected: BackgroundMode,
    onSelected: (BackgroundMode) -> Unit,
    onPickPhoto: () -> Unit,
    forceHdr: Boolean,
    onForceHdr: (Boolean) -> Unit,
    headroom: Float,
    onHeadroom: (Float) -> Unit,
    hdrState: HdrWindowState,
    displaySummary: List<String>,
    photoError: String?,
) {
    ControlCard {
        Text("背景 / HDR", style = MaterialTheme.typography.titleMedium)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BackgroundMode.entries.forEach { mode ->
                FilterChip(
                    selected = selected == mode,
                    onClick = { onSelected(mode) },
                    label = { Text(mode.label) },
                )
            }
        }
        Button(onClick = onPickPhoto) { Text("选择真实照片背景") }
        ToggleRow("强制请求 HDR window", forceHdr, onForceHdr)
        Text("Desired HDR headroom  ${"%.1f".format(headroom)}×")
        Slider(
            value = headroom,
            onValueChange = onHeadroom,
            valueRange = 1f..6f,
        )
        Text(
            "HDR request=${hdrState.requestedHdr} · gain map=${hdrState.photoHasGainmap} · photo WCG=${hdrState.photoWideGamut}",
            style = MaterialTheme.typography.bodySmall,
        )
        displaySummary.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        photoError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FamilySelector(selected: MaterialFamily, onSelected: (MaterialFamily) -> Unit) {
    ControlCard {
        Text("材质族", style = MaterialTheme.typography.titleMedium)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MaterialFamily.entries.forEach { family ->
                FilterChip(
                    selected = selected == family,
                    onClick = { onSelected(family) },
                    label = {
                        Text(
                            when (family) {
                                MaterialFamily.Blur -> "Blur"
                                MaterialFamily.GradientBlur -> "Gradient Blur"
                                MaterialFamily.Stroke -> "Stroke / SDF"
                                MaterialFamily.SpotLight -> "SpotLight"
                                MaterialFamily.ToolbarStack -> "Caustic Stack"
                            },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MaterialPresetCard(
    bridge: ColorOsMaterialBridge,
    family: MaterialFamily,
    name: String,
    shape: MaterialShapeKind,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(26.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val previewModifier = when (shape) {
                MaterialShapeKind.Circle -> Modifier.size(132.dp)
                MaterialShapeKind.Bar -> Modifier.fillMaxWidth().height(94.dp)
                MaterialShapeKind.Capsule -> Modifier.fillMaxWidth().height(82.dp)
                MaterialShapeKind.RoundRect -> Modifier.fillMaxWidth().height(126.dp)
                MaterialShapeKind.Generic -> Modifier.fillMaxWidth().height(112.dp)
            }
            AndroidView(
                factory = {
                    MaterialHostView(bridge.materialContext).apply {
                        shapeKind = shape
                        setLabel(name)
                    }
                },
                update = { host ->
                    val key = "$family:$name"
                    host.shapeKind = shape
                    if (host.appliedKey != key) {
                        bridge.clear(host)
                        val result: Result<Unit> = when (family) {
                            MaterialFamily.Blur -> bridge.applyBlur(host, name)
                            MaterialFamily.Stroke -> bridge.applyStroke(host, name)
                            MaterialFamily.SpotLight -> bridge.applySpotLight(host, name)
                            else -> Result.failure(
                                IllegalArgumentException("Unsupported preset family: $family"),
                            )
                        }
                        host.setLabel(
                            if (result.isSuccess) {
                                if (family == MaterialFamily.SpotLight) "$name\n按住 / 拖动" else name
                            } else {
                                "$name\nUNAVAILABLE: ${rootCause(result.exceptionOrNull()).javaClass.simpleName}"
                            },
                        )
                        host.appliedKey = key
                    }
                },
                modifier = previewModifier,
            )
        }
    }
}

@Composable
private fun GradientBlurPreview(bridge: ColorOsMaterialBridge, fraction: Float) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(26.dp),
    ) {
        AndroidView(
            factory = {
                MaterialHostView(bridge.materialContext).apply {
                    shapeKind = MaterialShapeKind.Bar
                    setLabel("COLOROS GRADIENT BLUR")
                }
            },
            update = { host ->
                if (host.appliedKey != "gradient") {
                    bridge.clear(host)
                    val result = bridge.applyGradientBlur(host, fraction)
                    host.setLabel(
                        if (result.isSuccess) {
                            "AppBarBlurHelper\nfraction=${"%.2f".format(fraction)}"
                        } else {
                            "GRADIENT BLUR UNAVAILABLE\n${rootCause(result.exceptionOrNull()).javaClass.simpleName}"
                        },
                    )
                    host.appliedKey = if (result.isSuccess) "gradient" else "gradient-failed"
                } else {
                    val result = bridge.updateGradientBlur(host, fraction)
                    host.setLabel(
                        if (result.isSuccess) {
                            "AppBarBlurHelper\nfraction=${"%.2f".format(fraction)}"
                        } else {
                            "GRADIENT UPDATE FAILED\n${rootCause(result.exceptionOrNull()).javaClass.simpleName}"
                        },
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().height(180.dp),
        )
    }
}

@Composable
private fun ToolbarStackCard(
    bridge: ColorOsMaterialBridge,
    category: String,
    blur: Boolean,
    stroke: Boolean,
    spot: Boolean,
    caustic: Boolean,
    force: Boolean,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(26.dp),
    ) {
        AndroidView(
            factory = {
                MaterialHostView(bridge.materialContext).apply {
                    shapeKind = MaterialShapeKind.Capsule
                    setLabel(category)
                }
            },
            update = { host ->
                val key = "$category:$blur:$stroke:$spot:$caustic:$force"
                if (host.appliedKey != key) {
                    bridge.clear(host)
                    val result = bridge.applyToolbarStack(
                        view = host,
                        categoryName = category,
                        blur = blur,
                        stroke = stroke,
                        spotLight = spot,
                        caustic = caustic,
                        forceEnable = force,
                    )
                    host.setLabel(
                        if (result.isSuccess) {
                            "$category\nB=$blur S=$stroke L=$spot C=$caustic"
                        } else {
                            "$category\nUNAVAILABLE: ${rootCause(result.exceptionOrNull()).javaClass.simpleName}"
                        },
                    )
                    host.appliedKey = key
                }
            },
            modifier = Modifier.fillMaxWidth().height(92.dp),
        )
    }
}

@Composable
private fun DiagnosticsCard(diagnostics: List<String>, runtimeBacked: Boolean) {
    ControlCard {
        Text("Runtime probe", style = MaterialTheme.typography.titleMedium)
        Text(
            if (runtimeBacked) {
                "APK code loader 已成功枚举真实 ColorOS classes。"
            } else {
                "当前只显示 APK 提取 catalog；下面的 probe 会指出运行时阻断层。"
            },
            style = MaterialTheme.typography.bodySmall,
        )
        diagnostics.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(10.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ControlCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        shape = RoundedCornerShape(26.dp),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

private fun rootCause(throwable: Throwable?): Throwable {
    var current = throwable ?: IllegalStateException("Unknown error")
    while (current.cause != null && current.cause !== current) {
        current = current.cause!!
    }
    return current
}
