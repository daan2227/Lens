package com.lens.camera.collage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.content.res.ResourcesCompat
import com.lens.camera.R
import com.lens.camera.frames.FrameTheme
import com.lens.camera.frames.drawMotif

const val COLLAGE_WIDTH = 1080
const val COLLAGE_HEIGHT = 1440
private const val GAP = 8f
private const val BORDER_WIDTH = 26f

/** Center-crops [src] to fill the destination rect, matching the web version's cover-fit math. */
private fun drawCover(canvas: Canvas, src: Bitmap, dst: RectF, paint: Paint) {
    val srcRatio = src.width.toFloat() / src.height.toFloat()
    val dstRatio = dst.width() / dst.height()
    var sw = src.width.toFloat()
    var sh = src.height.toFloat()
    var sx = 0f
    var sy = 0f
    if (srcRatio > dstRatio) {
        sw = src.height * dstRatio
        sx = (src.width - sw) / 2f
    } else {
        sh = src.width / dstRatio
        sy = (src.height - sh) / 2f
    }
    val srcRect = android.graphics.Rect(sx.toInt(), sy.toInt(), (sx + sw).toInt(), (sy + sh).toInt())
    canvas.drawBitmap(src, srcRect, dst, paint)
}

/** Composites [shots] onto the chosen [layout] and paints the [frame] theme on top. */
fun buildCollage(
    context: Context,
    shots: List<Bitmap>,
    layout: Layout,
    frame: FrameTheme,
    banner: String?
): Bitmap {
    val bitmap = Bitmap.createBitmap(COLLAGE_WIDTH, COLLAGE_HEIGHT, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(frame.bg)

    val margin = if (frame.border != null) 54f else 0f
    val innerW = COLLAGE_WIDTH - 2 * margin
    val innerH = COLLAGE_HEIGHT - 2 * margin
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    layout.cells.forEachIndexed { i, cell ->
        val shot = shots.getOrNull(i) ?: return@forEachIndexed
        val dst = RectF(
            margin + cell.x * innerW + GAP,
            margin + cell.y * innerH + GAP,
            margin + (cell.x + cell.w) * innerW - GAP,
            margin + (cell.y + cell.h) * innerH - GAP
        )
        drawCover(canvas, shot, dst, paint)
    }

    drawFrameTheme(context, canvas, frame, banner, COLLAGE_WIDTH.toFloat(), COLLAGE_HEIGHT.toFloat())
    return bitmap
}

private fun drawFrameTheme(context: Context, canvas: Canvas, frame: FrameTheme, banner: String?, w: Float, h: Float) {
    val border = frame.border ?: return
    val bw = BORDER_WIDTH

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = bw
        shader = LinearGradient(0f, 0f, w, h, border.first, border.second, Shader.TileMode.CLAMP)
    }
    canvas.drawRect(bw / 2, bw / 2, w - bw / 2, h - bw / 2, borderPaint)

    val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.argb(128, 255, 255, 255)
    }
    val inset = bw + 6f
    canvas.drawRect(inset, inset, w - inset, h - inset, innerPaint)

    if (frame.motifs.isNotEmpty()) {
        val spots = listOf(
            floatArrayOf(bw + 34f, bw + 34f), floatArrayOf(w - bw - 34f, bw + 34f),
            floatArrayOf(bw + 34f, h - bw - 34f), floatArrayOf(w - bw - 34f, h - bw - 34f),
            floatArrayOf(w * .28f, bw + 14f), floatArrayOf(w * .72f, bw + 14f), floatArrayOf(w * .5f, h - bw - 14f),
            floatArrayOf(bw + 14f, h * .35f), floatArrayOf(bw + 14f, h * .68f),
            floatArrayOf(w - bw - 14f, h * .35f), floatArrayOf(w - bw - 14f, h * .68f)
        )
        spots.forEachIndexed { i, p ->
            val motif = frame.motifs[i % frame.motifs.size]
            val rotation = ((i * 137) % 40 - 20).toFloat()
            canvas.save()
            canvas.translate(p[0], p[1])
            canvas.rotate(rotation)
            canvas.scale(0.62f, 0.62f)
            drawMotif(motif, canvas)
            canvas.restore()
        }
    }

    if (!banner.isNullOrEmpty()) {
        val typeface = ResourcesCompat.getFont(context, R.font.space_grotesk_bold)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 44f
            textAlign = Paint.Align.CENTER
            color = Color.WHITE
        }
        val textWidth = textPaint.measureText(banner)
        val pillWidth = textWidth + 70f
        val pillHeight = 72f
        val pillTop = h - bw - 100f
        val pillLeft = (w - pillWidth) / 2f
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(140, 0, 0, 0) }
        val pillPath = Path().apply {
            addRoundRect(
                RectF(pillLeft, pillTop, pillLeft + pillWidth, pillTop + pillHeight),
                36f, 36f, Path.Direction.CW
            )
        }
        canvas.drawPath(pillPath, pillPaint)
        val centerY = pillTop + pillHeight / 2f
        val baseline = centerY - (textPaint.ascent() + textPaint.descent()) / 2f
        canvas.drawText(banner, w / 2f, baseline, textPaint)
    }
}
