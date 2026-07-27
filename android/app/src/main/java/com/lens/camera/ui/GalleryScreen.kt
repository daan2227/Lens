package com.lens.camera.ui

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
        ViewerDialog(viewModel = viewModel, capture = state.captures[state.viewerIndex])
    }
}

@Composable
private fun ViewerDialog(viewModel: MainViewModel, capture: Capture) {
    val bitmap = remember(capture.id) { viewModel.loadCaptureBitmap(capture) }
    val context = LocalContext.current
    val shareLabel = stringResource(R.string.viewer_share)

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.96f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            bitmap?.let {
                Image(
                    it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .aspectRatio(3f / 4f)
                        .padding(bottom = 18.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
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
