package com.lens.camera.ui

import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.HdrOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.lens.camera.AppUiState
import com.lens.camera.MainViewModel
import com.lens.camera.Mode
import com.lens.camera.R
import com.lens.camera.camera.CameraController
import com.lens.camera.collage.LAYOUTS
import com.lens.camera.filters.FILTERS
import com.lens.camera.frames.FRAMES
import com.lens.camera.ui.theme.Viewfinder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * A frosted "liquid glass" surface: a soft translucent gradient fill plus a bright
 * hairline rim, evoking a glass panel floating over whatever sits behind it. This
 * approximates the look without a real-time backdrop blur (not available here without
 * a third-party blur library), which is enough for small floating controls.
 */
private fun Modifier.glass(shape: RoundedCornerShape): Modifier = this
    .background(Color.Black.copy(alpha = .32f), shape)
    .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = .16f), Color.White.copy(alpha = .02f))), shape)
    .border(1.dp, Color.White.copy(alpha = .30f), shape)

@Composable
fun CameraScreen(viewModel: MainViewModel, cameraPermissionGranted: Boolean) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraController = remember { CameraController(context) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    LaunchedEffect(cameraPermissionGranted) {
        if (cameraPermissionGranted) {
            viewModel.setAvailableCameras(cameraController.availableCameras())
        }
    }

    val selectedCamera = state.selectedCamera
    LaunchedEffect(cameraPermissionGranted, selectedCamera, state.hdrEnabled, previewView) {
        val pv = previewView
        if (!cameraPermissionGranted || selectedCamera == null) {
            viewModel.setCameraReady(ready = false, demo = true)
            return@LaunchedEffect
        }
        if (pv != null) {
            try {
                cameraController.bind(pv, lifecycleOwner, selectedCamera, state.hdrEnabled)
                viewModel.setCameraReady(ready = true, demo = false)
                val iso = cameraController.isoRange()
                viewModel.setCameraCapabilities(
                    zoomRange = cameraController.zoomRange(),
                    exposureRange = cameraController.exposureRange(),
                    isoRange = if (iso != null) iso.lower..iso.upper else 100..100,
                    manualIsoSupported = cameraController.hasManualSensorControl(),
                    hdrSupported = cameraController.isHdrSupported(selectedCamera)
                )
            } catch (e: Exception) {
                viewModel.setCameraReady(ready = false, demo = true)
            }
        }
    }

    LaunchedEffect(state.filterIndex, state.pro, state.mode, previewView) {
        previewView?.setLayerType(
            View.LAYER_TYPE_HARDWARE,
            Paint().apply { colorFilter = ColorMatrixColorFilter(state.activeColorMatrix) }
        )
    }

    LaunchedEffect(state.zoomRatio, state.cameraReady) {
        if (state.cameraReady) cameraController.setZoomRatio(state.zoomRatio)
    }
    LaunchedEffect(state.exposureIndex, state.cameraReady) {
        if (state.cameraReady) cameraController.setExposureIndex(state.exposureIndex)
    }
    LaunchedEffect(state.manualIsoEnabled, state.isoValue, state.cameraReady) {
        if (state.cameraReady) {
            cameraController.setManualIso(if (state.manualIsoEnabled) state.isoValue else null)
        }
    }

    var reticleOffset by remember { mutableStateOf<Offset?>(null) }
    var reticleTick by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Viewfinder)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        previewView?.let { cameraController.focusAt(it, offset.x, offset.y) }
                        reticleOffset = offset
                        reticleTick++
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        val current = viewModel.state.value.zoomRatio
                        viewModel.setZoom(current * zoom)
                    }
                }
        ) {
            if (state.demoMode) {
                DemoScene(modifier = Modifier.fillMaxSize())
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            previewView = this
                        }
                    }
                )
            }

            if (state.gridVisible) CornerFrame(Modifier.fillMaxSize())

            reticleOffset?.let { off -> FocusReticle(offset = off, tick = reticleTick) }

            TopHud(viewModel = viewModel)

            if (state.mode == Mode.COLLAGE) {
                CollageProgressHud(
                    total = LAYOUTS[state.layoutIndex].cells.size,
                    done = state.collageShots.size,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 64.dp)
                )
            }

            HudData(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp))

            if (state.mode == Mode.PRO) {
                ProPanel(
                    viewModel = viewModel,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
                )
            } else if (state.zoomRange.endInclusive > state.zoomRange.start) {
                ZoomIndicator(
                    ratio = state.zoomRatio,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                )
            }

            if (state.cameraPickerOpen) {
                CameraPickerDialog(viewModel, cameraController)
            }

            state.countdownValue?.let { value ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(value.toString(), color = Color.White, fontSize = 110.sp, fontWeight = FontWeight.Bold)
                }
            }

            FlashFx(tick = state.flashFxTick)
        }

        BottomPanel(viewModel = viewModel, cameraController = cameraController)
    }
}

