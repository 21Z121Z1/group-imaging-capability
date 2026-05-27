package com.oplus.groupimaging.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.oplus.groupimaging.domain.AssetPreview
import com.oplus.groupimaging.domain.InsightBucket
import com.oplus.groupimaging.domain.RuleGroupSummary
import com.oplus.groupimaging.testing.TestTags
import com.oplus.groupimaging.ui.theme.OplusInsightTheme
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Column {
                Text(title, modifier = Modifier.semantics { heading() })
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.labelMedium)
                }
            }
        },
    )
}

@Composable
fun HeroSummaryCard(
    title: String,
    headline: String,
    supporting: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(OplusInsightTheme.shapes.heroCorner)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .semantics(mergeDescendants = true) {}
                        .clickable { onClick() }
                } else {
                    Modifier
                },
            ),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = OplusInsightTheme.elevation.hero),
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                )
                .padding(OplusInsightTheme.spacing.lg),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(OplusInsightTheme.spacing.sm)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(headline, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text(supporting, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun SectionContainer(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    actionModifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(OplusInsightTheme.shapes.cardCorner),
    ) {
        Column(
            modifier = Modifier.padding(OplusInsightTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(OplusInsightTheme.spacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                if (actionLabel != null && onAction != null) {
                    TextButton(onClick = onAction, modifier = actionModifier) {
                        Text(actionLabel)
                    }
                }
            }
            content()
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(OplusInsightTheme.shapes.cardCorner)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.semantics(mergeDescendants = true) {
                contentDescription = "$title $value"
            },
            shape = shape,
        ) {
            Column(
                modifier = Modifier.padding(OplusInsightTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(OplusInsightTheme.spacing.xs),
            ) {
                Text(title, style = MaterialTheme.typography.labelLarge)
                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        Card(
            modifier = modifier,
            shape = shape,
        ) {
            Column(
                modifier = Modifier.padding(OplusInsightTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(OplusInsightTheme.spacing.xs),
            ) {
                Text(title, style = MaterialTheme.typography.labelLarge)
                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RatioStatCard(
    title: String,
    value: String,
    supporting: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.then(
            if (onClick != null) {
                Modifier
                    .semantics(mergeDescendants = true) {}
                    .clickable { onClick() }
            } else {
                Modifier
            },
        ),
        shape = RoundedCornerShape(OplusInsightTheme.shapes.cardCorner),
    ) {
        Column(
            modifier = Modifier.padding(OplusInsightTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(OplusInsightTheme.spacing.xs),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(value, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(supporting, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun TrendMiniChartCard(
    title: String,
    buckets: List<InsightBucket>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val chartColors = OplusInsightTheme.chartColors.series
    val displayBuckets = buckets.take(12)
    Card(
        modifier = modifier.then(
            if (onClick != null) {
                Modifier
                    .semantics(mergeDescendants = true) {}
                    .clickable { onClick() }
            } else {
                Modifier
            },
        ),
        shape = RoundedCornerShape(OplusInsightTheme.shapes.cardCorner),
    ) {
        Column(
            modifier = Modifier.padding(OplusInsightTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(OplusInsightTheme.spacing.sm),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .semantics {
                        contentDescription = "$title，${displayBuckets.joinToString { "${it.label} ${it.count}" }}"
                    },
            ) {
                if (displayBuckets.isEmpty()) return@Canvas
                val max = displayBuckets.maxOf { it.count }.coerceAtLeast(1)
                val width = size.width / displayBuckets.size
                displayBuckets.forEachIndexed { index, bucket ->
                    val height = size.height * (bucket.count.toFloat() / max.toFloat())
                    drawLine(
                        color = chartColors[index % chartColors.size],
                        start = androidx.compose.ui.geometry.Offset(width * index + width / 2, size.height),
                        end = androidx.compose.ui.geometry.Offset(width * index + width / 2, size.height - height),
                        strokeWidth = width * 0.5f,
                        cap = StrokeCap.Round,
                    )
                }
            }
            if (displayBuckets.isEmpty()) {
                Text("暂无趋势数据", style = MaterialTheme.typography.bodyMedium)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    displayBuckets.forEach { bucket ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(bucket.label, style = MaterialTheme.typography.bodyMedium)
                            Text("${bucket.count} 张", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun FilterChipRow(
    chips: List<String>,
    onRemove: (String) -> Unit,
    onClear: (() -> Unit)? = null,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chips.forEach { chip ->
            AssistChip(
                onClick = { onRemove(chip) },
                label = { Text(chip) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics {
                        contentDescription = "移除筛选：$chip"
                    },
            )
        }
        if (chips.isNotEmpty() && onClear != null) {
            AssistChip(
                onClick = onClear,
                label = { Text("清空") },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
}

@Composable
fun EmptyStateView(
    title: String,
    body: String,
    primaryAction: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    primaryActionModifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(OplusInsightTheme.shapes.cardCorner),
    ) {
        Column(
            modifier = Modifier.padding(OplusInsightTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(OplusInsightTheme.spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            if (primaryAction != null && onPrimaryAction != null) {
                TextButton(onClick = onPrimaryAction, modifier = primaryActionModifier) {
                    Text(primaryAction)
                }
            }
        }
    }
}

@Composable
fun ErrorStateView(
    title: String,
    body: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                error(body)
            },
        shape = RoundedCornerShape(OplusInsightTheme.shapes.cardCorner),
        colors = CardDefaults.cardColors(containerColor = OplusInsightTheme.danger.dangerContainer),
    ) {
        Column(
            modifier = Modifier.padding(OplusInsightTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(OplusInsightTheme.spacing.sm),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = OplusInsightTheme.danger.danger)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            if (onRetry != null) {
                TextButton(onClick = onRetry) {
                    Text("重试", color = OplusInsightTheme.danger.danger)
                }
            }
        }
    }
}

@Composable
fun RuleGroupCard(
    item: RuleGroupSummary,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(OplusInsightTheme.shapes.cardCorner),
    ) {
        Column(
            modifier = Modifier.padding(OplusInsightTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(OplusInsightTheme.spacing.xs),
        ) {
            Text(item.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("命中 ${item.count}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun AssetPreviewItem(
    item: AssetPreview,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(OplusInsightTheme.shapes.cardCorner),
    ) {
        Row(
            modifier = Modifier.padding(OplusInsightTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(OplusInsightTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MediaThumbnail(item = item)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.fileName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    listOfNotNull(
                        item.captureDate?.toString(),
                        item.deviceModel,
                        item.focalLengthEq?.let { "${it}mm" },
                        item.captureModeLabel,
                        "Live".takeIf { item.isLivePhoto },
                        "RAW".takeIf { item.isRaw },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun MediaThumbnail(
    item: AssetPreview,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { ThumbnailSize.roundToPx() }
    val thumbnail by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = item.assetUri,
        key2 = sizePx,
    ) {
        value = loadThumbnailImage(context, item.assetUri, sizePx)
    }
    val shape = RoundedCornerShape(12.dp)
    if (thumbnail != null) {
        Image(
            bitmap = thumbnail!!,
            contentDescription = "${item.fileName} 缩略图",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(ThumbnailSize)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    } else {
        Box(
            modifier = modifier
                .size(ThumbnailSize)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .semantics {
                    contentDescription = "${item.fileName} 缩略图不可用"
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.fileName.previewInitials(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private suspend fun loadThumbnailImage(
    context: Context,
    uriString: String,
    sizePx: Int,
): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val uri = Uri.parse(uriString)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver
                .loadThumbnail(uri, Size(sizePx, sizePx), null)
                .asImageBitmap()
        } else {
            decodeSampledBitmap(context, uri, sizePx)?.asImageBitmap()
        }
    }.getOrNull()
}

private fun decodeSampledBitmap(
    context: Context,
    uri: Uri,
    sizePx: Int,
): android.graphics.Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val options = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(bounds, sizePx, sizePx)
    }
    return context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)
    }
}

private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int,
): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        var halfHeight = height / 2
        var halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

private fun String.previewInitials(): String {
    val extension = substringAfterLast('.', missingDelimiterValue = "")
        .takeIf { it.isNotBlank() }
        ?.take(4)
        ?.uppercase(Locale.US)
    return extension ?: take(2).uppercase(Locale.US)
}

private val ThumbnailSize = 56.dp

@Composable
fun ProgressHeroCard(
    title: String,
    supporting: String,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f)
            },
        shape = RoundedCornerShape(OplusInsightTheme.shapes.heroCorner),
    ) {
        Column(
            modifier = Modifier.padding(OplusInsightTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(OplusInsightTheme.spacing.sm),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            androidx.compose.material3.LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Text(supporting, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun PermissionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(TestTags.PermissionDialog.ROOT),
        title = { Text("需要媒体权限") },
        text = { Text("授权后才能扫描本机拍摄的照片并建立索引。") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(TestTags.PermissionDialog.CONFIRM),
            ) {
                Text("继续授权")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TestTags.PermissionDialog.DISMISS),
            ) {
                Text("稍后")
            }
        },
    )
}

@Composable
fun MonthGrid(
    items: List<InsightBucket>,
    onClick: (InsightBucket) -> Unit,
) {
    LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxWidth().height(240.dp)) {
        items(items.size) { index ->
            val item = items[index]
            Card(
                modifier = Modifier
                    .padding(6.dp)
                    .semantics {
                        contentDescription = "${item.label}，${item.count} 张"
                    }
                    .clickable { onClick(item) },
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(item.label, style = MaterialTheme.typography.titleSmall)
                    Text(item.count.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
