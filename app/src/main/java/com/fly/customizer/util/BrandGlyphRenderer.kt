package com.fly.customizer.util

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface

/**
 * Draws the simplified brand glyphs. Each shape is built from primitives so the artwork scales
 * cleanly and never depends on bundled brand assets.
 */
object BrandGlyphRenderer {

    fun draw(canvas: Canvas, glyph: BrandGlyph, color: Int, detailColor: Int, size: Float) {
        val box = size * 0.52f
        val left = (size - box) / 2f
        val top = (size - box) / 2f
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

        when (glyph) {
            BrandGlyph.NOTE -> drawNote(canvas, fill, left, top, box)
            BrandGlyph.PLAY -> drawPlay(canvas, fill, left, top, box)
            BrandGlyph.FACE -> drawFace(canvas, fill, detailColor, left, top, box)
            BrandGlyph.GHOST -> drawGhost(canvas, fill, detailColor, left, top, box)
            BrandGlyph.CROSS -> drawCross(canvas, fill, left, top, box)
            BrandGlyph.DOLLAR -> drawCenteredText(canvas, "$", color, size, box * 0.95f)
            BrandGlyph.BLOCK -> drawBlock(canvas, fill, detailColor, left, top, box, size)
            BrandGlyph.WAVES -> drawWaves(canvas, fill, left, top, box)
            BrandGlyph.RING -> drawRing(canvas, fill, left, top, box)
            BrandGlyph.PLANE -> drawPlane(canvas, fill, detailColor, left, top, box)
            BrandGlyph.ENVELOPE -> drawEnvelope(canvas, fill, detailColor, left, top, box)
            BrandGlyph.EFF -> drawCenteredText(canvas, "f", color, size, box * 1.05f)
        }
    }

