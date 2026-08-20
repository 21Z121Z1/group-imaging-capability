package dev.groupimaging.unmark.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.groupimaging.unmark.UnmarkViewModel
import dev.groupimaging.unmark.media.MediaAccess

private enum class Destination(val label: String) {
    Process("处理"),
    Calibrate("校准"),
    Settings("设置"),
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun UnmarkApp(viewModel: UnmarkViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val capture = state.calibration as? UnmarkViewModel.CalibrationState.Capturing
    if (capture != null) {
        BackHandler { viewModel.cancelCalibration() }
        CalibrationCaptureSurface(capture.level)
        return
    }

    var destination by remember { mutableStateOf(Destination.Process) }
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val navigationType = NavigationSuiteScaffoldDefaults.navigationSuiteType(adaptiveInfo)
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        viewModel.dismissMessage()
    }

    NavigationSuiteScaffold(
        navigationSuiteType = navigationType,
        navigationItems = {
            Destination.entries.forEach { item ->
                NavigationSuiteItem(
                    navigationSuiteType = navigationType,
                    selected = destination == item,
                    onClick = { destination = item },
                    icon = {
                        Icon(
                            imageVector = when (item) {
                                Destination.Process -> Icons.Filled.AddPhotoAlternate
                                Destination.Calibrate -> Icons.Filled.Tune
                                Destination.Settings -> Icons.Filled.Settings
                            },
                            contentDescription = null,
                        )
                    },
                    label = { Text(item.label) },
                )
            }
        },
    ) {
        Surface(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    when (destination) {
                        Destination.Process -> ProcessPage(adaptiveInfo, state, viewModel)
                        Destination.Calibrate -> CalibrationPage(state, viewModel)
                        Destination.Settings -> SettingsPage(state, viewModel)
                    }
                }
                SnackbarHost(
                    hostState = snackbar,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun CalibrationCaptureSurface(level: Int) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(red = level, green = level, blue = level)),
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun ProcessPage(
    adaptiveInfo: WindowAdaptiveInfo,
    state: UnmarkViewModel.UiState,
    viewModel: UnmarkViewModel,
) {
    val directive = calculatePaneScaffoldDirective(adaptiveInfo)
    AdaptiveTwoPane(
        directive = directive,
        posture = adaptiveInfo.windowPosture,
        primary = { PreviewPane(state) },
        secondary = { ProcessControls(state, viewModel) },
    )
}

@Composable
private fun PreviewPane(state: UnmarkViewModel.UiState) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("去印", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        Text(
            "从照片选择器或系统分享菜单加入截图。处理完全在设备本地完成。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.largeIncreased)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            val selected = state.jobs.firstOrNull()
            if (selected == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("选择截图开始", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        Icons.Filled.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        selected.uri.lastPathSegment ?: "已选择图片",
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        when (selected.status) {
                            UnmarkViewModel.JobStatus.Queued -> "等待处理"
                            UnmarkViewModel.JobStatus.Processing -> "处理中"
                            UnmarkViewModel.JobStatus.Done -> if (selected.wasUltraHdr) "已处理 · Ultra HDR" else "已处理"
                            UnmarkViewModel.JobStatus.Failed -> "处理失败"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProcessControls(state: UnmarkViewModel.UiState, viewModel: UnmarkViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            FloatingGlassSurface {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("处理", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (state.profile == null) "尚未校准" else "模型 ${state.profile.width}×${state.profile.height} · ${state.profile.size} px",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = viewModel::requestImagePicker,
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                    ) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("选择图片")
                    }
                    FilledTonalButton(
                        onClick = viewModel::processQueued,
                        enabled = !state.busy && state.jobs.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        if (state.busy) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.size(8.dp))
                        }
                        Text("去除水印")
                    }
                }
            }
        }
        item { Text("队列", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        if (state.jobs.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Text("暂无待处理图片", Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(state.jobs, key = { it.uri.toString() }) { job ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(job.uri.lastPathSegment ?: "图片", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            when (job.status) {
                                UnmarkViewModel.JobStatus.Queued -> "等待处理"
                                UnmarkViewModel.JobStatus.Processing -> "处理中"
                                UnmarkViewModel.JobStatus.Done -> buildString {
                                    append("完成")
                                    if (job.wasUltraHdr) append(if (job.ultraHdrVerified == true) " · HDR 已验证" else " · HDR 未验证")
                                }
                                UnmarkViewModel.JobStatus.Failed -> "失败 · ${job.error.orEmpty()}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (!state.busy && job.status != UnmarkViewModel.JobStatus.Processing) {
                            OutlinedButton(onClick = { viewModel.removeJob(job.uri) }) { Text("移出队列") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalibrationPage(state: UnmarkViewModel.UiState, viewModel: UnmarkViewModel) {
    val activity = LocalActivity.current
    val calibration = state.calibration
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("校准", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "六档已知灰阶逐像素拟合 Y = aX + b。截图监听只在纯色捕获面开启；完整照片权限仅用于自动取得刚生成的 Screenshot。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("截图导入", style = MaterialTheme.typography.titleMedium)
                    Text(
                        when (state.mediaAccess) {
                            MediaAccess.Full -> "完整照片访问 · 截图后自动导入"
                            MediaAccess.Partial -> "Selected Photos · 截图后使用 Photo Picker"
                            MediaAccess.PickerOnly -> "无需照片库权限 · 截图后使用 Photo Picker"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.mediaAccess != MediaAccess.Full) {
                        OutlinedButton(onClick = viewModel::requestFullMediaAccess) { Text("允许自动导入截图") }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("当前模型", style = MaterialTheme.typography.titleMedium)
                    Text(
                        state.profile?.let { "${it.width}×${it.height} · ${it.size} 个水印像素" } ?: "尚未校准",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            when (calibration) {
                UnmarkViewModel.CalibrationState.Idle -> Unit
                UnmarkViewModel.CalibrationState.Fitting -> Text("正在拟合并验证模型…")
                is UnmarkViewModel.CalibrationState.Complete -> Text(
                    "校准完成：${calibration.width}×${calibration.height}，${calibration.pixels} 个有效水印像素",
                    color = MaterialTheme.colorScheme.primary,
                )
                is UnmarkViewModel.CalibrationState.Error -> Text(
                    calibration.message,
                    color = MaterialTheme.colorScheme.error,
                )
                is UnmarkViewModel.CalibrationState.Capturing -> Unit
            }
        }
        item {
            Button(
                onClick = { viewModel.startCalibration(canUseFullScreen = activity?.isInMultiWindowMode == false) },
                enabled = calibration !is UnmarkViewModel.CalibrationState.Fitting,
                modifier = Modifier.fillMaxWidth().height(64.dp),
            ) {
                Text("开始六灰阶校准")
            }
        }
    }
}

@Composable
private fun SettingsPage(state: UnmarkViewModel.UiState, viewModel: UnmarkViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("设置", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("输出位置", style = MaterialTheme.typography.titleMedium)
                    Text(
                        state.outputTree?.toString() ?: "Pictures/去印",
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = viewModel::requestOutputDirectory) { Text("选择目录") }
                        if (state.outputTree != null) {
                            OutlinedButton(onClick = { viewModel.setOutputTree(null) }) { Text("恢复默认") }
                        }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("JPEG 质量 · ${state.jpegQuality}", style = MaterialTheme.typography.titleMedium)
                    Text("高质量 JPEG 重量化可进一步压掉亚码值级逆合成残差。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = state.jpegQuality.toFloat(),
                        onValueChange = { viewModel.setJpegQuality(it.toInt()) },
                        valueRange = 70f..100f,
                        steps = 29,
                    )
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Ultra HDR", style = MaterialTheme.typography.titleMedium)
                    Text("处理前缓存 Gainmap，修改 SDR primary 后重新挂回，并在导出后重新解码检查 gain map。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("ColorOS 小窗 / 自由窗口", style = MaterialTheme.typography.titleMedium)
                    Text("普通处理界面实时响应窗口尺寸与折叠 posture；只有逐像素校准要求全屏。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun FloatingGlassSurface(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.largeIncreased,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
    ) {
        Box(Modifier.padding(20.dp)) { content() }
    }
}