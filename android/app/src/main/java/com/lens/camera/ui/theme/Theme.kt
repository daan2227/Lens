package com.lens.camera.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Night palette (matches the original always-dark design 1:1).
private val LensAccent = Color(0xFFFFD54A)
private val LensRec = Color(0xFFE5484D)

private val DarkColors = darkColorScheme(
    primary = LensAccent,
    onPrimary = Color(0xFF1A1400),
    background = Color(0xFF060608),
    onBackground = Color(0xFFF2F1EC),
    surface = Color(0xFF101014),
    onSurface = Color(0xFFF2F1EC),
    surfaceVariant = Color(0xFF17171D),
    onSurfaceVariant = Color(0xFF8A8A93),
    error = LensRec,
    onError = Color(0xFFFFFFFF)
)

// Day palette: chrome (panels, gallery, editor) switches to a light surface;
// the camera viewfinder itself stays black regardless of theme, like every
// real camera app, since inverting a live photo backdrop makes no sense.
private val LightColors = lightColorScheme(
    primary = LensAccent,
    onPrimary = Color(0xFF1A1400),
    background = Color(0xFFF5F5F2),
    onBackground = Color(0xFF17171B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17171B),
    surfaceVariant = Color(0xFFECECE7),
    onSurfaceVariant = Color(0xFF6B6B73),
    error = LensRec,
    onError = Color(0xFFFFFFFF)
)

val Viewfinder = Color(0xFF000000)

@Composable
fun LensTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = LensTypography, content = content)
}
