package com.lens.camera.frames

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/** Hand-drawn decorative motifs used on themed collage frames, in a -50..50-ish local box. */
enum class MotifId {
    CORAZON, FLOR, CALABAZA, FANTASMA, MURCIELAGO, ARBOL, COPO, REGALO, ESTRELLA,
    FUEGO_ART, BANDERA, GLOBO, PASTEL, CONFETI, ANILLO, PALOMA, BIRRETE, BIBERON,
    PALMERA, SOL, SANDIA, COPA, ESFERA, NOTA
}

private fun fillPaint(color: Int, alpha: Int = 255): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.FILL
    this.color = color
    this.alpha = alpha
}

private fun strokePaint(color: Int, width: Float, cap: Paint.Cap = Paint.Cap.BUTT): Paint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        this.color = color
        strokeWidth = width
        strokeCap = cap
    }

private fun circlePath(cx: Float, cy: Float, r: Float): Path = Path().apply {
    addCircle(cx, cy, r, Path.Direction.CW)
}

private fun ellipsePath(cx: Float, cy: Float, rx: Float, ry: Float, rotationDeg: Float = 0f): Path {
    val p = Path()
    p.addOval(RectF(cx - rx, cy - ry, cx + rx, cy + ry), Path.Direction.CW)
    if (rotationDeg != 0f) {
        val m = android.graphics.Matrix()
        m.setRotate(rotationDeg, cx, cy)
        p.transform(m)
    }
    return p
}