@Composable
private fun TopHud(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HudButton(
                icon = if (state.flashSim) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                active = state.flashSim,
                contentDescription = stringResource(R.string.cd_flash),
                onClick = { viewModel.toggleFlashSim() }
            )
            HudButton(
                icon = Icons.Filled.Timer,
                active = state.timerSeconds > 0,
                contentDescription = stringResource(R.string.cd_timer),
                onClick = { viewModel.cycleTimer() }
            )
        }
        Text("LENS", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 4.sp, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HudButton(
                icon = Icons.Filled.HdrOn,
                active = state.hdrEnabled,
                contentDescription = stringResource(R.string.cd_hdr),
                onClick = { viewModel.toggleHdr() }
            )
            HudButton(
                icon = Icons.Filled.GridOn,
                active = state.gridVisible,
                contentDescription = stringResource(R.string.cd_grid),
                onClick = { viewModel.toggleGrid() }
            )
        }
    }
}

@Composable
private fun HudButton(icon: ImageVector, active: Boolean, contentDescription: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(42.dp)
            .glass(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = if (active) MaterialTheme.colorScheme.primary else Color.White)
    }
}

@Composable
private fun HudData(modifier: Modifier = Modifier) {
    var clock by remember { mutableStateOf(currentTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            clock = currentTime()
            delay(1000)
        }
    }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("● " + stringResource(R.string.hud_live), color = Color.White.copy(alpha = .8f), fontSize = 10.sp)
        Text(stringResource(R.string.hud_iso), color = Color.White.copy(alpha = .8f), fontSize = 10.sp)
        Text(stringResource(R.string.hud_shutter), color = Color.White.copy(alpha = .8f), fontSize = 10.sp)
        Text(clock, color = Color.White.copy(alpha = .8f), fontSize = 10.sp)
    }
}

private fun currentTime(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

@Composable
private fun CornerFrame(modifier: Modifier = Modifier) {
    Canvas(modifier.padding(14.dp)) {
        val len = 26.dp.toPx()
        val stroke = 2.dp.toPx()
        val color = Color.White.copy(alpha = .55f)
        drawLine(color, Offset(0f, 0f), Offset(len, 0f), stroke)
        drawLine(color, Offset(0f, 0f), Offset(0f, len), stroke)
        drawLine(color, Offset(size.width, 0f), Offset(size.width - len, 0f), stroke)
        drawLine(color, Offset(size.width, 0f), Offset(size.width, len), stroke)
        drawLine(color, Offset(0f, size.height), Offset(len, size.height), stroke)
        drawLine(color, Offset(0f, size.height), Offset(0f, size.height - len), stroke)
        drawLine(color, Offset(size.width, size.height), Offset(size.width - len, size.height), stroke)
        drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - len), stroke)
    }
}

@Composable
private fun FocusReticle(offset: Offset, tick: Int) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(1.5f) }
    val density = LocalDensity.current
    val sizePx = with(density) { 78.dp.toPx() }
    LaunchedEffect(tick) {
        alpha.snapTo(0f)
        scale.snapTo(1.5f)
        launch {
            alpha.animateTo(1f, tween(150))
            alpha.animateTo(0f, tween(300, delayMillis = 150))
        }
        scale.animateTo(1f, tween(450))
    }
    Box(
        Modifier
            .size(78.dp)
            .offset {
                IntOffset(
                    (offset.x - sizePx / 2).roundToInt(),
                    (offset.y - sizePx / 2).roundToInt()
                )
            }
            .scale(scale.value)
            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = alpha.value), RoundedCornerShape(4.dp))
    )
}

