package com.fly.customizer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperPresetTest {

    @Test
    fun presetIdsAreUnique() {
        val ids = WallpaperStudio.presets.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun everyPresetHasUsablePalette() {
        for (preset in WallpaperStudio.presets) {
            assertTrue("${preset.id} needs at least two colours", preset.palette.size >= 2)
            assertTrue("${preset.id} style out of range", preset.style in 0..3)
            assertTrue("${preset.id} grain too strong", preset.grain in 0..24)
        }
    }

    @Test
    fun presetLookupWorks() {
        assertEquals("aurora", WallpaperStudio.preset("aurora")?.id)
        assertEquals(null, WallpaperStudio.preset("does-not-exist"))
    }
}
