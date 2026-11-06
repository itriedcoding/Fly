package com.fly.customizer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimatedWallpaperTest {

    @Test
    fun animatedIdsAndLabelsAreUnique() {
        val presets = AnimatedWallpaperCatalog.presets
        assertTrue("expected at least 6 animated wallpapers", presets.size >= 6)
        assertEquals(presets.size, presets.map { it.id }.toSet().size)
        assertEquals(presets.size, presets.map { it.labelRes }.toSet().size)
    }

    @Test
    fun everyAnimatedPresetIsRenderable() {
        for (preset in AnimatedWallpaperCatalog.presets) {
            assertTrue("${preset.id} palette too short", preset.palette.size >= 2)
            assertTrue(
                "${preset.id} style out of range",
                preset.style in 0 until AnimatedWallpaperCatalog.styleCount,
            )
            assertTrue("${preset.id} grain out of range", preset.grain in 0..24)
            assertTrue(
                "${preset.id} speed must be a slow drift",
                preset.speed in 0.01f..0.5f,
            )
        }
    }

    @Test
    fun everyAnimatedStyleIsUsed() {
        val used = AnimatedWallpaperCatalog.presets.map { it.style }.toSet()
        for (style in 0 until AnimatedWallpaperCatalog.styleCount) {
            assertTrue("animated style $style has no preset", used.contains(style))
        }
    }

    @Test
    fun lookupFallsBackToTheFirstPreset() {
        assertEquals(
            AnimatedWallpaperCatalog.presets.first().id,
            AnimatedWallpaperCatalog.presetOrDefault("missing").id,
        )
        assertEquals(
            AnimatedWallpaperCatalog.presets.first().id,
            AnimatedWallpaperCatalog.presetOrDefault(null).id,
        )
    }
}
