package com.lens.camera.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lens.camera.MainViewModel
import com.lens.camera.R
import com.lens.camera.collage.LAYOUTS
import com.lens.camera.frames.FRAMES

@Composable
fun CollageEditorScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val margin = if (FRAMES[state.frameIndex].border != null) 54f else 0f
    val density = LocalDensity.current

    val preview = remember(state.collageShots, state.layoutIndex, state.frameIndex) {
        if (state.collageShots.isNotEmpty()) viewModel.buildCollagePreview() else null
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.editor_title),
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        BoxWithConstraints(Modifier.fillMaxWidth(0.86f).aspectRatio(3f / 4f)) {
            val wPx = constraints.maxWidth.toFloat()
            val hPx = constraints.maxHeight.toFloat()
            val innerW = wPx - 2 * margin
            val innerH = hPx - 2 * margin

            preview?.let {
                Image(
                    it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            LAYOUTS[state.layoutIndex].cells.forEachIndexed { i, cell ->
                val selected = i == state.editorSelected
                val xDp = with(density) { (margin + cell.x * innerW).toDp() }
                val yDp = with(density) { (margin + cell.y * innerH).toDp() }
                val wDp = with(density) { (cell.w * innerW).toDp() }
                val hDp = with(density) { (cell.h * innerH).toDp() }
                Box(
                    Modifier
                        .offset(x = xDp, y = yDp)
                        .size(wDp, hDp)
                        .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent)
                        .clickable { viewModel.openEditorCell(i) }
                )
            }
        }

        Text(
            stringResource(R.string.editor_hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            modifier = Modifier.padding(vertical = 10.dp)
        )

        val sameCountLayouts = LAYOUTS.withIndex().filter { it.value.cells.size == state.collageShots.size }
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            sameCountLayouts.forEach { (i, layout) ->
                val selected = i == state.layoutIndex
                Box(
                    Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(12.dp))
                        .clickable { viewModel.selectLayout(i) }
                ) {
                    layout.cells.forEach { cell ->
                        Box(
                            Modifier
                                .offset((cell.x * 48).dp, (cell.y * 48).dp)
                                .size((cell.w * 48 - 3).dp, (cell.h * 48 - 3).dp)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary else Color(0xFF3A3A44),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }
        }

        Row(
            Modifier.padding(vertical = 10.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FRAMES.forEachIndexed { i, frame ->
                val selected = i == state.frameIndex
                Box(
                    Modifier
                        .size(40.dp)
                        .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(10.dp))
                        .padding(2.dp)
                        .background(Color(frame.bg), RoundedCornerShape(8.dp))
                        .clickable { viewModel.selectFrame(i) }
                )
            }
        }

        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { viewModel.requestReplace() }) {
                Text(stringResource(R.string.editor_replace))
            }
            Button(
                onClick = { viewModel.saveCollage() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.editor_save), color = MaterialTheme.colorScheme.onPrimary)
            }
            OutlinedButton(
                onClick = { viewModel.cancelCollage() },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.editor_discard), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
