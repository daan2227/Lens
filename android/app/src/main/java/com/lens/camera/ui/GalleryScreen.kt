package com.lens.camera.ui

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lens.camera.MainViewModel
import com.lens.camera.R
import com.lens.camera.gallery.Capture

@Composable
fun GalleryScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                stringResource(R.string.gallery_title),
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                fontSize = 14.sp
            )
            OutlinedButton(onClick = { viewModel.closeGallery() }) {
                Text(stringResource(R.string.gallery_close))
            }
        }

        if (state.captures.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.gallery_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items(state.captures, key = { it.id }) { capture ->
                    val bitmap = remember(capture.id) { viewModel.loadCaptureBitmap(capture) }
                    bitmap?.let {
                        Image(
                            it.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .aspectRatio(3f / 4f)
                                .clickable { viewModel.openViewer(state.captures.indexOf(capture)) },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }

    if (state.viewerIndex >= 0 && state.viewerIndex < state.captures.size) {
        ViewerDialog(viewModel = viewModel, captures = state.captures, initialIndex = state.viewerIndex)
    }
}

@Composable
private fun ViewerDialog(viewModel: MainViewModel, captures: List<Capture>, initialIndex: Int) {
    val context = LocalContext.current
    val shareLabel = stringResource(R.string.viewer_share)
    val pagerState = rememberPagerState(initialPage = initialIndex) { captures.size }
    var zoomedIn by remember { mutableStateOf(false) }

    // Keep the ViewModel's viewer index in sync as the user swipes between photos.
    LaunchedEffect(pagerState.currentPage) {
        viewModel.openViewer(pagerState.currentPage)
    }

    val capture = captures.getOrNull(pagerState.currentPage) ?: return

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.96f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !zoomedIn,
                key = { captures.getOrNull(it)?.id ?: it },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
            ) { page ->
                val pageCapture = captures.getOrNull(page)
                val bitmap = remember(pageCapture?.id) { pageCapture?.let(viewModel::loadCaptureBitmap) }
                bitmap?.let {
                    ZoomableImage(
                        bitmap = it.asImageBitmap(),
                        onZoomChanged = { zoomed -> if (page == pagerState.currentPage) zoomedIn = zoomed },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Button(onClick = { viewModel.exportToGallery(capture) }) {
                    Text(stringResource(R.string.viewer_save))
                }
                OutlinedButton(onClick = {
                    val uri = viewModel.shareUri(capture)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/jpeg"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, shareLabel))
                }) {
                    Text(shareLabel)
                }
                OutlinedButton(onClick = { viewModel.deleteCapture(capture) }) {
                    Text(stringResource(R.string.viewer_delete), color = MaterialTheme.colorScheme.error)
                }
                OutlinedButton(onClick = { viewModel.closeViewer() }) {
                    Text(stringResource(R.string.viewer_close))
                }
            }
        }
    }
}

/**
 * An image that supports pinch-to-zoom and pan, while still letting a single-finger
 * horizontal drag fall through to the enclosing HorizontalPager for swiping between
 * photos when not zoomed in. [onZoomChanged] is used by the caller to disable the
 * pager's own swipe gesture while the user is panning around a zoomed-in photo.
 */
@Composable
private fun ZoomableImage(bitmap: ImageBitmap, onZoomChanged: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Image(
        bitmap = bitmap,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
            )
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    val zoomingIn = scale <= 1f
                    scale = if (zoomingIn) 2.5f else 1f
                    offset = Offset.Zero
                    onZoomChanged(zoomingIn)
                })
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        val multitouch = event.changes.size > 1
                        if (multitouch || scale > 1f) {
                            val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                            scale = newScale
                            offset = if (newScale <= 1f) Offset.Zero else offset + panChange
                            onZoomChanged(newScale > 1f)
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    )
}
