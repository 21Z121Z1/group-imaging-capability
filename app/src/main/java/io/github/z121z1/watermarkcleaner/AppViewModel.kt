package io.github.z121z1.watermarkcleaner

import android.app.Application
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.z121z1.watermarkcleaner.core.CalibrationEngine
import io.github.z121z1.watermarkcleaner.core.CalibrationOrientation
import io.github.z121z1.watermarkcleaner.core.CalibrationTarget
import io.github.z121z1.watermarkcleaner.core.DynamicRange
import io.github.z121z1.watermarkcleaner.core.GainMapCalibrationEngine
import io.github.z121z1.watermarkcleaner.core.ProfileRepository
import io.github.z121z1.watermarkcleaner.core.WatermarkProcessor
import io.github.z121z1.watermarkcleaner.data.AppSettings
import io.github.z121z1.watermarkcleaner.data.ImageExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class QueueItem(
    val uri: Uri,
    val status: QueueStatus = QueueStatus.READY,
    val output: Uri? = null,
    val message: String? = null,
    val hdr: Boolean = false,
)

enum class QueueStatus { READY, PROCESSING, DONE, ERROR }

data class CalibrationState(
    val active: Boolean = false,
    val target: CalibrationTarget? = null,
    val samples: List<Uri> = emptyList(),
    val pickerRequested: Boolean = false,
    val fitting: Boolean = false,
    val message: String? = null,
) {
    val hdr: Boolean get() = target?.dynamicRange == DynamicRange.HDR
    val levelIndex: Int get() = samples.size.coerceAtMost(CalibrationEngine.LEVELS.lastIndex)
    val complete: Boolean get() = samples.size == CalibrationEngine.LEVELS.size
}