    private fun drawNote(canvas: Canvas, fill: Paint, left: Float, top: Float, box: Float) {
        val head = box * 0.16f
        canvas.drawOval(
            RectF(left + box * 0.04f, top + box * 0.60f, left + box * 0.04f + head * 2.1f, top + box * 0.60f + head * 1.7f),
            fill,
        )
        canvas.drawOval(
            RectF(left + box * 0.54f, top + box * 0.47f, left + box * 0.54f + head * 2.1f, top + box * 0.47f + head * 1.7f),
            fill,
        )
        val stroke = Paint(fill).apply {
            style = Paint.Style.STROKE
            strokeWidth = box * 0.085f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(left + box * 0.33f, top + box * 0.64f, left + box * 0.33f, top + box * 0.16f, stroke)
        canvas.drawLine(left + box * 0.83f, top + box * 0.51f, left + box * 0.83f, top + box * 0.08f, stroke)
        canvas.drawLine(left + box * 0.33f, top + box * 0.16f, left + box * 0.83f, top + box * 0.08f, stroke)
    }

    private fun drawPlay(canvas: Canvas, fill: Paint, left: Float, top: Float, box: Float) {
        val paint = Paint(fill).apply {
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = box * 0.16f
            strokeJoin = Paint.Join.ROUND
        }
        val path = Path().apply {
            moveTo(left + box * 0.26f, top + box * 0.12f)
            lineTo(left + box * 0.90f, top + box * 0.50f)
            lineTo(left + box * 0.26f, top + box * 0.88f)
            close()
        }
        canvas.drawPath(path, paint)
    }

    private fun drawFace(
        canvas: Canvas,
        fill: Paint,
        detailColor: Int,
        left: Float,
        top: Float,
        box: Float,
    ) {
        val body = Paint(fill).apply {
            style = Paint.Style.STROKE
            strokeWidth = box * 0.10f
        }
        canvas.drawRoundRect(
            RectF(left + box * 0.02f, top + box * 0.26f, left + box * 0.98f, top + box * 0.80f),
            box * 0.24f,
            box * 0.24f,
            body,
        )
        val eyes = Paint(fill).apply { color = detailColor }
        canvas.drawCircle(left + box * 0.33f, top + box * 0.53f, box * 0.085f, eyes)
        canvas.drawCircle(left + box * 0.67f, top + box * 0.53f, box * 0.085f, eyes)
    }

    private fun drawGhost(
        canvas: Canvas,
        fill: Paint,
        detailColor: Int,
        left: Float,
        top: Float,
        box: Float,
    ) {
        val path = Path()
        path.moveTo(left + box * 0.08f, top + box * 0.88f)
        path.lineTo(left + box * 0.08f, top + box * 0.45f)
        path.cubicTo(
            left + box * 0.08f, top - box * 0.04f,
            left + box * 0.92f, top - box * 0.04f,
            left + box * 0.92f, top + box * 0.45f,
        )
        path.lineTo(left + box * 0.92f, top + box * 0.88f)
        path.cubicTo(
            left + box * 0.78f, top + box * 0.99f,
            left + box * 0.66f, top + box * 0.72f,
            left + box * 0.50f, top + box * 0.88f,
        )
        path.cubicTo(
            left + box * 0.34f, top + box * 0.99f,
            left + box * 0.22f, top + box * 0.72f,
            left + box * 0.08f, top + box * 0.88f,
        )
        path.close()
        canvas.drawPath(path, fill)

        val eyes = Paint(fill).apply { color = detailColor }
        canvas.drawCircle(left + box * 0.33f, top + box * 0.44f, box * 0.075f, eyes)
        canvas.drawCircle(left + box * 0.67f, top + box * 0.44f, box * 0.075f, eyes)
    }

    private fun drawCross(canvas: Canvas, fill: Paint, left: Float, top: Float, box: Float) {
        val stroke = Paint(fill).apply {
            style = Paint.Style.STROKE
            strokeWidth = box * 0.20f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(left + box * 0.12f, top + box * 0.12f, left + box * 0.88f, top + box * 0.88f, stroke)
        canvas.drawLine(left + box * 0.88f, top + box * 0.12f, left + box * 0.12f, top + box * 0.88f, stroke)
    }

    private fun drawBlock(
        canvas: Canvas,
        fill: Paint,
        detailColor: Int,
        left: Float,
        top: Float,
        box: Float,
        size: Float,
    ) {
        canvas.save()
        canvas.rotate(12f, size / 2f, size / 2f)
        canvas.drawRoundRect(
            RectF(left + box * 0.14f, top + box * 0.14f, left + box * 0.86f, top + box * 0.86f),
            box * 0.16f,
            box * 0.16f,
            fill,
        )
        canvas.drawRoundRect(
            RectF(left + box * 0.40f, top + box * 0.40f, left + box * 0.60f, top + box * 0.60f),
            box * 0.05f,
            box * 0.05f,
            Paint(fill).apply { color = detailColor },
        )
        canvas.restore()
    }

    private fun drawWaves(canvas: Canvas, fill: Paint, left: Float, top: Float, box: Float) {
        val stroke = Paint(fill).apply {
            style = Paint.Style.STROKE
            strokeWidth = box * 0.10f
            strokeCap = Paint.Cap.ROUND
        }
        val centerY = top + box * 0.5f
        val centerX = left
        for (index in 0..2) {
            val radius = box * (0.30f + index * 0.20f)
            canvas.drawArc(
                RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius),
                -46f,
                92f,
                false,
                stroke,
            )
        }
    }

    private fun drawRing(canvas: Canvas, fill: Paint, left: Float, top: Float, box: Float) {
        val stroke = Paint(fill).apply {
            style = Paint.Style.STROKE
            strokeWidth = box * 0.14f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(
            RectF(left + box * 0.06f, top + box * 0.06f, left + box * 0.94f, top + box * 0.94f),
            -115f,
            285f,
            false,
            stroke,
        )
        canvas.drawCircle(
            left + box * 0.90f,
            top + box * 0.24f,
            box * 0.09f,
            Paint(fill).apply { style = Paint.Style.FILL },
        )
    }

    private fun drawPlane(canvas: Canvas, fill: Paint, detailColor: Int, left: Float, top: Float, box: Float) {
        val path = Path().apply {
            moveTo(left + box * 0.04f, top + box * 0.50f)
            lineTo(left + box * 0.96f, top + box * 0.10f)
            lineTo(left + box * 0.60f, top + box * 0.92f)
            lineTo(left + box * 0.44f, top + box * 0.56f)
            close()
        }
        canvas.drawPath(path, fill)
        val fold = Paint(fill).apply {
            style = Paint.Style.STROKE
            strokeWidth = box * 0.05f
            color = detailColor
        }
        canvas.drawLine(left + box * 0.44f, top + box * 0.56f, left + box * 0.96f, top + box * 0.10f, fold)
    }

    private fun drawEnvelope(
        canvas: Canvas,
        fill: Paint,
        detailColor: Int,
        left: Float,
        top: Float,
        box: Float,
    ) {
        val body = RectF(
            left + box * 0.04f,
            top + box * 0.22f,
            left + box * 0.96f,
            top + box * 0.78f,
        )
        canvas.drawRoundRect(body, box * 0.10f, box * 0.10f, fill)
        val fold = Paint(fill).apply {
            style = Paint.Style.STROKE
            strokeWidth = box * 0.09f
            color = detailColor
        }
        canvas.drawLine(body.left, body.top, left + box * 0.5f, top + box * 0.55f, fold)
        canvas.drawLine(body.right, body.top, left + box * 0.5f, top + box * 0.55f, fold)
    }

    private fun drawCenteredText(
        canvas: Canvas,
        text: String,
        color: Int,
        size: Float,
        textSize: Float,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            this.textSize = textSize
        }
        val metrics = paint.fontMetrics
        val baseline = size / 2f - (metrics.ascent + metrics.descent) / 2f
        canvas.drawText(text, size / 2f, baseline, paint)
    }
}
