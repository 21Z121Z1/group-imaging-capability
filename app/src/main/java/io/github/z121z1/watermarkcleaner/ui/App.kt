package io.github.z121z1.watermarkcleaner.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.util.Size
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.z121z1.watermarkcleaner.AppViewModel
import io.github.z121z1.watermarkcleaner.CalibrationState
import io.github.z121z1.watermarkcleaner.QueueItem
import io.github.z121z1.watermarkcleaner.QueueStatus
import io.github.z121z1.watermarkcleaner.UiState
import io.github.z121z1.watermarkcleaner.core.CalibrationEngine
import io.github.z121z1.watermarkcleaner.core.HdrProbeFactory
import io.github.z121z1.watermarkcleaner.core.HdrProbePattern
import io.github.z121z1.watermarkcleaner.data.PhotoLibraryAccess
import io.github.z121z1.watermarkcleaner.data.ScreenshotMediaFinder
import io.github.z121z1.watermarkcleaner.platform.ColorOsCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Destination(val label: String, val glyph: String) {
    PROCESS("处理", "净"),
    CALIBRATE("校准", "准"),
    SETTINGS("设置", "设"),
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WatermarkCleanerApp(viewModel: AppViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current as? ComponentActivity
    var destination by remember { mutableStateOf(Destination.PROCESS) }

    val pickImages = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(50),
    ) { uris -> viewModel.enqueue(uris) }

    val pickCalibration = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        viewModel.consumeCalibrationPickerRequest()
        if (uri != null) viewModel.addCalibrationSample(uri)
        else viewModel.calibrationMessage("未选择截图；请重新截取当前灰阶")
    }

    val chooseOutput = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            viewModel.setOutputTree(uri)
        }
    }

    val requestMediaAccess = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { }

    LaunchedEffect(state.calibration.pickerRequested) {
        if (state.calibration.pickerRequested) {
            pickCalibration.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    if (state.calibration.active) {
        CalibrationCaptureScreen(
            calibration = state.calibration,
            onScreenshotUri = viewModel::addCalibrationSample,
            onNeedPicker = viewModel::requestCalibrationPicker,
            onMessage = viewModel::calibrationMessage,
            onCancel = viewModel::cancelCalibration,
        )
        return
    }

    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val navType = NavigationSuiteScaffoldDefaults.navigationSuiteType(adaptiveInfo)

    NavigationSuiteScaffold(
        navigationSuiteType = navType,
        navigationItems = {
            Destination.entries.forEach { item ->
                NavigationSuiteItem(
                    navigationSuiteType = navType,
                    icon = { NavGlyph(item.glyph, selected = destination == item) },
                    label = { Text(item.label) },
                    selected = destination == item,
                    onClick = { destination = item },
                )
            }
        },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                ),
        ) {
            when (destination) {
                Destination.PROCESS -> ProcessScreen(
                    state = state,
                    onPick = {
                        pickImages.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onProcess = viewModel::processReady,
                    onRemove = viewModel::remove,
                    onClearFinished = viewModel::clearFinished,
                )
                Destination.CALIBRATE -> CalibrationHomeScreen(
                    state = state,
                    mediaAccess = ScreenshotMediaFinder(context).access(),
                    onRequestFullAccess = {
                        requestMediaAccess.launch(
                            arrayOf(
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                            ),
                        )
                    },
                    onStartSdr = { viewModel.startCalibration(false) },
                    onStartHdr = { viewModel.startCalibration(true) },
                    onReset = viewModel::resetModels,
                )
                Destination.SETTINGS -> SettingsScreen(
                    state = state,
                    onChooseOutput = { chooseOutput.launch(null) },
                    onDefaultOutput = { viewModel.setOutputTree(null) },
                    onQuality = viewModel::setJpegQuality,
                    onCleanHdr = viewModel::setCleanHdrGainMap,
                )
            }
        }
    }
}