@Composable
private fun CollageProgressHud(total: Int, done: Int, modifier: Modifier = Modifier) {
    Row(
        modifier
            .background(Color.Black.copy(alpha = .45f), RoundedCornerShape(12.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(total) { i ->
            Box(
                Modifier
                    .size(22.dp)
                    .border(1.5.dp, Color.White.copy(alpha = .5f), RoundedCornerShape(4.dp))
                    .background(
                        if (i < done) MaterialTheme.colorScheme.primary else Color.Transparent,
                        RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

@Composable
private fun FlashFx(tick: Int) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(tick) {
        if (tick == 0) return@LaunchedEffect
        alpha.snapTo(0.9f)
        alpha.animateTo(0f, tween(180))
    }
    if (alpha.value > 0f) {
        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = alpha.value)))
    }
}

@Composable
private fun ZoomIndicator(ratio: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .glass(RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            String.format(Locale.US, "%.1f×", ratio),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CameraPickerDialog(viewModel: MainViewModel, cameraController: CameraController) {
    val state by viewModel.state.collectAsState()
    val frontLabel = stringResource(R.string.camera_front)
    val backLabel = stringResource(R.string.camera_back)
    val genericLabel = stringResource(R.string.camera_generic)

    val specs = remember(state.availableCameras) {
        state.availableCameras.map { cameraController.cameraSpecs(it) }
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = .5f)).clickable { viewModel.closeCameraPicker() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .widthIn(min = 220.dp)
                .glass(RoundedCornerShape(18.dp))
                .padding(vertical = 8.dp)
        ) {
            state.availableCameras.forEachIndexed { i, info ->
                val facing = info.lensFacing
                val sameFacingBefore = state.availableCameras.take(i).count { it.lensFacing == facing }
                val base = when (facing) {
                    CameraSelector.LENS_FACING_FRONT -> frontLabel
                    CameraSelector.LENS_FACING_BACK -> backLabel
                    else -> genericLabel
                }
                val name = if (sameFacingBefore > 0) "$base ${sameFacingBefore + 1}" else base
                val spec = specs.getOrNull(i)
                val details = buildList {
                    spec?.megapixels?.let { add("${it}MP") }
                    spec?.focalLengthMm?.let { add(String.format(Locale.US, "%.1fmm", it)) }
                }.joinToString(" · ")
                val selected = i == state.selectedCameraIndex
                Column(
                    Modifier
                        .clickable { viewModel.selectCamera(i) }
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 16.dp)
                ) {
                    Text(
                        name,
                        color = if (selected) MaterialTheme.colorScheme.primary else Color.White,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                    if (details.isNotEmpty()) {
                        Text(details, color = Color.White.copy(alpha = .6f), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoScene(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "demo")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "t"
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(Color(0xFF0B1220))
        val sunX = w * 0.5f + kotlin.math.sin(t * 2 * Math.PI).toFloat() * w * 0.12f
        drawCircle(Color(0xFFFFE1A0), radius = w * 0.09f, center = Offset(sunX, h * 0.42f))
        drawRect(
            Color(0xFF0D1420),
            topLeft = Offset(0f, h * 0.62f),
            size = androidx.compose.ui.geometry.Size(w, h * 0.38f)
        )
    }
}

@Composable
private fun BottomPanel(viewModel: MainViewModel, cameraController: CameraController) {
    val state by viewModel.state.collectAsState()
    Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.systemBars)) {
        if (state.mode == Mode.COLLAGE) {
            FramesRow(viewModel)
            LayoutsRow(viewModel)
        } else {
            FiltersRow(viewModel)
        }
        ShutterRow(viewModel, cameraController)
        ModeSwitcher(viewModel)
    }
}

@Composable
private fun FiltersRow(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val names = stringArrayResource(R.array.filter_names)
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FILTERS.forEachIndexed { i, _ ->
            val selected = i == state.filterIndex
            Column(
                Modifier.clickable { viewModel.selectFilter(i) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(56.dp)
                        .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(14.dp))
                        .padding(2.dp)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF3D5A80), Color(0xFFEE6C4D), Color(0xFFF4D35E))),
                            RoundedCornerShape(12.dp)
                        )
                )
                Text(
                    names.getOrElse(i) { "" }.uppercase(),
                    fontSize = 9.sp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private enum class ProControl { CAMERA, ZOOM, EV, ISO, BRI, CON, SAT, TEMP }

@Composable
private fun ProPanel(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    var active by remember { mutableStateOf<ProControl?>(null) }
    val frontLabel = stringResource(R.string.camera_front)
    val backLabel = stringResource(R.string.camera_back)
    val genericLabel = stringResource(R.string.camera_generic)

    // Floats over the viewfinder as a self-contained overlay: it never takes part in
    // the screen's layout, so opening a control never shrinks the camera preview.
    Column(modifier.widthIn(max = 420.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        active?.let { control ->
            ProControlPopup(control, viewModel, state)
        }
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .glass(RoundedCornerShape(18.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProChip(
                label = cameraChipLabel(state, frontLabel, backLabel, genericLabel),
                icon = Icons.Filled.Cameraswitch,
                active = false,
                onClick = { viewModel.openCameraPicker() }
            )
            if (state.zoomRange.endInclusive > state.zoomRange.start) {
                ProChip(
                    label = stringResource(R.string.pro_zoom),
                    value = String.format(Locale.US, "%.1f×", state.zoomRatio),
                    active = active == ProControl.ZOOM,
                    onClick = { active = if (active == ProControl.ZOOM) null else ProControl.ZOOM }
                )
            }
            if (state.exposureRange.last > state.exposureRange.first) {
                ProChip(
                    label = stringResource(R.string.pro_ev),
                    value = state.exposureIndex.toString(),
                    active = active == ProControl.EV,
                    onClick = { active = if (active == ProControl.EV) null else ProControl.EV }
                )
            }
            if (state.manualIsoSupported) {
                ProChip(
                    label = stringResource(R.string.pro_iso),
                    value = if (state.manualIsoEnabled) state.isoValue.toString() else "AUTO",
                    active = active == ProControl.ISO,
                    onClick = { active = if (active == ProControl.ISO) null else ProControl.ISO }
                )
            }
            ProChip(
                label = stringResource(R.string.pro_bri),
                value = (state.pro.brightness * 100).roundToInt().toString(),
                active = active == ProControl.BRI,
                onClick = { active = if (active == ProControl.BRI) null else ProControl.BRI }
            )
            ProChip(
                label = stringResource(R.string.pro_con),
                value = (state.pro.contrast * 100).roundToInt().toString(),
                active = active == ProControl.CON,
                onClick = { active = if (active == ProControl.CON) null else ProControl.CON }
            )
            ProChip(
                label = stringResource(R.string.pro_sat),
                value = (state.pro.saturate * 100).roundToInt().toString(),
                active = active == ProControl.SAT,
                onClick = { active = if (active == ProControl.SAT) null else ProControl.SAT }
            )
            ProChip(
                label = stringResource(R.string.pro_temp),
                value = state.pro.hueRotate.roundToInt().toString(),
                active = active == ProControl.TEMP,
                onClick = { active = if (active == ProControl.TEMP) null else ProControl.TEMP }
            )
        }
    }
}

private fun cameraChipLabel(state: AppUiState, front: String, back: String, generic: String): String {
    val facing = state.selectedCamera?.lensFacing
    return when (facing) {
        CameraSelector.LENS_FACING_FRONT -> front
        CameraSelector.LENS_FACING_BACK -> back
        else -> generic
    }.uppercase()
}

@Composable
private fun ProChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    value: String? = null,
    icon: ImageVector? = null
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (active) {
                    Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = .24f), RoundedCornerShape(14.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .65f), RoundedCornerShape(14.dp))
                } else {
                    Modifier.glass(RoundedCornerShape(14.dp))
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        icon?.let {
            Icon(it, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White.copy(alpha = .85f))
        }
        Text(label, fontSize = 9.sp, color = Color.White.copy(alpha = .7f), letterSpacing = 0.5.sp)
        value?.let {
            Text(it, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ProControlPopup(control: ProControl, viewModel: MainViewModel, state: AppUiState) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .glass(RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        when (control) {
            ProControl.ZOOM -> MiniSlider(
                state.zoomRatio, state.zoomRange.start, state.zoomRange.endInclusive,
                display = String.format(Locale.US, "%.1f×", state.zoomRatio)
            ) { viewModel.setZoom(it) }

            ProControl.EV -> MiniSlider(
                state.exposureIndex.toFloat(), state.exposureRange.first.toFloat(), state.exposureRange.last.toFloat(),
                display = state.exposureIndex.toString()
            ) { viewModel.setExposure(it.roundToInt()) }

            ProControl.ISO -> Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.pro_iso),
                        Modifier.weight(1f),
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = .8f)
                    )
                    androidx.compose.material3.Switch(
                        checked = state.manualIsoEnabled,
                        onCheckedChange = { viewModel.toggleManualIso() }
                    )
                }
                if (state.manualIsoEnabled) {
                    MiniSlider(
                        state.isoValue.toFloat(), state.isoRange.first.toFloat(), state.isoRange.last.toFloat(),
                        display = state.isoValue.toString()
                    ) { viewModel.setIso(it.roundToInt()) }
                }
            }

            ProControl.BRI -> MiniSlider(
                state.pro.brightness, 0.5f, 1.6f, display = (state.pro.brightness * 100).roundToInt().toString()
            ) { viewModel.updatePro(state.pro.copy(brightness = it)) }

            ProControl.CON -> MiniSlider(
                state.pro.contrast, 0.5f, 1.8f, display = (state.pro.contrast * 100).roundToInt().toString()
            ) { viewModel.updatePro(state.pro.copy(contrast = it)) }

            ProControl.SAT -> MiniSlider(
                state.pro.saturate, 0f, 2.2f, display = (state.pro.saturate * 100).roundToInt().toString()
            ) { viewModel.updatePro(state.pro.copy(saturate = it)) }

            ProControl.TEMP -> MiniSlider(
                state.pro.hueRotate, -40f, 40f, display = state.pro.hueRotate.roundToInt().toString()
            ) { viewModel.updatePro(state.pro.copy(hueRotate = it)) }

            ProControl.CAMERA -> {}
        }
    }
}

@Composable
private fun MiniSlider(value: Float, min: Float, max: Float, display: String, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = if (max > min) min..max else min..(min + 0.001f),
            modifier = Modifier.weight(1f).height(28.dp)
        )
        Text(display, Modifier.width(40.dp), fontSize = 10.sp, color = Color.White, textAlign = TextAlign.End)
    }
}

