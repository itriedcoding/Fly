package com.fly.customizer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorMathTest {

    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()

    @Test
    fun contrastRatioMatchesWcagExtremes() {
        assertEquals(21.0f, ColorMath.contrastRatio(white, black), 0.02f)
        assertEquals(1.0f, ColorMath.contrastRatio(white, white), 0.001f)
    }

    @Test
    fun luminanceIsOrdered() {
        assertTrue(ColorMath.luminance(white) > 0.99f)
        assertTrue(ColorMath.luminance(black) < 0.01f)
        assertTrue(ColorMath.luminance(0xFF808080.toInt()) in 0.05f..0.5f)
    }

    @Test
    fun shiftHueRotatesFullCircle() {
        val violet = 0xFF7C5CFF.toInt()
        val rotated = ColorMath.shiftHue(violet, 360f)
        assertEquals(ColorMath.toHex(violet), ColorMath.toHex(rotated))
        assertEquals(ColorMath.alpha(violet), ColorMath.alpha(rotated))
    }

    @Test
    fun shiftHueKeepsSaturationAndValue() {
        val original = 0xFF16B364.toInt()
        val originalHsv = ColorMath.toHsv(original)
        val shifted = ColorMath.toHsv(ColorMath.shiftHue(original, 90f))

        assertEquals(originalHsv[1], shifted[1], 0.01f)
        assertEquals(originalHsv[2], shifted[2], 0.01f)
    }

    @Test
    fun hueWrapHandlesNegativeDegrees() {
        val start = 0xFF3366CC.toInt()
        val hsvStart = ColorMath.toHsv(start)[0]
        val hsvShifted = ColorMath.toHsv(ColorMath.shiftHue(start, -350f))[0]
        val expected = ((hsvStart - 350f) % 360f + 360f) % 360f
        assertEquals(expected, hsvShifted, 0.5f)
    }

    @Test
    fun hsvRoundTripPreservesColour() {
        val colors = listOf(
            0xFF7C5CFF.toInt(),
            0xFF29C7E0.toInt(),
            0xFFFF7A3C.toInt(),
            0xFF000000.toInt(),
            0xFFFFFFFF.toInt(),
        )
        for (color in colors) {
            val hsv = ColorMath.toHsv(color)
            assertEquals(color, ColorMath.fromHsv(hsv[0], hsv[1], hsv[2]))
        }
    }

    @Test
    fun readableOnChoosesDarkTextForLightBackground() {
        assertEquals(0xFF15151E.toInt(), ColorMath.readableOn(0xFFFFFFFF.toInt()))
        assertEquals(white, ColorMath.readableOn(0xFF101024.toInt()))
    }

    @Test
    fun withAlphaReplacesOnlyAlpha() {
        val result = ColorMath.withAlpha(0xFF112233.toInt(), 0x40)
        assertEquals(0x40112233, result)
    }

    @Test
    fun toHexFormatsSixDigits() {
        assertEquals("#7C5CFF", ColorMath.toHex(0xFF7C5CFF.toInt()))
        assertEquals("#000000", ColorMath.toHex(black))
    }
}