@Composable
private fun NavGlyph(text: String, selected: Boolean) {
    Surface(
        modifier = Modifier.size(32.dp),
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun ProcessScreen(
    state: UiState,
    onPick: () -> Unit,
    onProcess: () -> Unit,
    onRemove: (android.net.Uri) -> Unit,
    onClearFinished: () -> Unit,
) {
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val directive = calculatePaneScaffoldDirective(adaptiveInfo)
    val twoPane = directive.maxHorizontalPartitions >= 2

    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PageHeader(
            title = "截图去水印",
            subtitle = if (state.modelReady) "模型已就绪 · 本地处理 · 支持 Ultra HDR" else "先完成一次校准，再处理截图",
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onPick) { Text("选择图片") }
            Button(
                onClick = onProcess,
                enabled = state.modelReady && state.queue.any { it.status == QueueStatus.READY || it.status == QueueStatus.ERROR },
            ) { Text("开始处理") }
            if (state.queue.any { it.status == QueueStatus.DONE }) {
                OutlinedButton(onClick = onClearFinished) { Text("清除已完成") }
            }
        }

        if (state.queue.isEmpty()) {
            EmptyProcessCard(state.modelReady)
        } else if (twoPane) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                QueueList(
                    queue = state.queue,
                    onRemove = onRemove,
                    modifier = Modifier.weight(0.56f).fillMaxHeight(),
                )
                QueueSummary(state.queue, Modifier.weight(0.44f).fillMaxHeight())
            }
        } else {
            QueueList(queue = state.queue, onRemove = onRemove, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun EmptyProcessCard(modelReady: Boolean) {
    GlassCard(Modifier.fillMaxWidth()) {
        Text(
            if (modelReady) "通过 Photo Picker 选择截图，或从系统分享菜单把截图发送到这里。"
            else "校准会显示 6 张已知灰阶并监听系统截图，用截图结果反推出逐像素水印模型。",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "所有计算都在设备本地完成；不会上传图片。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun QueueList(
    queue: List<QueueItem>,
    onRemove: (android.net.Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(queue, key = { it.uri.toString() }) { item ->
            QueueCard(item, onRemove)
        }
        item { Spacer(Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 12.dp)) }
    }
}

@Composable
private fun QueueCard(item: QueueItem, onRemove: (android.net.Uri) -> Unit) {
    val context = LocalContext.current
    val thumbnail by produceState<Bitmap?>(null, item.uri) {
        value = runCatching { context.contentResolver.loadThumbnail(item.uri, Size(240, 240), null) }.getOrNull()
    }
    GlassCard(Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.medium, modifier = Modifier.size(76.dp)) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    item.uri.lastPathSegment ?: "图片",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    when (item.status) {
                        QueueStatus.READY -> "等待处理"
                        QueueStatus.PROCESSING -> "处理中…"
                        QueueStatus.DONE -> item.message ?: "已完成"
                        QueueStatus.ERROR -> item.message ?: "处理失败"
                    },
                    color = when (item.status) {
                        QueueStatus.ERROR -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (item.hdr) Text("HDR", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (item.status != QueueStatus.PROCESSING) {
                OutlinedButton(onClick = { onRemove(item.uri) }) { Text("移除") }
            }
        }
    }
}

@Composable
private fun QueueSummary(queue: List<QueueItem>, modifier: Modifier = Modifier) {
    GlassCard(modifier) {
        Text("批处理", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        SummaryLine("总计", queue.size.toString())
        SummaryLine("已完成", queue.count { it.status == QueueStatus.DONE }.toString())
        SummaryLine("HDR", queue.count { it.hdr }.toString())
        SummaryLine("失败", queue.count { it.status == QueueStatus.ERROR }.toString())
        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        Text(
            "处理按顺序执行，避免同时解码多张 1440×3168 截图造成不必要的峰值内存。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CalibrationHomeScreen(
    state: UiState,
    mediaAccess: PhotoLibraryAccess,
    onRequestFullAccess: () -> Unit,
    onStartSdr: () -> Unit,
    onStartHdr: () -> Unit,
    onReset: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { PageHeader("校准", "像刚才一样，让已知灰阶经过系统截图链路，再拟合 Y = aX + b") }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("截图接收", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    when (mediaAccess) {
                        PhotoLibraryAccess.FULL -> "完整照片访问：检测到截图后会自动查询刚生成的 Screenshot。"
                        PhotoLibraryAccess.PARTIAL -> "仅选定照片访问：检测到截图后会打开 Photo Picker，请点选刚才的截图。"
                        PhotoLibraryAccess.NONE -> "无照片库访问：检测到截图后会打开 Photo Picker；普通处理不需要照片库权限。"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (mediaAccess != PhotoLibraryAccess.FULL) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = onRequestFullAccess) { Text("授权自动读取截图") }
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("SDR 六灰阶", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("0 / 32 / 64 / 128 / 192 / 255，按系统截图顺序自动收集。")
                Spacer(Modifier.height(10.dp))
                Button(onClick = onStartSdr) { Text("开始 SDR 校准") }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("HDR 链路校准", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "切换窗口到 HDR color mode，并用带 Gainmap 的已知灰阶进入截图链路。主图仍按六灰阶拟合；输出时同时清理 Ultra HDR gain map。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Button(onClick = onStartHdr) { Text("激活 HDR 并校准") }
            }
        }
        if (state.calibration.message != null) {
            item { GlassCard(Modifier.fillMaxWidth()) { Text(state.calibration.message) } }
        }
        if (state.modelReady) {
            item {
                OutlinedButton(onClick = onReset) { Text("删除当前模型") }
            }
        }
        item { Spacer(Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 12.dp)) }
    }
}

@Composable
private fun SettingsScreen(
    state: UiState,
    onChooseOutput: () -> Unit,
    onDefaultOutput: () -> Unit,
    onQuality: (Int) -> Unit,
    onCleanHdr: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val capabilities = remember { ColorOsCompat.detect(context) }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { PageHeader("设置", "导出、HDR 与平台适配") }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("输出位置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    state.outputTree?.toString() ?: "Pictures/截图去水印",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onChooseOutput) { Text("选择目录") }
                    OutlinedButton(onClick = onDefaultOutput) { Text("恢复默认") }
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("JPEG 质量 ${state.jpegQuality}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Slider(
                    value = state.jpegQuality.toFloat(),
                    onValueChange = { onQuality(it.toInt()) },
                    valueRange = 70f..100f,
                )
                Text(
                    "默认 Q90：保留截图细节，同时让逆合成后亚码值级尾影得到一次有损重量化。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("清理 HDR gain map", fontWeight = FontWeight.SemiBold)
                        Text("优先使用 gain profile；否则仅在已知水印掩膜内做确定性局部中值清理。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = state.cleanHdrGainMap, onCheckedChange = onCleanHdr)
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("ColorOS / 自由窗口", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    if (capabilities.colorOsFamily) "检测到 OPPO / OnePlus / realme 系平台。应用保持 resizeable，并让系统自由窗负责窗口级圆角、阴影与手势。"
                    else "使用标准 Android 自适应窗口路径。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "跨窗口模糊：${if (capabilities.crossWindowBlur) "系统支持" else "当前不可用"}。不通过反射调用 ColorOS 签名级私有材质 API，避免 OTA 后失效和隐藏 API 限制。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { Spacer(Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 12.dp)) }
    }
}

@Composable
private fun CalibrationCaptureScreen(
    calibration: CalibrationState,
    onScreenshotUri: (android.net.Uri) -> Unit,
    onNeedPicker: () -> Unit,
    onMessage: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current as? Activity
    val finder = remember { ScreenshotMediaFinder(context) }
    val scope = rememberCoroutineScope()
    val level = CalibrationEngine.LEVELS[calibration.levelIndex]
    var captureBusy by remember(calibration.samples.size) { mutableStateOf(false) }

    if (calibration.fitting || calibration.complete) {
        LaunchedEffect(Unit) {
            activity?.window?.colorMode = ActivityInfo.COLOR_MODE_DEFAULT
        }
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            GlassCard(Modifier.padding(24.dp)) {
                Text("正在拟合模型", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(calibration.message ?: "分析六张截图…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    DisposableEffect(activity, calibration.hdr, calibration.samples.size) {
        if (activity == null) return@DisposableEffect onDispose { }
        val window = activity.window
        val oldColorMode = window.colorMode
        window.colorMode = if (calibration.hdr) ActivityInfo.COLOR_MODE_HDR else ActivityInfo.COLOR_MODE_DEFAULT
        window.decorView.keepScreenOn = true
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val callback = Activity.ScreenCaptureCallback {
            if (captureBusy) return@ScreenCaptureCallback
            captureBusy = true
            val started = System.currentTimeMillis() - 2_000L
            scope.launch {
                if (finder.access() == PhotoLibraryAccess.FULL) {
                    var found: android.net.Uri? = null
                    repeat(18) {
                        found = finder.findAfter(started)
                        if (found != null) return@repeat
                        delay(120)
                    }
                    if (found != null) {
                        onScreenshotUri(found!!)
                    } else {
                        onMessage("检测到截图，但媒体库尚未找到文件；请从 Photo Picker 选择刚才的截图")
                        onNeedPicker()
                    }
                } else {
                    onNeedPicker()
                }
                captureBusy = false
            }
        }
        activity.registerScreenCaptureCallback(activity.mainExecutor, callback)
        onDispose {
            runCatching { activity.unregisterScreenCaptureCallback(callback) }
            window.colorMode = oldColorMode
            window.decorView.keepScreenOn = false
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    if (calibration.hdr) {
        HdrFlatCalibrationFrame(level)
    } else {
        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(level, level, level)))
    }

    // Hardware/system screenshot gesture is the intended control. Long-press/back is not placed on
    // the calibration frame because any visible control would contaminate the fit.
}

@Composable
private fun HdrFlatCalibrationFrame(level: Int) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(size, level) {
        bitmap?.let { old ->
            old.gainmap?.gainmapContents?.takeIf { !it.isRecycled }?.recycle()
            if (!old.isRecycled) old.recycle()
        }
        bitmap = if (size.width > 0 && size.height > 0) {
            HdrProbeFactory.create(size.width, size.height, level, HdrProbePattern.FLAT)
        } else null
    }
    DisposableEffect(Unit) {
        onDispose {
            bitmap?.let { old ->
                old.gainmap?.gainmapContents?.takeIf { !it.isRecycled }?.recycle()
                if (!old.isRecycled) old.recycle()
            }
        }
    }
    Box(
        Modifier.fillMaxSize().onSizeChanged { size = it }.background(androidx.compose.ui.graphics.Color(level, level, level)),
    ) {
        val current = bitmap
        if (current != null) {
            AndroidView(
                factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.FIT_XY } },
                update = { it.setImageBitmap(current) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun PageHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun GlassCard(modifier: Modifier = Modifier, content: @Composable Column.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(18.dp), content = content)
    }
}
