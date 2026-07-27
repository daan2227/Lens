package com.lens.camera

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lens.camera.camera.CameraController
import com.lens.camera.collage.LAYOUTS
import com.lens.camera.collage.buildCollage
import com.lens.camera.filters.FILTERS
import com.lens.camera.filters.ProAdjust
import com.lens.camera.filters.buildColorMatrix
import com.lens.camera.frames.FRAMES
import com.lens.camera.gallery.Capture
import com.lens.camera.gallery.CaptureStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Mode { PHOTO, COLLAGE, PRO }

data class AppUiState(
    val mode: Mode = Mode.PHOTO,
    val filterIndex: Int = 0,
    val pro: ProAdjust = ProAdjust(),
    val timerSeconds: Int = 0,
    val flashEnabled: Boolean = false,
    val gridVisible: Boolean = true,

    // Camera hardware
    val availableCameras: List<CameraInfo> = emptyList(),
    val selectedCameraIndex: Int = 0,
    val cameraPickerOpen: Boolean = false,
    val zoomRatio: Float = 1f,
    val zoomRange: ClosedFloatingPointRange<Float> = 1f..1f,
    val exposureIndex: Int = 0,
    val exposureRange: IntRange = 0..0,
    val hdrEnabled: Boolean = false,
    val hdrSupported: Boolean = false,
    val manualIsoEnabled: Boolean = false,
    val isoValue: Int = 100,
    val isoRange: IntRange = 100..100,
    val manualIsoSupported: Boolean = false,

    val layoutIndex: Int = 3,
    val frameIndex: Int = 0,
    val collageShots: List<Bitmap> = emptyList(),
    val captures: List<Capture> = emptyList(),
    val countdownValue: Int? = null,
    val flashFxTick: Int = 0,
    val flashHold: Boolean = false,
    val cameraReady: Boolean = false,
    val demoMode: Boolean = false,
    val editorOpen: Boolean = false,
    val editorSelected: Int = -1,
    val replaceIndex: Int = -1,
    val galleryOpen: Boolean = false,
    val viewerIndex: Int = -1,
    val toast: String? = null
) {
    val activeColorMatrix
        get() = buildColorMatrix(FILTERS[filterIndex] + if (mode == Mode.PRO || !pro.isIdentity) pro.toOps() else emptyList())

    val selectedCamera: CameraInfo?
        get() = availableCameras.getOrNull(selectedCameraIndex)

    val selectedCameraIsFront: Boolean
        get() = selectedCamera?.lensFacing == CameraSelector.LENS_FACING_FRONT
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val captureStore = CaptureStore(application)
    private fun ctx() = getApplication<Application>()
    private fun str(resId: Int, vararg args: Any): String = ctx().getString(resId, *args)
    private fun filterName(index: Int) = ctx().resources.getStringArray(R.array.filter_names).getOrElse(index) { "" }
    private fun frameName(index: Int) = ctx().resources.getStringArray(R.array.frame_names).getOrElse(index) { "" }
    private fun frameBanner(index: Int) = ctx().resources.getStringArray(R.array.frame_banners).getOrElse(index) { "" }

    private val _state = MutableStateFlow(AppUiState(captures = captureStore.listAll()))
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    fun setMode(mode: Mode) {
        _state.update {
            it.copy(mode = mode, collageShots = emptyList(), replaceIndex = -1, editorOpen = false)
        }
    }

    fun selectFilter(index: Int) = _state.update {
        it.copy(filterIndex = index, toast = filterName(index))
    }

    fun updatePro(pro: ProAdjust) = _state.update { it.copy(pro = pro) }

    fun toggleFlash() = _state.update {
        val next = !it.flashEnabled
        it.copy(flashEnabled = next, toast = str(if (next) R.string.toast_flash_on else R.string.toast_flash_off))
    }

    fun cycleTimer() = _state.update {
        val next = when (it.timerSeconds) { 0 -> 3; 3 -> 10; else -> 0 }
        val toast = if (next > 0) str(R.string.toast_timer_set, next) else str(R.string.toast_timer_off)
        it.copy(timerSeconds = next, toast = toast)
    }

    fun toggleGrid() = _state.update { it.copy(gridVisible = !it.gridVisible) }

    // ---- Camera hardware selection ----

    /** Called once camera enumeration completes; keeps the current selection if already valid. */
    fun setAvailableCameras(cameras: List<CameraInfo>) = _state.update {
        if (it.availableCameras.isNotEmpty()) return@update it
        val defaultIndex = cameras.indexOfFirst { c -> c.lensFacing == CameraSelector.LENS_FACING_FRONT }
            .let { i -> if (i >= 0) i else 0 }
        it.copy(availableCameras = cameras, selectedCameraIndex = defaultIndex)
    }

    fun openCameraPicker() = _state.update { it.copy(cameraPickerOpen = true) }
    fun closeCameraPicker() = _state.update { it.copy(cameraPickerOpen = false) }

    fun selectCamera(index: Int) = _state.update {
        it.copy(
            selectedCameraIndex = index,
            cameraPickerOpen = false,
            zoomRatio = 1f,
            exposureIndex = 0,
            manualIsoEnabled = false
        )
    }

    fun toggleFacing() = _state.update {
        if (it.availableCameras.size < 2) return@update it
        val currentFacing = it.selectedCamera?.lensFacing
        val targetFacing = if (currentFacing == CameraSelector.LENS_FACING_FRONT)
            CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
        val next = it.availableCameras.indexOfFirst { c -> c.lensFacing == targetFacing }
        if (next < 0) return@update it
        it.copy(selectedCameraIndex = next, zoomRatio = 1f, exposureIndex = 0, manualIsoEnabled = false)
    }

    fun setCameraCapabilities(
        zoomRange: ClosedFloatingPointRange<Float>,
        exposureRange: IntRange,
        isoRange: IntRange,
        manualIsoSupported: Boolean,
        hdrSupported: Boolean
    ) = _state.update {
        it.copy(
            zoomRange = zoomRange,
            exposureRange = exposureRange,
            isoRange = isoRange,
            isoValue = isoRange.first + (isoRange.last - isoRange.first) / 2,
            manualIsoSupported = manualIsoSupported,
            hdrSupported = hdrSupported
        )
    }

    fun setZoom(ratio: Float) = _state.update { it.copy(zoomRatio = ratio.coerceIn(it.zoomRange)) }

    fun setExposure(index: Int) = _state.update {
        it.copy(exposureIndex = index.coerceIn(it.exposureRange))
    }

    // HDR always toggles: when the device's camera exposes a native HDR extension
    // (hdrSupported) the capture pipeline itself returns a fused frame; otherwise
    // onShutter() falls back to a manual bracketed-exposure fusion so the toggle
    // still does something real instead of just reporting "unsupported".
    fun toggleHdr() = _state.update {
        val next = !it.hdrEnabled
        it.copy(hdrEnabled = next, toast = str(if (next) R.string.toast_hdr_on else R.string.toast_hdr_off))
    }

    fun toggleManualIso() = _state.update {
        if (!it.manualIsoSupported) it.copy(toast = str(R.string.toast_iso_unsupported))
        else it.copy(manualIsoEnabled = !it.manualIsoEnabled)
    }

    fun setIso(value: Int) = _state.update { it.copy(isoValue = value.coerceIn(it.isoRange)) }

    fun selectLayout(index: Int) = _state.update {
        it.copy(layoutIndex = index, collageShots = emptyList())
    }

    fun selectFrame(index: Int) = _state.update {
        it.copy(frameIndex = index, toast = str(R.string.toast_frame_selected, frameName(index)))
    }

    fun setCameraReady(ready: Boolean, demo: Boolean) = _state.update {
        val justWentDemo = demo && !it.demoMode
        it.copy(
            cameraReady = ready,
            demoMode = demo,
            toast = if (justWentDemo) str(R.string.permission_camera_denied) else it.toast
        )
    }

    fun dismissToast() = _state.update { it.copy(toast = null) }

    /** Runs the timer countdown (if any), captures a photo, and routes it to the
     * collage builder or the gallery depending on the current mode. */
    fun onShutter(cameraController: CameraController?) = viewModelScope.launch {
        val s = _state.value
        if (s.timerSeconds > 0) {
            for (i in s.timerSeconds downTo 1) {
                _state.update { it.copy(countdownValue = i) }
                delay(1000)
            }
            _state.update { it.copy(countdownValue = null) }
        }
        // Cameras with a physical flash unit get the LED forced on via torch (a direct
        // hardware control, unlike ImageCapture's flash mode which some devices silently
        // fail to trigger through the AE-precapture handshake). Cameras without one
        // (front-facing) get no real flash hardware at all, so the screen itself is held
        // bright through the capture to actually help.
        val hasHardwareFlash = cameraController?.hasFlashUnit == true
        val usingTorch = _state.value.flashEnabled && hasHardwareFlash
        if (_state.value.flashEnabled) {
            if (usingTorch) {
                cameraController?.setTorch(true)
                _state.update { it.copy(flashFxTick = it.flashFxTick + 1) }
                delay(350)
            } else {
                _state.update { it.copy(flashHold = true) }
                delay(450)
            }
        }

        val matrix = _state.value.activeColorMatrix
        val mirror = _state.value.selectedCameraIsFront
        val useManualHdr = _state.value.hdrEnabled && !_state.value.hdrSupported
        val bitmap = if (cameraController != null && _state.value.cameraReady && !_state.value.demoMode) {
            try {
                if (useManualHdr) {
                    cameraController.captureHdrPhoto(matrix, mirror)
                } else {
                    cameraController.capturePhoto(matrix, mirror)
                }
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
        if (usingTorch) {
            cameraController?.setTorch(false)
        }
        if (_state.value.flashHold) {
            _state.update { it.copy(flashHold = false) }
        }
        if (bitmap == null) {
            _state.update { it.copy(toast = str(R.string.toast_capture_failed)) }
            return@launch
        }

        val current = _state.value
        if (current.mode == Mode.COLLAGE) {
            if (current.replaceIndex >= 0) {
                val updated = current.collageShots.toMutableList()
                updated[current.replaceIndex] = bitmap
                _state.update {
                    it.copy(
                        collageShots = updated,
                        replaceIndex = -1,
                        editorOpen = true,
                        toast = str(R.string.toast_photo_replaced)
                    )
                }
                return@launch
            }
            val updated = current.collageShots + bitmap
            val total = LAYOUTS[current.layoutIndex].cells.size
            _state.update {
                it.copy(
                    collageShots = updated,
                    editorOpen = updated.size >= total,
                    toast = if (updated.size < total) str(R.string.toast_collage_progress, updated.size, total) else null
                )
            }
        } else {
            val capture = captureStore.save(bitmap)
            _state.update {
                it.copy(captures = listOf(capture) + it.captures, toast = str(R.string.toast_capture))
            }
        }
    }

    fun buildCollagePreview(): Bitmap {
        val s = _state.value
        val banner = frameBanner(s.frameIndex).takeIf { it.isNotEmpty() }
        return buildCollage(ctx(), s.collageShots, LAYOUTS[s.layoutIndex], FRAMES[s.frameIndex], banner)
    }

    fun openEditorCell(index: Int) = _state.update {
        val sel = it.editorSelected
        if (sel == -1 || sel == index) {
            it.copy(editorSelected = if (sel == index) -1 else index)
        } else {
            val swapped = it.collageShots.toMutableList()
            val tmp = swapped[sel]
            swapped[sel] = swapped[index]
            swapped[index] = tmp
            it.copy(collageShots = swapped, editorSelected = -1, toast = str(R.string.toast_swapped))
        }
    }

    fun requestReplace(): Boolean {
        val sel = _state.value.editorSelected
        if (sel < 0) {
            _state.update { it.copy(toast = str(R.string.toast_pick_replace_first)) }
            return false
        }
        _state.update {
            it.copy(
                replaceIndex = sel,
                editorOpen = false,
                editorSelected = -1,
                toast = str(R.string.toast_take_new_photo)
            )
        }
        return true
    }

    fun saveCollage() {
        val bitmap = buildCollagePreview()
        val capture = captureStore.save(bitmap)
        _state.update {
            it.copy(
                captures = listOf(capture) + it.captures,
                collageShots = emptyList(),
                editorOpen = false,
                toast = str(R.string.toast_collage_saved)
            )
        }
    }

    fun cancelCollage() = _state.update {
        it.copy(collageShots = emptyList(), editorOpen = false, editorSelected = -1)
    }

    fun openGallery() = _state.update { it.copy(galleryOpen = true) }
    fun closeGallery() = _state.update { it.copy(galleryOpen = false) }
    fun openViewer(index: Int) = _state.update { it.copy(viewerIndex = index) }
    fun closeViewer() = _state.update { it.copy(viewerIndex = -1) }

    fun loadCaptureBitmap(capture: Capture, maxDimension: Int = Int.MAX_VALUE): Bitmap? =
        captureStore.loadBitmap(capture, maxDimension)

    fun exportToGallery(capture: Capture): Uri? {
        val uri = captureStore.exportToGallery(capture)
        if (uri != null) _state.update { it.copy(toast = str(R.string.toast_saved_to_gallery)) }
        return uri
    }

    fun shareUri(capture: Capture): Uri = captureStore.shareUri(capture)

    fun deleteCapture(capture: Capture) {
        captureStore.delete(capture)
        _state.update {
            it.copy(
                captures = it.captures.filterNot { c -> c.id == capture.id },
                viewerIndex = -1,
                toast = str(R.string.toast_deleted)
            )
        }
    }
}