data class UiState(
    val queue: List<QueueItem> = emptyList(),
    val calibration: CalibrationState = CalibrationState(),
    val modelReady: Boolean = false,
    val calibratedTargets: Set<CalibrationTarget> = emptySet(),
    val outputTree: Uri? = null,
    val jpegQuality: Int = 90,
    val cleanHdrGainMap: Boolean = true,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "WatermarkCleaner"
    }

    private val context = application.applicationContext
    private val profiles = ProfileRepository(context)
    private val settings = AppSettings(context)
    private val processor = WatermarkProcessor(context.contentResolver, profiles)
    private val exporter = ImageExporter(context.contentResolver, settings)
    private val calibrationEngine = CalibrationEngine(context.contentResolver)
    private val gainMapCalibrationEngine = GainMapCalibrationEngine(context.contentResolver)

    private val _state = MutableStateFlow(
        UiState(
            modelReady = profiles.hasAnyBase(),
            calibratedTargets = profiles.calibratedTargets(),
            outputTree = settings.outputTree,
            jpegQuality = settings.jpegQuality,
            cleanHdrGainMap = settings.cleanHdrGainMap,
        ),
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun enqueue(uris: Collection<Uri>) {
        if (uris.isEmpty()) return
        val existing = _state.value.queue.mapTo(hashSetOf()) { it.uri }
        val newItems = uris.filterNot(existing::contains).map(::QueueItem)
        _state.value = _state.value.copy(queue = _state.value.queue + newItems)
    }

    fun remove(uri: Uri) {
        _state.value = _state.value.copy(queue = _state.value.queue.filterNot { it.uri == uri })
    }

    fun clearFinished() {
        _state.value = _state.value.copy(queue = _state.value.queue.filter { it.status != QueueStatus.DONE })
    }

    fun processReady() {
        viewModelScope.launch {
            val targets = _state.value.queue.filter { it.status == QueueStatus.READY || it.status == QueueStatus.ERROR }
            for (target in targets) {
                updateQueue(target.uri) { it.copy(status = QueueStatus.PROCESSING, message = null) }
                try {
                    val result = processor.process(target.uri, settings.cleanHdrGainMap)
                    val output = try {
                        exporter.export(result.bitmap, target.uri)
                    } finally {
                        result.bitmap.recycle()
                    }
                    updateQueue(target.uri) {
                        it.copy(
                            status = QueueStatus.DONE,
                            output = output,
                            hdr = result.wasHdr,
                            message = "已保存",
                        )
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "Processing failed for ${target.uri}", t)
                    updateQueue(target.uri) {
                        it.copy(status = QueueStatus.ERROR, message = processErrorMessage(t))
                    }
                }
            }
        }
    }

    /**
     * The original MD3 screen exposes one button per dynamic range. Prefer the current orientation,
     * but once that slot is calibrated automatically select the still-missing orientation. This
     * makes the second run enter the independent landscape profile even when auto-rotate is locked.
     */
    fun startCalibration(hdr: Boolean) {
        val range = if (hdr) DynamicRange.HDR else DynamicRange.SDR
        val preferred = if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            CalibrationOrientation.LANDSCAPE
        } else {
            CalibrationOrientation.PORTRAIT
        }
        val calibrated = profiles.calibratedTargets()
        val preferredTarget = CalibrationTarget(preferred, range)
        val otherOrientation = if (preferred == CalibrationOrientation.PORTRAIT) {
            CalibrationOrientation.LANDSCAPE
        } else {
            CalibrationOrientation.PORTRAIT
        }
        val otherTarget = CalibrationTarget(otherOrientation, range)
        val target = when {
            preferredTarget !in calibrated -> preferredTarget
            otherTarget !in calibrated -> otherTarget
            else -> preferredTarget
        }
        startCalibration(target)
    }

    fun startCalibration(target: CalibrationTarget) {
        _state.value = _state.value.copy(calibration = CalibrationState(active = true, target = target))
    }

    fun cancelCalibration() {
        _state.value = _state.value.copy(calibration = CalibrationState())
    }

    fun requestCalibrationPicker() {
        val c = _state.value.calibration
        if (c.active) _state.value = _state.value.copy(calibration = c.copy(pickerRequested = true))
    }

    fun consumeCalibrationPickerRequest() {
        val c = _state.value.calibration
        _state.value = _state.value.copy(calibration = c.copy(pickerRequested = false))
    }

    fun addCalibrationSample(uri: Uri) {
        val current = _state.value.calibration
        if (!current.active || current.fitting || current.complete || uri in current.samples) return
        val updated = current.copy(samples = current.samples + uri, pickerRequested = false, message = null)
        _state.value = _state.value.copy(calibration = updated)
        if (updated.complete) fitCalibration()
    }

    fun calibrationMessage(message: String) {
        val c = _state.value.calibration
        if (c.active) _state.value = _state.value.copy(calibration = c.copy(message = message))
    }

    private fun fitCalibration() {
        val snapshot = _state.value.calibration
        val target = snapshot.target ?: return
        if (!snapshot.complete || snapshot.fitting) return
        _state.value = _state.value.copy(
            calibration = snapshot.copy(
                fitting = true,
                message = "正在分析截图…",
            ),
        )
        viewModelScope.launch {
            try {
                val baseProfile = calibrationEngine.fit(snapshot.samples)
                require(target.orientation.matches(baseProfile.width, baseProfile.height)) {
                    "截图方向与 ${target.orientation.label} 校准目标不一致：${baseProfile.width}×${baseProfile.height}"
                }

                val hasGainMap = target.dynamicRange == DynamicRange.HDR && sampleHasGainMap(snapshot.samples.first())
                val gainProfile = if (hasGainMap) {
                    gainMapCalibrationEngine.fit(snapshot.samples).also {
                        require(it.baseWidth == baseProfile.width && it.baseHeight == baseProfile.height)
                    }
                } else {
                    null
                }

                withContext(Dispatchers.IO) {
                    if (target.dynamicRange == DynamicRange.HDR) {
                        if (gainProfile != null) profiles.saveHdrGain(gainProfile)
                        else profiles.deleteHdrGain(baseProfile.width, baseProfile.height)
                    }
                    profiles.saveBase(target.dynamicRange, baseProfile)
                }
                _state.value = _state.value.copy(
                    modelReady = true,
                    calibratedTargets = profiles.calibratedTargets(),
                    calibration = CalibrationState(
                        active = false,
                        message = "${target.label} 校准完成",
                    ),
                )
            } catch (t: Throwable) {
                Log.w(TAG, "Calibration failed for ${target.label}", t)
                _state.value = _state.value.copy(
                    calibration = snapshot.copy(
                        fitting = false,
                        message = calibrationErrorMessage(t),
                    ),
                )
            }
        }
    }

    private suspend fun sampleHasGainMap(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: return@withContext false
        try {
            bitmap.gainmap != null
        } finally {
            bitmap.recycle()
        }
    }

    fun setOutputTree(uri: Uri?) {
        settings.outputTree = uri
        _state.value = _state.value.copy(outputTree = uri)
    }

    fun setJpegQuality(value: Int) {
        settings.jpegQuality = value
        _state.value = _state.value.copy(jpegQuality = settings.jpegQuality)
    }

    fun setCleanHdrGainMap(value: Boolean) {
        settings.cleanHdrGainMap = value
        _state.value = _state.value.copy(cleanHdrGainMap = value)
    }

    fun resetModels() {
        profiles.clear()
        _state.value = _state.value.copy(modelReady = false, calibratedTargets = emptySet())
    }

    private fun processErrorMessage(t: Throwable): String {
        val message = t.message.orEmpty()
        return when {
            message.contains("校准模型") -> "缺少与这张截图匹配的校准，请先完成对应方向的校准。"
            message.contains("清理 HDR gain map") -> "请在设置中开启“清理 Ultra HDR 增益图”后重试。"
            message.contains("HDR_TO_SDR") -> "暂不支持这种 Ultra HDR 图片。"
            message.contains("gain map", ignoreCase = true) && message.contains("未得到") ->
                "当前无法安全处理这张 Ultra HDR 图片。"
            message.contains("gain map", ignoreCase = true) &&
                (message.contains("尺寸已变化") || message.contains("通道布局已变化")) ->
                "这张 Ultra HDR 图片与当前校准不匹配，请重新完成对应方向的 HDR 校准。"
            message.contains("无法解码") -> "无法读取这张图片。"
            message.contains("无法创建") -> "无法处理这张图片。"
            else -> "处理失败，请重试。"
        }
    }

    private fun calibrationErrorMessage(t: Throwable): String {
        val message = t.message.orEmpty()
        return when {
            message.contains("identical dimensions") -> "6 张截图尺寸不一致，请重新校准。"
            message.contains("No verified OPlus beta watermark pixels found") ->
                "没有检测到可验证的系统水印，请重新截取 6 张灰阶截图。"
            message.contains("Unable to decode") ||
                message.contains("Unsupported calibration image") ||
                message.contains("Unable to open") ->
                "无法读取校准截图，请重新选择。"
            message.startsWith("截图方向与") -> "截图方向不一致，请按当前方向重新校准。"
            else -> "校准失败，请重新截取 6 张截图。"
        }
    }

    private fun updateQueue(uri: Uri, transform: (QueueItem) -> QueueItem) {
        _state.value = _state.value.copy(
            queue = _state.value.queue.map { if (it.uri == uri) transform(it) else it },
        )
    }
}
