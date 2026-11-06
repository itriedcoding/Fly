package com.fly.customizer.util

/**
 * Pure colour helpers (no Android framework calls) so they stay unit-testable.
 */
object ColorMath {

    fun alpha(argb: Int): Int = (argb ushr 24) and 0xFF

    fun shiftHue(argb: Int, degrees: Float): Int {
        val hsv = toHsv(argb)
        val shifted = ((hsv[0] + degrees) % 360f + 360f) % 360f
        return fromHsv(shifted, hsv[1], hsv[2], alpha(argb))
    }

    fun withAlpha(argb: Int, newAlpha: Int): Int =
        (argb and 0x00FFFFFF) or ((newAlpha.coerceIn(0, 255)) shl 24)

    fun toHsv(argb: Int): FloatArray {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        val hue = when {
            delta == 0f -> 0f
            max == r -> 60f * (((g - b) / delta) % 6f)
            max == g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }
        val saturation = if (max == 0f) 0f else delta / max
        return floatArrayOf((hue + 360f) % 360f, saturation, max)
    }

    fun fromHsv(hue: Float, saturation: Float, value: Float, alpha: Int = 255): Int {
        val h = ((hue % 360f) + 360f) % 360f
        val s = saturation.coerceIn(0f, 1f)
        val v = value.coerceIn(0f, 1f)
        val chroma = v * s
        val x = chroma * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
        val m = v - chroma
        val (r, g, b) = when {
            h < 60f -> Triple(chroma, x, 0f)
            h < 120f -> Triple(x, chroma, 0f)
            h < 180f -> Triple(0f, chroma, x)
            h < 240f -> Triple(0f, x, chroma)
            h < 300f -> Triple(x, 0f, chroma)
            else -> Triple(chroma, 0f, x)
        }
        fun channel(component: Float): Int =
            Math.round((component + m) * 255f).coerceIn(0, 255)
        return (alpha.coerceIn(0, 255) shl 24) or
            (channel(r) shl 16) or
            (channel(g) shl 8) or
            channel(b)
    }

    /** sRGB relative luminance, WCAG definition. */
    fun luminance(argb: Int): Float {
        fun linear(component: Int): Float {
            val c = component / 255f
            return if (c <= 0.04045f) c / 12.92f else Math.pow(((c + 0.055) / 1.055).toDouble(), 2.4).toFloat()
        }
        return 0.2126f * linear((argb shr 16) and 0xFF) +
            0.7152f * linear((argb shr 8) and 0xFF) +
            0.0722f * linear(argb and 0xFF)
    }

    fun contrastRatio(a: Int, b: Int): Float {
        val la = luminance(a)
        val lb = luminance(b)
        val lighter = maxOf(la, lb)
        val darker = minOf(la, lb)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    fun readableOn(background: Int): Int =
        if (contrastRatio(0xFF15151E.toInt(), background) >=
            contrastRatio(0xFFFFFFFF.toInt(), background)
        ) {
            0xFF15151E.toInt()
        } else {
            0xFFFFFFFF.toInt()
        }

    /** Perceptually weighted RGB distance, useful for nearest-colour matching. */
    fun distance(a: Int, b: Int): Float {
        val dr = (((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)).toFloat()
        val dg = (((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)).toFloat()
        val db = ((a and 0xFF) - (b and 0xFF)).toFloat()
        return dr * dr * 0.3f + dg * dg * 0.59f + db * db * 0.11f
    }

    fun toHex(argb: Int): String =
        String.format("#%06X", argb and 0xFFFFFF)
}
