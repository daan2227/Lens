package com.lens.camera.filters

import android.graphics.ColorMatrix
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A single filter operation, mirroring one CSS filter() function
 * (https://drafts.fxtf.org/filter-effects/#supported-filter-functions).
 * Kept as data so filters can be combined and reasoned about individually
 * instead of hand-rolling one matrix per named filter.
 */
sealed class FilterOp {
    data class Brightness(val amount: Float) : FilterOp()
    data class Contrast(val amount: Float) : FilterOp()
    data class Saturate(val amount: Float) : FilterOp()
    data class Grayscale(val amount: Float) : FilterOp()
    data class Sepia(val amount: Float) : FilterOp()
    data class HueRotate(val degrees: Float) : FilterOp()
}

private fun identity() = ColorMatrix()

private fun brightnessMatrix(amount: Float): ColorMatrix = ColorMatrix(
    floatArrayOf(
        amount, 0f, 0f, 0f, 0f,
        0f, amount, 0f, 0f, 0f,
        0f, 0f, amount, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )
)

private fun contrastMatrix(amount: Float): ColorMatrix {
    val offset = 128f * (1f - amount)
    return ColorMatrix(
        floatArrayOf(
            amount, 0f, 0f, 0f, offset,
            0f, amount, 0f, 0f, offset,
            0f, 0f, amount, 0f, offset,
            0f, 0f, 0f, 1f, 0f
        )
    )
}

/** Same luminance-preserving formula the CSS spec uses for both saturate() and grayscale(). */
private fun saturateMatrix(amount: Float): ColorMatrix {
    val s = amount
    return ColorMatrix(
        floatArrayOf(
            0.213f + 0.787f * s, 0.715f - 0.715f * s, 0.072f - 0.072f * s, 0f, 0f,
            0.213f - 0.213f * s, 0.715f + 0.285f * s, 0.072f - 0.072f * s, 0f, 0f,
            0.213f - 0.213f * s, 0.715f - 0.715f * s, 0.072f + 0.928f * s, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
}

private fun grayscaleMatrix(amount: Float): ColorMatrix = saturateMatrix(1f - amount)

private fun sepiaMatrix(amount: Float): ColorMatrix {
    val a = amount
    fun lerp(identityDiag: Float, sepiaVal: Float) = identityDiag * (1f - a) + sepiaVal * a
    return ColorMatrix(
        floatArrayOf(
            lerp(1f, 0.393f), lerp(0f, 0.769f), lerp(0f, 0.189f), 0f, 0f,
            lerp(0f, 0.349f), lerp(1f, 0.686f), lerp(0f, 0.168f), 0f, 0f,
            lerp(0f, 0.272f), lerp(0f, 0.534f), lerp(1f, 0.131f), 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
}

/** Standard SVG/CSS hueRotate matrix (luminance-preserving hue rotation). */
private fun hueRotateMatrix(degrees: Float): ColorMatrix {
    val rad = degrees * PI.toFloat() / 180f
    val c = cos(rad)
    val s = sin(rad)
    return ColorMatrix(
        floatArrayOf(
            0.213f + c * 0.787f - s * 0.213f, 0.715f - c * 0.715f - s * 0.715f, 0.072f - c * 0.072f + s * 0.928f, 0f, 0f,
            0.213f - c * 0.213f + s * 0.143f, 0.715f + c * 0.285f + s * 0.140f, 0.072f - c * 0.072f - s * 0.283f, 0f, 0f,
            0.213f - c * 0.213f - s * 0.787f, 0.715f - c * 0.715f + s * 0.715f, 0.072f + c * 0.928f + s * 0.072f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
}

private fun FilterOp.toColorMatrix(): ColorMatrix = when (this) {
    is FilterOp.Brightness -> brightnessMatrix(amount)
    is FilterOp.Contrast -> contrastMatrix(amount)
    is FilterOp.Saturate -> saturateMatrix(amount)
    is FilterOp.Grayscale -> grayscaleMatrix(amount)
    is FilterOp.Sepia -> sepiaMatrix(amount)
    is FilterOp.HueRotate -> hueRotateMatrix(degrees)
}

/**
 * Combines filter ops in listed order (first op applied to the source image first,
 * matching CSS `filter: a() b() c()` semantics) into a single ColorMatrix.
 */
fun buildColorMatrix(ops: List<FilterOp>): ColorMatrix {
    val result = identity()
    for (op in ops) {
        result.postConcat(op.toColorMatrix())
    }
    return result
}