fun drawMotif(id: MotifId, canvas: Canvas) {
    when (id) {
        MotifId.CORAZON -> {
            val p = Path()
            p.moveTo(0f, 35f)
            p.cubicTo(-55f, -5f, -30f, -45f, 0f, -15f)
            p.cubicTo(30f, -45f, 55f, -5f, 0f, 35f)
            canvas.drawPath(p, fillPaint(Color.parseColor("#FF4D6D")))
        }

        MotifId.FLOR -> {
            val petals = fillPaint(Color.parseColor("#FF8FA3"))
            for (i in 0 until 6) {
                val a = i * Math.PI / 3
                val cx = (Math.cos(a) * 20).toFloat()
                val cy = (Math.sin(a) * 20).toFloat()
                canvas.drawPath(circlePath(cx, cy, 14f), petals)
            }
            canvas.drawPath(circlePath(0f, 0f, 12f), fillPaint(Color.parseColor("#FFD54A")))
        }

        MotifId.CALABAZA -> {
            canvas.drawRect(-4f, -42f, 4f, -28f, fillPaint(Color.parseColor("#2A9D3A")))
            val orange = fillPaint(Color.parseColor("#FF7B00"))
            for (dx in floatArrayOf(-16f, 16f, 0f)) {
                canvas.drawPath(ellipsePath(dx, 5f, 20f, 30f), orange)
            }
            val dark = fillPaint(Color.parseColor("#12081C"))
            val eyeL = Path().apply { moveTo(-16f, -6f); lineTo(-8f, 4f); lineTo(-24f, 4f); close() }
            val eyeR = Path().apply { moveTo(16f, -6f); lineTo(24f, 4f); lineTo(8f, 4f); close() }
            val mouth = Path().apply { moveTo(-14f, 16f); lineTo(14f, 16f); lineTo(0f, 26f); close() }
            canvas.drawPath(eyeL, dark); canvas.drawPath(eyeR, dark); canvas.drawPath(mouth, dark)
        }

        MotifId.FANTASMA -> {
            val body = Path()
            body.addArc(RectF(-25f, -33f, 25f, 17f), 180f, 180f)
            body.lineTo(25f, 26f)
            body.quadTo(17f, 16f, 9f, 26f)
            body.quadTo(0f, 16f, -9f, 26f)
            body.quadTo(-17f, 16f, -25f, 26f)
            body.close()
            canvas.drawPath(body, fillPaint(Color.parseColor("#F2F1EC")))
            val eyes = Path().apply {
                addCircle(-9f, -8f, 4f, Path.Direction.CW)
                addCircle(10f, -8f, 4f, Path.Direction.CW)
            }
            canvas.drawPath(eyes, fillPaint(Color.parseColor("#222222")))
        }

        MotifId.MURCIELAGO -> {
            val purple = fillPaint(Color.parseColor("#3D2B5F"))
            for (s in intArrayOf(1, -1)) {
                val wing = Path()
                wing.moveTo(-4f * s, 0f)
                wing.quadTo(-28f * s, -22f, -46f * s, -6f)
                wing.quadTo(-36f * s, 0f, -32f * s, 8f)
                wing.quadTo(-20f * s, 0f, -4f * s, 10f)
                wing.close()
                canvas.drawPath(wing, purple)
            }
            canvas.drawPath(ellipsePath(0f, 0f, 9f, 14f), purple)
            val earL = Path().apply { moveTo(-7f, -11f); lineTo(-3f, -19f); lineTo(-1f, -11f); close() }
            val earR = Path().apply { moveTo(7f, -11f); lineTo(3f, -19f); lineTo(1f, -11f); close() }
            canvas.drawPath(earL, purple); canvas.drawPath(earR, purple)
        }

        MotifId.ARBOL -> {
            canvas.drawRect(-6f, 26f, 6f, 40f, fillPaint(Color.parseColor("#7A4A21")))
            val green = fillPaint(Color.parseColor("#2A9D3A"))
            for (i in 0 until 3) {
                val w = 44f - i * 11f
                val y = 26f - i * 17f
                val p = Path().apply {
                    moveTo(-w / 2, y); lineTo(w / 2, y); lineTo(0f, y - 26f); close()
                }
                canvas.drawPath(p, green)
            }
            canvas.drawPath(circlePath(0f, -40f, 6f), fillPaint(Color.parseColor("#FFD54A")))
        }

        MotifId.COPO -> {
            val paint = strokePaint(Color.parseColor("#BDE0FE"), 5f, Paint.Cap.ROUND)
            for (i in 0 until 6) {
                val angle = Math.toDegrees(i * Math.PI / 3).toFloat()
                val p = Path()
                p.moveTo(0f, 0f); p.lineTo(0f, -38f)
                p.moveTo(0f, -22f); p.lineTo(-9f, -30f)
                p.moveTo(0f, -22f); p.lineTo(9f, -30f)
                val m = android.graphics.Matrix().apply { setRotate(angle) }
                p.transform(m)
                canvas.drawPath(p, paint)
            }
        }

        MotifId.REGALO -> {
            canvas.drawRect(-24f, -12f, 24f, 28f, fillPaint(Color.parseColor("#D62828")))
            val gold = fillPaint(Color.parseColor("#FFD54A"))
            canvas.drawRect(-5f, -12f, 5f, 28f, gold)
            canvas.drawRect(-28f, -20f, 28f, -10f, gold)
            val bow = strokePaint(Color.parseColor("#FFD54A"), 5f)
            canvas.drawPath(circlePath(-10f, -26f, 7f), bow)
            canvas.drawPath(circlePath(10f, -26f, 7f), bow)
        }

        MotifId.ESTRELLA -> canvas.drawPath(starPath(), fillPaint(Color.parseColor("#FFD54A")))

        MotifId.FUEGO_ART -> {
            val colors = intArrayOf(Color.parseColor("#FFD54A"), Color.parseColor("#FF4D6D"), Color.parseColor("#4CC9F0"))
            for (i in 0 until 12) {
                val a = i * Math.PI / 6
                val c = colors[i % 3]
                val line = strokePaint(c, 4f, Paint.Cap.ROUND)
                val p = Path().apply {
                    moveTo((Math.cos(a) * 10).toFloat(), (Math.sin(a) * 10).toFloat())
                    lineTo((Math.cos(a) * 36).toFloat(), (Math.sin(a) * 36).toFloat())
                }
                canvas.drawPath(p, line)
                canvas.drawPath(circlePath((Math.cos(a) * 41).toFloat(), (Math.sin(a) * 41).toFloat(), 3.5f), fillPaint(c))
            }
        }

        MotifId.BANDERA -> {
            canvas.drawRect(-36f, -24f, -32f, 42f, fillPaint(Color.parseColor("#9AA2AD")))
            canvas.drawRect(-32f, -22f, 32f, 20f, fillPaint(Color.parseColor("#F2F1EC")))
            val red = fillPaint(Color.parseColor("#C1121F"))
            for (i in 0 until 3) canvas.drawRect(-32f, -22f + i * 16f, 32f, -22f + i * 16f + 8f, red)
            canvas.drawRect(-32f, -22f, -4f, -2f, fillPaint(Color.parseColor("#1D3A8F")))
            val star = fillPaint(Color.WHITE)
            for (r in 0 until 2) for (c in 0 until 3) {
                canvas.drawPath(circlePath(-26f + c * 9f, -17f + r * 9f, 2f), star)
            }
        }

        MotifId.GLOBO -> {
            val string = strokePaint(Color.parseColor("#CFD8E3"), 2f)
            val stringPath = Path().apply { moveTo(0f, 20f); quadTo(7f, 32f, 0f, 44f) }
            canvas.drawPath(stringPath, string)
            canvas.drawPath(ellipsePath(0f, -6f, 18f, 24f), fillPaint(Color.parseColor("#FF6EC7")))
            val knot = Path().apply { moveTo(-5f, 17f); lineTo(5f, 17f); lineTo(0f, 24f); close() }
            canvas.drawPath(knot, fillPaint(Color.parseColor("#FF6EC7")))
            canvas.drawPath(ellipsePath(-7f, -14f, 5f, 8f, -23f), fillPaint(Color.argb(128, 255, 255, 255)))
        }

        MotifId.PASTEL -> {
            canvas.drawRect(-28f, 10f, 28f, 30f, fillPaint(Color.parseColor("#7A4A21")))
            canvas.drawRect(-22f, -6f, 22f, 10f, fillPaint(Color.parseColor("#FFCCD5")))
            canvas.drawRect(-3f, -24f, 3f, -6f, fillPaint(Color.parseColor("#4CC9F0")))
            canvas.drawPath(ellipsePath(0f, -29f, 4f, 6f), fillPaint(Color.parseColor("#FFD54A")))
        }

        MotifId.CONFETI -> {
            val colors = intArrayOf(
                Color.parseColor("#FFD54A"), Color.parseColor("#FF4D6D"), Color.parseColor("#4CC9F0"),
                Color.parseColor("#2A9D3A"), Color.parseColor("#B5179E")
            )
            for (i in 0 until 14) {
                val paint = fillPaint(colors[i % 5])
                val cx = (Math.sin(i * 7.0) * 36).toFloat()
                val cy = (Math.cos(i * 5.0) * 36).toFloat()
                val rectPath = Path().apply { addRect(-5f, -3f, 5f, 3f, Path.Direction.CW) }
                val m = android.graphics.Matrix().apply {
                    setRotate(Math.toDegrees(i.toDouble()).toFloat())
                    postTranslate(cx, cy)
                }
                rectPath.transform(m)
                canvas.drawPath(rectPath, paint)
            }
        }

        MotifId.ANILLO -> {
            canvas.drawPath(circlePath(0f, 12f, 22f), strokePaint(Color.parseColor("#FFD700"), 8f))
            val gem = Path().apply {
                moveTo(0f, -36f); lineTo(13f, -22f); lineTo(0f, -8f); lineTo(-13f, -22f); close()
            }
            canvas.drawPath(gem, fillPaint(Color.parseColor("#D6F5FF")))
            canvas.drawPath(gem, strokePaint(Color.parseColor("#9AD4E8"), 2f))
        }

        MotifId.PALOMA -> {
            val white = fillPaint(Color.parseColor("#F2F1EC"))
            canvas.drawPath(ellipsePath(2f, 6f, 22f, 12f, -14f), white)
            val wing = Path().apply {
                moveTo(0f, 2f); quadTo(-8f, -32f, 18f, -24f); quadTo(8f, -8f, 4f, 2f); close()
            }
            canvas.drawPath(wing, white)
            canvas.drawPath(circlePath(23f, -3f, 7f), white)
            val beak = Path().apply { moveTo(29f, -5f); lineTo(38f, -3f); lineTo(29f, -1f); close() }
            canvas.drawPath(beak, fillPaint(Color.parseColor("#FFB703")))
            canvas.drawPath(circlePath(24f, -5f, 1.5f), fillPaint(Color.parseColor("#222222")))
        }

        MotifId.BIRRETE -> {
            val navy = fillPaint(Color.parseColor("#14213D"))
            val cap = Path().apply {
                moveTo(0f, -18f); lineTo(38f, -4f); lineTo(0f, 10f); lineTo(-38f, -4f); close()
            }
            canvas.drawPath(cap, navy)
            canvas.drawRect(-14f, 2f, 14f, 16f, navy)
            val tassel = strokePaint(Color.parseColor("#FFD54A"), 3f)
            val tasselPath = Path().apply { moveTo(0f, -4f); lineTo(30f, 6f); lineTo(30f, 22f) }
            canvas.drawPath(tasselPath, tassel)
            canvas.drawPath(circlePath(30f, 26f, 5f), fillPaint(Color.parseColor("#FFD54A")))
        }

        MotifId.BIBERON -> {
            val bottle = Path().apply {
                addRoundRect(RectF(-13f, -8f, 13f, 34f), 8f, 8f, Path.Direction.CW)
            }
            canvas.drawPath(bottle, fillPaint(Color.parseColor("#8ECAE6")))
            canvas.drawRect(-9f, -20f, 9f, -8f, fillPaint(Color.parseColor("#FFB5C2")))
            canvas.drawPath(ellipsePath(0f, -27f, 6f, 9f), fillPaint(Color.parseColor("#F2F1EC")))
            val marks = strokePaint(Color.argb(153, 255, 255, 255), 2f)
            for (y in floatArrayOf(2f, 12f, 22f)) {
                canvas.drawLine(5f, y, 11f, y, marks)
            }
        }

        MotifId.PALMERA -> {
            val trunk = strokePaint(Color.parseColor("#7A4A21"), 8f, Paint.Cap.ROUND)
            val trunkPath = Path().apply { moveTo(5f, 44f); quadTo(-3f, 12f, 0f, -12f) }
            canvas.drawPath(trunkPath, trunk)
            val frond = strokePaint(Color.parseColor("#2A9D3A"), 6f)
            for (a in doubleArrayOf(-2.7, -2.1, -1.1, -.5, .2)) {
                val p = Path().apply {
                    moveTo(0f, -12f)
                    quadTo(
                        (Math.cos(a) * 22).toFloat(), -20f + (Math.sin(a) * 20).toFloat(),
                        (Math.cos(a) * 38).toFloat(), -12f + (Math.sin(a) * 28).toFloat()
                    )
                }
                canvas.drawPath(p, frond)
            }
        }

        MotifId.SOL -> {
            canvas.drawPath(circlePath(0f, 0f, 20f), fillPaint(Color.parseColor("#FFD54A")))
            val ray = strokePaint(Color.parseColor("#FFD54A"), 5f, Paint.Cap.ROUND)
            for (i in 0 until 8) {
                val a = i * Math.PI / 4
                val p = Path().apply {
                    moveTo((Math.cos(a) * 27).toFloat(), (Math.sin(a) * 27).toFloat())
                    lineTo((Math.cos(a) * 38).toFloat(), (Math.sin(a) * 38).toFloat())
                }
                canvas.drawPath(p, ray)
            }
        }

        MotifId.SANDIA -> {
            val rind = Path().apply { addArc(RectF(-34f, -26f, 34f, 42f), 180f, 180f); close() }
            canvas.drawPath(rind, fillPaint(Color.parseColor("#2A9D3A")))
            val flesh = Path().apply { addArc(RectF(-27f, -19f, 27f, 35f), 180f, 180f); close() }
            canvas.drawPath(flesh, fillPaint(Color.parseColor("#FF4D6D")))
            val seed = fillPaint(Color.parseColor("#222222"))
            for (p in arrayOf(floatArrayOf(-13f, 0f), floatArrayOf(0f, -6f), floatArrayOf(13f, 0f))) {
                canvas.drawPath(ellipsePath(p[0], p[1] + 12f, 2.5f, 4f), seed)
            }
        }

        MotifId.COPA -> {
            val gold = fillPaint(Color.argb(217, 255, 215, 120))
            val outline = strokePaint(Color.parseColor("#E8D5B0"), 3f)
            val cup = Path().apply {
                moveTo(-10f, -36f); lineTo(-6f, 4f); quadTo(0f, 12f, 6f, 4f); lineTo(10f, -36f); close()
            }
            canvas.drawPath(cup, gold)
            canvas.drawPath(cup, outline)
            val stem = strokePaint(Color.parseColor("#E8D5B0"), 3f)
            val stemPath = Path().apply {
                moveTo(0f, 10f); lineTo(0f, 32f); moveTo(-10f, 36f); lineTo(10f, 36f)
            }
            canvas.drawPath(stemPath, stem)
            val bubble = fillPaint(Color.WHITE)
            for (p in arrayOf(floatArrayOf(-3f, -26f), floatArrayOf(3f, -16f), floatArrayOf(-1f, -6f))) {
                canvas.drawPath(circlePath(p[0], p[1], 2f), bubble)
            }
        }

        MotifId.ESFERA -> {
            canvas.drawRect(-2f, -34f, 2f, -26f, fillPaint(Color.parseColor("#9AA2AD")))
            canvas.save()
            canvas.clipPath(circlePath(0f, 2f, 26f))
            canvas.drawRect(-26f, -24f, 26f, 28f, fillPaint(Color.parseColor("#CFD8E3")))
            val grid = strokePaint(Color.parseColor("#8A94A6"), 2f)
            for (i in -2..2) {
                canvas.drawLine(-26f, 2f + i * 10f, 26f, 2f + i * 10f, grid)
                canvas.drawLine(i * 11f, -24f, i * 11f, 28f, grid)
            }
            canvas.restore()
        }

        MotifId.NOTA -> {
            val white = fillPaint(Color.parseColor("#F2F1EC"))
            canvas.drawPath(ellipsePath(-12f, 26f, 10f, 7f, -17f), white)
            canvas.drawPath(ellipsePath(17f, 20f, 10f, 7f, -17f), white)
            canvas.drawRect(-4f, -22f, 0f, 26f, white)
            canvas.drawRect(23f, -28f, 27f, 20f, white)
            val beam = Path().apply {
                moveTo(-4f, -22f); lineTo(27f, -28f); lineTo(27f, -16f); lineTo(-4f, -10f); close()
            }
            canvas.drawPath(beam, white)
        }
    }
}

private fun starPath(): Path {
    val p = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) 36.0 else 15.0
        val a = i * Math.PI / 5 - Math.PI / 2
        val x = (Math.cos(a) * r).toFloat()
        val y = (Math.sin(a) * r).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    return p
}
