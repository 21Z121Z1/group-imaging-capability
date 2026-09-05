package io.github.z121z1.watermarkcleaner.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.util.Size
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
import io.github.z121z1.watermarkcleaner.core.CalibrationOrientation
import io.github.z121z1.watermarkcleaner.core.CalibrationTarget
import io.github.z121z1.watermarkcleaner.core.DynamicRange
import io.github.z121z1.watermarkcleaner.core.HdrProbeFactory
import io.github.z121z1.watermarkcleaner.core.HdrProbePattern
import io.github.z121z1.watermarkcleaner.data.PhotoLibraryAccess
import io.github.z121z1.watermarkcleaner.data.ScreenshotMediaFinder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Destination(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    PROCESS("处理", Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary),
    CALIBRATE("校准", Icons.Filled.Tune, Icons.Outlined.Tune),
    SETTINGS("设置", Icons.Filled.Settings, Icons.Outlined.Settings),
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WatermarkCleanerApp(viewModel: AppViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var destination by remember { mutableStateOf(Destination.PROCESS) }

    val pickImages = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(50),
    ) { uris -> viewModel.enqueue(uris) }

    val pickCalibration = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        viewModel.consumeCalibrationPickerRequest()
        if (uri != null) viewModel.addCalibrationSample(uri)
        else viewModel.calibrationMessage("未选择截图，请重新截取当前灰阶。")
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
                val selected = destination == item
                NavigationSuiteItem(
                    navigationSuiteType = navType,
                    icon = {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = null,
                        )
                    },
                    label = { Text(item.label) },
                    selected = selected,
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
                    onOpenCalibration = { destination = Destination.CALIBRATE },
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
                    onStart = viewModel::startCalibration,
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

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun ProcessScreen(
    state: UiState,
    onPick: () -> Unit,
    onOpenCalibration: () -> Unit,
    onProcess: () -> Unit,
    onRemove: (android.net.Uri) -> Unit,
    onClearFinished: () -> Unit,
) {
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val directive = calculatePaneScaffoldDirective(adaptiveInfo)
    val twoPane = directive.maxHorizontalPartitions >= 2
    val actionableCount = state.queue.count { it.status == QueueStatus.READY || it.status == QueueStatus.ERROR }
    val hasFinished = state.queue.any { it.status == QueueStatus.DONE }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PageHeader(
            title = "截图去水印",
            subtitle = if (state.modelReady) "选择截图并处理" else "先完成校准",
        )

        if (state.queue.isEmpty()) {
            EmptyProcessCard(
                modelReady = state.modelReady,
                onPick = onPick,
                onOpenCalibration = onOpenCalibration,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = onPick, modifier = Modifier.weight(1f)) {
                    Text("添加截图")
                }
                Button(
                    onClick = onProcess,
                    enabled = state.modelReady && actionableCount > 0,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (actionableCount > 0) "处理 $actionableCount 张" else "处理完成")
                }
            }
            if (hasFinished) {
                TextButton(onClick = onClearFinished) { Text("清除已完成") }
            }

            if (twoPane) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    QueueList(
                        queue = state.queue,
                        onRemove = onRemove,
                        modifier = Modifier.weight(0.58f).fillMaxHeight(),
                    )
                    QueueSummary(state.queue, Modifier.weight(0.42f).fillMaxHeight())
                }
            } else {
                QueueList(queue = state.queue, onRemove = onRemove, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun EmptyProcessCard(
    modelReady: Boolean,
    onPick: () -> Unit,
    onOpenCalibration: () -> Unit,
) {
    SectionCard(Modifier.fillMaxWidth()) {
        Text(
            if (modelReady) "还没有截图" else "先完成校准",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (modelReady) "选择截图后即可批量去除系统水印。" else "校准完成后即可开始处理截图。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = if (modelReady) onPick else onOpenCalibration) {
            Text(if (modelReady) "选择截图" else "开始校准")
        }
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
    val dark = isSystemInDarkTheme()
    val thumbnailOutline = if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.10f)
    val thumbnail by produceState<Bitmap?>(null, item.uri) {
        value = runCatching { context.contentResolver.loadThumbnail(item.uri, Size(240, 240), null) }.getOrNull()
    }

    SectionCard(Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(72.dp),
                border = BorderStroke(1.dp, thumbnailOutline),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHighest))
                }
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    item.uri.lastPathSegment ?: "图片",
                    maxLines = 2,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    when (item.status) {
                        QueueStatus.READY -> "等待处理"
                        QueueStatus.PROCESSING -> "处理中…"
                        QueueStatus.DONE -> item.message ?: "已完成"
                        QueueStatus.ERROR -> item.message ?: "处理失败，请重试"
                    },
                    color = if (item.status == QueueStatus.ERROR) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (item.hdr) HdrBadge()
            }

            if (item.status == QueueStatus.PROCESSING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                TextButton(onClick = { onRemove(item.uri) }) { Text("移除") }
            }
        }
    }
}

