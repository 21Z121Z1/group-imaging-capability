package dev.groupimaging.unmark.ui

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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class Destination(val label: String) {
    Process("处理"),
    Calibrate("校准"),
    Settings("设置"),
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun UnmarkApp() {
    var destination by remember { mutableStateOf(Destination.Process) }
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val navigationType = NavigationSuiteScaffoldDefaults.navigationSuiteType(adaptiveInfo)

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
            Box(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                when (destination) {
                    Destination.Process -> ProcessPage(adaptiveInfo)
                    Destination.Calibrate -> CalibrationPage()
                    Destination.Settings -> SettingsPage()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun ProcessPage(adaptiveInfo: androidx.compose.material3.adaptive.WindowAdaptiveInfo) {
    val directive = calculatePaneScaffoldDirective(adaptiveInfo)
    AdaptiveTwoPane(
        directive = directive,
        posture = adaptiveInfo.windowPosture,
        primary = { PreviewPane() },
        secondary = { ProcessControls() },
    )
}

@Composable
private fun PreviewPane() {
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
        }
    }
}

@Composable
private fun ProcessControls() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            FloatingGlassSurface {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("处理", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("选择图片")
                    }
                    FilledTonalButton(onClick = {}, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Text("去除水印")
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("队列", style = MaterialTheme.typography.titleMedium)
                    Text("暂无待处理图片", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun CalibrationPage() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("校准", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "用 0 / 32 / 64 / 128 / 192 / 255 六档已知灰阶拟合逐像素水印模型。校准捕获期间才监听系统截图。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(listOf("1. 检查截图权限", "2. 六灰阶截图", "3. HDR 探针", "4. 验证并保存模型")) { step ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(step, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        item {
            Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                Text("开始校准")
            }
        }
    }
}

@Composable
private fun SettingsPage() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("设置", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        }
        items(
            listOf(
                "输出位置 · Pictures/去印",
                "JPEG 质量 · 90",
                "Ultra HDR · 保留 Gain Map",
                "ColorOS 小窗 · 自适应布局",
            ),
        ) { text ->
            Card(Modifier.fillMaxWidth()) {
                Text(text, Modifier.padding(20.dp), style = MaterialTheme.typography.titleMedium)
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
