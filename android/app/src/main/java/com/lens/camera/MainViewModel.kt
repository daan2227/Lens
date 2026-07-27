package com.lens.camera

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
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
    val flashSim: Boolean = false,
    val gridVisible: Boolean = true,
    val lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    val layoutIndex: Int = 3,
    val frameIndex: Int = 0,
    val collageShots: List<Bitmap> = emptyList(),
    val captures: List<Capture> = emptyList(),
    val countdownValue: Int? = null,
    val flashFxTick: Int = 0,
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

    fun toggleFlashSim() = _state.update {
        val next = !it.flashSim
        it.copy(flashSim = next, toast = str(if (next) R.string.toast_flash_on else R.string.toast_flash_off))
    }

    fun cycleTimer() = _state.update {
        val next = when (it.timerSeconds) { 0 -> 3; 3 -> 10; else -> 0 }
        val toast = if (next > 0) str(R.string.toast_timer_set, next) else str(R.string.toast_timer_off)
        it.copy(timerSeconds = next, toast = toast)
    }

    fun toggleGrid() = _state.update { it.copy(gridVisible = !it.gridVisible) }

    fun toggleFacing() = _state.update {
        val next = if (it.lensFacing == CameraSelector.LENS_FACING_FRONT)
            CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
        it.copy(lensFacing = next)
    }

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
        if (_state.value.flashSim) {
            _state.update { it.copy(flashFxTick = it.flashFxTick + 1) }
        }

        val matrix = _state.value.activeColorMatrix
        val mirror = _state.value.lensFacing == CameraSelector.LENS_FACING_FRONT
        val bitmap = if (cameraController != null && _state.value.cameraReady && !_state.value.demoMode) {
            try {
                cameraController.capturePhoto(matrix, mirror)
            } catch (e: Exception) {
                null
            }
        } else {
            null
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

    fun loadCaptureBitmap(capture: Capture): Bitmap? = captureStore.loadBitmap(capture)

    fun exportToGallery(capture: Capture): Uri? {
        val uri = captureStore.exportToGallery(capture)
        if (uri != null) _state.update { it.copy(toast = str(R.string.toast_saved_to_gallery)) }
        return uri
    }

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
