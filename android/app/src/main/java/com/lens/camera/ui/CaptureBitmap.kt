package com.lens.camera.ui

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lens.camera.MainViewModel
import com.lens.camera.gallery.Capture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Decodes a capture's bitmap off the main thread at a downsampled [maxDimension] instead of
 * blocking composition with a full-resolution decode (the cause of laggy gallery scrolling
 * and swiping). [CaptureStore][com.lens.camera.gallery.CaptureStore] caches the result, so
 * revisiting the same capture/size is instant.
 */
@Composable
fun rememberCaptureBitmap(viewModel: MainViewModel, capture: Capture?, maxDimension: Int): Bitmap? {
    var bitmap by remember(capture?.id, maxDimension) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(capture?.id, maxDimension) {
        bitmap = if (capture != null) {
            withContext(Dispatchers.IO) { viewModel.loadCaptureBitmap(capture, maxDimension) }
        } else {
            null
        }
    }
    return bitmap
}
