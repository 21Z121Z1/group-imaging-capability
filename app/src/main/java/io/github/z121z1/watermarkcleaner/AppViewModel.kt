package io.github.z121z1.watermarkcleaner

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.z121z1.watermarkcleaner.core.CalibrationEngine
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
    val hdr: Boolean = false,
    val samples: List<Uri> = emptyList(),
    val pickerRequested: Boolean = false,
    val fitting: Boolean = false,
    val message: String? = null,
) {
    val levelIndex: Int get() = samples.size.coerceAtMost(CalibrationEngine.LEVELS.lastIndex)
    val complete: Boolean get() = samples.size == CalibrationEngine.LEVELS.size
}

data class UiState(
    val queue: List<QueueItem> = emptyList(),
    val calibration: CalibrationState = CalibrationState(),
    val modelReady: Boolean = false,
    val outputTree: Uri? = null,
    val jpegQuality: Int = 90,
    val cleanHdrGainMap: Boolean = true,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val profiles = ProfileRepository(context)
    private val settings = AppSettings(context)
    private val processor = WatermarkProcessor(context.contentResolver, profiles)
    private val exporter = ImageExporter(context.contentResolver, settings)
    private val calibrationEngine = CalibrationEngine(context.contentResolver)

    private val _state = MutableStateFlow(
        UiState(
            modelReady = profiles.hasPrimary(),
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
                            message = if (result.wasHdr) "Ultra HDR 已保留" else "已保存",
                        )
                    }
                } catch (t: Throwable) {
                    updateQueue(target.uri) {
                        it.copy(status = QueueStatus.ERROR, message = t.message ?: t::class.java.simpleName)
                    }
                }
            }
        }
    }

    fun startCalibration(hdr: Boolean) {
        _state.value = _state.value.copy(
            calibration = CalibrationState(active = true, hdr = hdr),
        )
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
        if (!snapshot.complete || snapshot.fitting) return
        _state.value = _state.value.copy(calibration = snapshot.copy(fitting = true, message = "正在拟合逐像素模型…"))
        viewModelScope.launch {
            try {
                val profile = calibrationEngine.fit(snapshot.samples)
                withContext(Dispatchers.IO) { profiles.savePrimary(profile) }
                _state.value = _state.value.copy(
                    modelReady = true,
                    calibration = CalibrationState(
                        active = false,
                        message = "校准完成：${profile.width}×${profile.height}，${profile.pixels.size} 个稳定水印像素",
                    ),
                )
            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    calibration = snapshot.copy(fitting = false, message = t.message ?: "校准失败"),
                )
            }
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
        _state.value = _state.value.copy(modelReady = false)
    }

    private fun updateQueue(uri: Uri, transform: (QueueItem) -> QueueItem) {
        _state.value = _state.value.copy(
            queue = _state.value.queue.map { if (it.uri == uri) transform(it) else it },
        )
    }
}
