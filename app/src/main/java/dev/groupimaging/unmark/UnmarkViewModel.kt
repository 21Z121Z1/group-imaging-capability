package dev.groupimaging.unmark

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.groupimaging.unmark.calibration.CalibrationEngine
import dev.groupimaging.unmark.data.AppSettings
import dev.groupimaging.unmark.image.ImageProcessor
import dev.groupimaging.unmark.image.OutputWriter
import dev.groupimaging.unmark.media.MediaAccess
import dev.groupimaging.unmark.media.ScreenshotFinder
import dev.groupimaging.unmark.model.AffineMath
import dev.groupimaging.unmark.model.ProfileRepository
import dev.groupimaging.unmark.model.WatermarkProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UnmarkViewModel(application: Application) : AndroidViewModel(application) {
    enum class JobStatus { Queued, Processing, Done, Failed }

    data class ImageJob(
        val uri: Uri,
        val status: JobStatus = JobStatus.Queued,
        val outputUri: Uri? = null,
        val wasUltraHdr: Boolean = false,
        val ultraHdrVerified: Boolean? = null,
        val error: String? = null,
    )

    sealed interface CalibrationState {
        data object Idle : CalibrationState
        data class Capturing(val index: Int, val level: Int) : CalibrationState
        data object Fitting : CalibrationState
        data class Complete(val pixels: Int, val width: Int, val height: Int) : CalibrationState
        data class Error(val message: String) : CalibrationState
    }

    sealed interface Effect {
        data object PickProcessingImages : Effect
        data object PickCalibrationImage : Effect
        data object PickOutputDirectory : Effect
        data object RequestFullMediaAccess : Effect
    }

    data class UiState(
        val jobs: List<ImageJob> = emptyList(),
        val calibration: CalibrationState = CalibrationState.Idle,
        val profile: WatermarkProfile? = null,
        val mediaAccess: MediaAccess = MediaAccess.PickerOnly,
        val jpegQuality: Int = 90,
        val outputTree: Uri? = null,
        val busy: Boolean = false,
        val message: String? = null,
    )

    private val app = application.applicationContext
    private val settings = AppSettings(app)
    private val profiles = ProfileRepository(app)
    private val processor = ImageProcessor(app.contentResolver)
    private val writer = OutputWriter(app, settings)
    private val calibrationEngine = CalibrationEngine(app.contentResolver)
    private val screenshotFinder = ScreenshotFinder(app)

    private val _uiState = MutableStateFlow(
        UiState(
            profile = profiles.load(),
            mediaAccess = MediaAccess.current(app),
            jpegQuality = settings.jpegQuality,
            outputTree = settings.outputTreeUri,
        ),
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<Effect>(extraBufferCapacity = 8)
    val effects: SharedFlow<Effect> = _effects.asSharedFlow()

    private val calibrationUris = mutableListOf<Uri>()
    private var captureStartedAt = 0L
    private var importInFlight = false

    fun requestImagePicker() {
        _effects.tryEmit(Effect.PickProcessingImages)
    }

    fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _uiState.update { state ->
            val known = state.jobs.mapTo(mutableSetOf()) { it.uri }
            state.copy(jobs = state.jobs + uris.filter { known.add(it) }.map(::ImageJob))
        }
    }

    fun removeJob(uri: Uri) {
        if (_uiState.value.busy) return
        _uiState.update { it.copy(jobs = it.jobs.filterNot { job -> job.uri == uri }) }
    }

    fun processQueued() {
        val profile = _uiState.value.profile
        if (profile == null) {
            _uiState.update { it.copy(message = "请先完成水印校准") }
            return
        }
        if (_uiState.value.busy) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(busy = true, message = null) }
            try {
                val uris = _uiState.value.jobs.filter { it.status == JobStatus.Queued || it.status == JobStatus.Failed }.map { it.uri }
                for (uri in uris) {
                    mutateJob(uri) { it.copy(status = JobStatus.Processing, error = null) }
                    try {
                        val processed = processor.decodeAndRemove(uri, profile)
                        try {
                            val output = writer.write(processed.bitmap, processed.wasUltraHdr)
                            mutateJob(uri) {
                                it.copy(
                                    status = JobStatus.Done,
                                    outputUri = output.uri,
                                    wasUltraHdr = processed.wasUltraHdr,
                                    ultraHdrVerified = output.ultraHdrVerified,
                                )
                            }
                        } finally {
                            processed.bitmap.recycle()
                        }
                    } catch (t: Throwable) {
                        mutateJob(uri) {
                            it.copy(status = JobStatus.Failed, error = t.message ?: t::class.java.simpleName)
                        }
                    }
                }
            } finally {
                _uiState.update { it.copy(busy = false) }
            }
        }
    }

    fun requestOutputDirectory() {
        _effects.tryEmit(Effect.PickOutputDirectory)
    }

    fun setOutputTree(uri: Uri?) {
        settings.outputTreeUri = uri
        _uiState.update { it.copy(outputTree = uri) }
    }

    fun setJpegQuality(quality: Int) {
        settings.jpegQuality = quality
        _uiState.update { it.copy(jpegQuality = settings.jpegQuality) }
    }

    fun requestFullMediaAccess() {
        _effects.tryEmit(Effect.RequestFullMediaAccess)
    }

    fun refreshMediaAccess() {
        _uiState.update { it.copy(mediaAccess = MediaAccess.current(app)) }
    }

    fun startCalibration(canUseFullScreen: Boolean) {
        if (!canUseFullScreen) {
            _uiState.update {
                it.copy(calibration = CalibrationState.Error("校准必须在全屏窗口中进行；请先退出分屏/系统小窗"))
            }
            return
        }
        calibrationUris.clear()
        beginCapture(0)
    }

    fun cancelCalibration() {
        calibrationUris.clear()
        importInFlight = false
        _uiState.update { it.copy(calibration = CalibrationState.Idle) }
    }

    fun onSystemScreenshotDetected() {
        val state = _uiState.value.calibration as? CalibrationState.Capturing ?: return
        if (importInFlight) return
        importInFlight = true

        if (MediaAccess.current(app) != MediaAccess.Full) {
            importInFlight = false
            _effects.tryEmit(Effect.PickCalibrationImage)
            return
        }

        val expectedStartedAt = captureStartedAt
        viewModelScope.launch(Dispatchers.IO) {
            var uri: Uri? = null
            repeat(8) {
                if (uri == null) {
                    delay(250L)
                    uri = runCatching { screenshotFinder.findRecent(expectedStartedAt) }.getOrNull()
                }
            }
            importInFlight = false
            if (uri != null && _uiState.value.calibration == state) {
                acceptCalibrationUri(uri!!)
            } else if (_uiState.value.calibration == state) {
                _effects.tryEmit(Effect.PickCalibrationImage)
            }
        }
    }

    fun onCalibrationImageSelected(uri: Uri?) {
        importInFlight = false
        if (uri == null) return
        if (_uiState.value.calibration is CalibrationState.Capturing) acceptCalibrationUri(uri)
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun acceptCalibrationUri(uri: Uri) {
        calibrationUris += uri
        if (calibrationUris.size < AffineMath.calibrationLevels.size) {
            beginCapture(calibrationUris.size)
        } else {
            _uiState.update { it.copy(calibration = CalibrationState.Fitting) }
            val inputs = calibrationUris.toList()
            viewModelScope.launch(Dispatchers.Default) {
                runCatching {
                    val profile = calibrationEngine.fit(inputs)
                    require(profile.size > 0) { "未检测到稳定水印，请确认六张截图顺序与显示比例一致" }
                    profiles.save(profile)
                    profile
                }.onSuccess { profile ->
                    _uiState.update {
                        it.copy(
                            profile = profile,
                            calibration = CalibrationState.Complete(profile.size, profile.width, profile.height),
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(calibration = CalibrationState.Error(error.message ?: "校准失败"))
                    }
                }
            }
        }
    }

    private fun beginCapture(index: Int) {
        captureStartedAt = System.currentTimeMillis()
        importInFlight = false
        _uiState.update {
            it.copy(
                calibration = CalibrationState.Capturing(index, AffineMath.calibrationLevels[index]),
                message = null,
            )
        }
    }

    private fun mutateJob(uri: Uri, transform: (ImageJob) -> ImageJob) {
        _uiState.update { state ->
            state.copy(jobs = state.jobs.map { if (it.uri == uri) transform(it) else it })
        }
    }
}