@Composable
private fun HdrBadge() {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Text(
            "HDR",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun QueueSummary(queue: List<QueueItem>, modifier: Modifier = Modifier) {
    SectionCard(modifier) {
        Text("批处理", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        SummaryLine("总计", queue.size.toString())
        SummaryLine("已完成", queue.count { it.status == QueueStatus.DONE }.toString())
        SummaryLine("HDR", queue.count { it.hdr }.toString())
        SummaryLine("失败", queue.count { it.status == QueueStatus.ERROR }.toString())
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
    onStart: (CalibrationTarget) -> Unit,
    onReset: () -> Unit,
) {
    val portraitSdr = CalibrationTarget(CalibrationOrientation.PORTRAIT, DynamicRange.SDR)
    val landscapeSdr = CalibrationTarget(CalibrationOrientation.LANDSCAPE, DynamicRange.SDR)
    val portraitHdr = CalibrationTarget(CalibrationOrientation.PORTRAIT, DynamicRange.HDR)
    val landscapeHdr = CalibrationTarget(CalibrationOrientation.LANDSCAPE, DynamicRange.HDR)
    var confirmReset by remember { mutableStateOf(false) }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("删除全部校准数据？") },
            text = { Text("删除后需要重新校准才能处理截图。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReset = false
                        onReset()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("删除校准数据")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("取消") }
            },
        )
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { PageHeader("校准", "分别校准竖屏、横屏与 HDR") }
        item {
            CaptureAccessCard(
                mediaAccess = mediaAccess,
                onRequestFullAccess = onRequestFullAccess,
            )
        }
        item {
            CalibrationGroup(
                title = "SDR",
                subtitle = "6 张灰阶截图",
                portraitTarget = portraitSdr,
                landscapeTarget = landscapeSdr,
                calibratedTargets = state.calibratedTargets,
                onStart = onStart,
            )
        }
        item {
            CalibrationGroup(
                title = "HDR / P3",
                subtitle = "6 张 HDR / P3 灰阶截图",
                portraitTarget = portraitHdr,
                landscapeTarget = landscapeHdr,
                calibratedTargets = state.calibratedTargets,
                onStart = onStart,
            )
        }
        if (state.calibration.message != null) {
            item {
                SectionCard(Modifier.fillMaxWidth()) {
                    Text(state.calibration.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (state.modelReady) {
            item {
                TextButton(
                    onClick = { confirmReset = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("删除全部校准数据")
                }
            }
        }
        item { Spacer(Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 12.dp)) }
    }
}

@Composable
private fun CaptureAccessCard(
    mediaAccess: PhotoLibraryAccess,
    onRequestFullAccess: () -> Unit,
) {
    SectionCard(Modifier.fillMaxWidth()) {
        Text("自动获取截图", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            when (mediaAccess) {
                PhotoLibraryAccess.FULL -> "已开启。截图后会自动进入下一步。"
                PhotoLibraryAccess.PARTIAL -> "需要完整照片访问；否则会打开照片选择器。"
                PhotoLibraryAccess.NONE -> "未授权时会打开照片选择器。"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (mediaAccess != PhotoLibraryAccess.FULL) {
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = onRequestFullAccess) { Text("允许自动获取") }
        }
    }
}

@Composable
private fun CalibrationGroup(
    title: String,
    subtitle: String,
    portraitTarget: CalibrationTarget,
    landscapeTarget: CalibrationTarget,
    calibratedTargets: Set<CalibrationTarget>,
    onStart: (CalibrationTarget) -> Unit,
) {
    SectionCard(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalButton(
                onClick = { onStart(portraitTarget) },
                modifier = Modifier.weight(1f),
            ) {
                Text(if (portraitTarget in calibratedTargets) "重新校准竖屏" else "校准竖屏")
            }
            FilledTonalButton(
                onClick = { onStart(landscapeTarget) },
                modifier = Modifier.weight(1f),
            ) {
                Text(if (landscapeTarget in calibratedTargets) "重新校准横屏" else "校准横屏")
            }
        }
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
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { PageHeader("设置", "输出与画质") }
        item {
            SectionCard(Modifier.fillMaxWidth()) {
                Text("保存位置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    if (state.outputTree == null) "Pictures/截图去水印" else "自定义文件夹",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(onClick = onChooseOutput) { Text("选择文件夹") }
                    if (state.outputTree != null) {
                        TextButton(onClick = onDefaultOutput) { Text("使用默认位置") }
                    }
                }

                Spacer(Modifier.height(22.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("JPEG 质量", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Q${state.jpegQuality}", fontWeight = FontWeight.SemiBold)
                }
                Slider(
                    value = state.jpegQuality.toFloat(),
                    onValueChange = { onQuality(it.toInt()) },
                    valueRange = 70f..100f,
                    steps = 29,
                )
                Text(
                    "默认 Q90；压缩也用于减弱残余水印。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            SectionCard(
                Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = state.cleanHdrGainMap,
                        role = Role.Switch,
                        onValueChange = onCleanHdr,
                    ),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("清理 Ultra HDR 增益图", fontWeight = FontWeight.SemiBold)
                        Text(
                            "对带增益图的图片同时处理 HDR 层。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = state.cleanHdrGainMap, onCheckedChange = null)
                }
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

    BackHandler(onBack = onCancel)

    if (calibration.fitting || calibration.complete) {
        LaunchedEffect(Unit) {
            activity?.window?.colorMode = ActivityInfo.COLOR_MODE_DEFAULT
        }
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            SectionCard(Modifier.padding(24.dp)) {
                Text("正在拟合模型", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(calibration.message ?: "正在分析 6 张截图…", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            val started = System.currentTimeMillis() - 1_500L
            val excluded = calibration.samples.toSet()
            scope.launch {
                try {
                    if (finder.access() == PhotoLibraryAccess.FULL) {
                        repeat(48) {
                            val found = finder.findAfter(started, excluded)
                            if (found != null) {
                                onScreenshotUri(found)
                                return@launch
                            }
                            delay(250)
                        }
                        onMessage("未能自动读取刚才的截图，请在照片选择器中选择它。")
                        onNeedPicker()
                    } else {
                        onNeedPicker()
                    }
                } finally {
                    captureBusy = false
                }
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
private fun PageHeader(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        if (!subtitle.isNullOrBlank()) {
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SectionCard(modifier: Modifier = Modifier, content: @Composable Column.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}