@Composable
private fun FramesRow(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val names = stringArrayResource(R.array.frame_names)
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        FRAMES.forEachIndexed { i, frame ->
            val selected = i == state.frameIndex
            Column(
                Modifier.clickable { viewModel.selectFrame(i) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(50.dp)
                        .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(12.dp))
                        .background(Color(frame.bg), RoundedCornerShape(10.dp))
                )
                Text(
                    names.getOrElse(i) { "" }.uppercase(),
                    fontSize = 9.sp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LayoutsRow(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LAYOUTS.forEachIndexed { i, layout ->
                val selected = i == state.layoutIndex
                Box(
                    Modifier
                        .size(52.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(12.dp))
                        .clickable { viewModel.selectLayout(i) }
                ) {
                    layout.cells.forEach { cell ->
                        Box(
                            Modifier
                                .align(Alignment.TopStart)
                                .padding(start = (cell.x * 52).dp, top = (cell.y * 52).dp)
                                .size((cell.w * 52 - 3).dp, (cell.h * 52 - 3).dp)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary else Color(0xFF3A3A44),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ShutterRow(viewModel: MainViewModel, cameraController: CameraController) {
    val state by viewModel.state.collectAsState()
    val thumb = remember(state.captures.firstOrNull()?.id) {
        state.captures.firstOrNull()?.let { viewModel.loadCaptureBitmap(it) }
    }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 34.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(50.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                .border(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .3f), RoundedCornerShape(14.dp))
                .clickable { viewModel.openGallery() },
            contentAlignment = Alignment.Center
        ) {
            thumb?.let {
                Image(
                    it.asImageBitmap(),
                    contentDescription = stringResource(R.string.cd_gallery),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Box(
            Modifier
                .size(76.dp)
                .border(4.dp, Color.White, CircleShape)
                .clickable { viewModel.onShutter(cameraController) },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(58.dp)
                    .background(if (state.mode == Mode.COLLAGE) MaterialTheme.colorScheme.primary else Color.White, CircleShape)
            )
        }

        Box(
            Modifier
                .size(50.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                .combinedClickable(
                    onClick = { viewModel.toggleFacing() },
                    onLongClick = { if (state.availableCameras.size > 2) viewModel.openCameraPicker() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Cameraswitch, contentDescription = stringResource(R.string.cd_flip_camera))
        }
    }
}

@Composable
private fun ModeSwitcher(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp), horizontalArrangement = Arrangement.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
            ModeTab(stringResource(R.string.mode_photo), state.mode == Mode.PHOTO) { viewModel.setMode(Mode.PHOTO) }
            ModeTab(stringResource(R.string.mode_collage), state.mode == Mode.COLLAGE) { viewModel.setMode(Mode.COLLAGE) }
            ModeTab(stringResource(R.string.mode_pro), state.mode == Mode.PRO) { viewModel.setMode(Mode.PRO) }
        }
    }
}

@Composable
private fun ModeTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        modifier = Modifier.clickable(onClick = onClick).padding(bottom = 4.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.Medium
    )
}
