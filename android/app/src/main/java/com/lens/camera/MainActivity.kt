package com.lens.camera

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lens.camera.ui.CameraScreen
import com.lens.camera.ui.CollageEditorScreen
import com.lens.camera.ui.GalleryScreen
import com.lens.camera.ui.theme.LensTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LensTheme {
                App()
            }
        }
    }
}

private val requiredPermissions: Array<String> = buildList {
    add(Manifest.permission.CAMERA)
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
        add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}.toTypedArray()

@Composable
private fun App() {
    val viewModel: MainViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    var permissionsRequested by remember { mutableStateOf(false) }
    var cameraGranted by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        cameraGranted = result[Manifest.permission.CAMERA] == true
        permissionsRequested = true
        if (!cameraGranted) {
            viewModel.setCameraReady(ready = false, demo = true)
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(requiredPermissions)
    }

    Box(Modifier.fillMaxSize()) {
        when {
            state.galleryOpen -> GalleryScreen(viewModel = viewModel)
            state.editorOpen -> CollageEditorScreen(viewModel = viewModel)
            else -> CameraScreen(viewModel = viewModel, cameraPermissionGranted = permissionsRequested && cameraGranted)
        }

        state.toast?.let { message ->
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 130.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            ) {
                Text(message, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
            }
        }
    }

    LaunchedEffect(state.toast) {
        if (state.toast != null) {
            delay(1600)
            viewModel.dismissToast()
        }
    }
}
