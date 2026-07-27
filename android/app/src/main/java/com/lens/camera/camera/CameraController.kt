package com.lens.camera.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.util.Range
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Rough hardware specs for a camera, used to tell same-facing lenses apart in the picker. */
data class CameraSpecs(val megapixels: Int?, val focalLengthMm: Float?)

/** Thin CameraX wrapper: binds preview + still capture and post-processes captures with a filter. */
class CameraController(private val context: Context) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var extensionsManager: ExtensionsManager? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)

    var hasFlashUnit: Boolean = false
        private set

    /** All cameras the device reports (front, back, and any extra lenses CameraX exposes separately). */
    suspend fun availableCameras(): List<CameraInfo> {
        val provider = cameraProvider ?: getCameraProvider(context).also { cameraProvider = it }
        return provider.availableCameraInfos
    }

    @OptIn(ExperimentalCamera2Interop::class)
    fun cameraSpecs(info: CameraInfo): CameraSpecs = try {
        val char = Camera2CameraInfo.from(info)
        val pixelArray = char.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
        val megapixels = pixelArray?.let { ((it.width.toLong() * it.height.toLong()) / 1_000_000L).toInt() }
        val focalLength = char.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            ?.firstOrNull()
        CameraSpecs(megapixels, focalLength)
    } catch (e: Exception) {
        CameraSpecs(null, null)
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun cameraId(info: CameraInfo): String? = try {
        Camera2CameraInfo.from(info).cameraId
    } catch (e: Exception) {
        null
    }

    private fun selectorFor(cameraInfo: CameraInfo): CameraSelector {
        val targetId = cameraId(cameraInfo)
        return CameraSelector.Builder()
            .addCameraFilter { infos ->
                if (targetId != null) infos.filter { cameraId(it) == targetId } else infos.filter { it == cameraInfo }
            }
            .build()
    }

    suspend fun isHdrSupported(cameraInfo: CameraInfo): Boolean = try {
        val provider = cameraProvider ?: return false
        val selector = selectorFor(cameraInfo)
        val mgr = getExtensionsManager(provider)
        mgr.isExtensionAvailable(selector, ExtensionMode.HDR)
    } catch (e: Exception) {
        false
    }

    /** Binds preview + still capture to the exact camera identified by [cameraInfo]. */
    suspend fun bind(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        cameraInfo: CameraInfo,
        hdrEnabled: Boolean
    ) {
        val provider = cameraProvider ?: getCameraProvider(context).also { cameraProvider = it }
        provider.unbindAll()

        val baseSelector = selectorFor(cameraInfo)

        val selector = if (hdrEnabled) {
            try {
                val mgr = getExtensionsManager(provider)
                if (mgr.isExtensionAvailable(baseSelector, ExtensionMode.HDR)) {
                    mgr.getExtensionEnabledCameraSelector(baseSelector, ExtensionMode.HDR)
                } else baseSelector
            } catch (e: Exception) {
                baseSelector
            }
        } else baseSelector

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }
        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                    .build()
            )
            .build()

        val boundCamera = provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
        imageCapture = capture
        camera = boundCamera
        hasFlashUnit = boundCamera.cameraInfo.hasFlashUnit()
    }

    /** Real hardware flash for the rear LED; a no-op (safely ignored) on cameras without one. */
    fun setFlashMode(enabled: Boolean) {
        imageCapture?.flashMode = if (enabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
    }

    fun focusAt(previewView: PreviewView, x: Float, y: Float) {
        val cam = camera ?: return
        val point = previewView.meteringPointFactory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()
        cam.cameraControl.startFocusAndMetering(action)
    }

    // ---- Zoom ----

    fun zoomRange(): ClosedFloatingPointRange<Float> {
        val state = camera?.cameraInfo?.zoomState?.value
        val min = state?.minZoomRatio ?: 1f
        val max = state?.maxZoomRatio ?: 1f
        return if (max > min) min..max else 1f..1f
    }

    fun setZoomRatio(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(ratio)
    }

    // ---- Exposure compensation (EV) ----

    fun exposureRange(): IntRange {
        val state = camera?.cameraInfo?.exposureState ?: return 0..0
        if (!state.isExposureCompensationSupported) return 0..0
        val r = state.exposureCompensationRange
        return r.lower..r.upper
    }

    fun setExposureIndex(index: Int) {
        camera?.cameraControl?.setExposureCompensationIndex(index)
    }

    // ---- Manual ISO (best-effort; not every device exposes full manual sensor control) ----

    @OptIn(ExperimentalCamera2Interop::class)
    fun hasManualSensorControl(): Boolean {
        val info = camera?.cameraInfo ?: return false
        return try {
            val modes = Camera2CameraInfo.from(info)
                .getCameraCharacteristic(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)
            modes?.contains(CameraMetadata.CONTROL_AE_MODE_OFF) == true
        } catch (e: Exception) {
            false
        }
    }

    @OptIn(ExperimentalCamera2Interop::class)
    fun isoRange(): Range<Int>? {
        val info = camera?.cameraInfo ?: return null
        return try {
            Camera2CameraInfo.from(info).getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        } catch (e: Exception) {
            null
        }
    }

    @OptIn(ExperimentalCamera2Interop::class)
    fun setManualIso(iso: Int?) {
        val control = camera?.cameraControl ?: return
        val cam2Control = Camera2CameraControl.from(control)
        val options = if (iso != null) {
            CaptureRequestOptions.Builder()
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF)
                .setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, iso)
                .setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, 1_000_000_000L / 60)
                .build()
        } else {
            CaptureRequestOptions.Builder()
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON)
                .build()
        }
        cam2Control.setCaptureRequestOptions(options)
    }

    // ---- Capture ----

    suspend fun capturePhoto(colorMatrix: ColorMatrix, mirror: Boolean): Bitmap {
        val capture = imageCapture ?: error("Camera not bound")
        val proxy = suspendCancellableCoroutine<ImageProxy> { cont ->
            capture.takePicture(mainExecutor, object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    cont.resume(image)
                }

                override fun onError(exception: ImageCaptureException) {
                    cont.resumeWithException(exception)
                }
            })
        }
        return proxy.use { decodeAndProcess(it, colorMatrix, mirror) }
    }

    private fun decodeAndProcess(proxy: ImageProxy, colorMatrix: ColorMatrix, mirror: Boolean): Bitmap {
        val buffer = proxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        val matrix = Matrix()
        val rotation = proxy.imageInfo.rotationDegrees
        if (rotation != 0) matrix.postRotate(rotation.toFloat())
        if (mirror) matrix.postScale(-1f, 1f)

        val oriented = if (matrix.isIdentity) {
            rawBitmap
        } else {
            Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
        }

        val filtered = Bitmap.createBitmap(oriented.width, oriented.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(filtered)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(oriented, 0f, 0f, paint)
        return filtered
    }

    fun unbind() {
        cameraProvider?.unbindAll()
    }

    private suspend fun getExtensionsManager(provider: ProcessCameraProvider): ExtensionsManager =
        extensionsManager ?: suspendCancellableCoroutine { cont ->
            val future = ExtensionsManager.getInstanceAsync(context, provider)
            future.addListener({ cont.resume(future.get()) }, mainExecutor)
        }.also { extensionsManager = it }
}

private suspend fun getCameraProvider(context: Context): ProcessCameraProvider =
    suspendCancellableCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            { cont.resume(future.get()) },
            ContextCompat.getMainExecutor(context)
        )
    }
