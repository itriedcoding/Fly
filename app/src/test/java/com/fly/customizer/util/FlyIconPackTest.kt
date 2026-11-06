package com.fly.customizer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlyIconPackTest {

    @Test
    fun builtInPacksAreUnique() {
        val packs = FlyIconPacks.all
        assertTrue("expected at least 12 built-in icon packs", packs.size >= 12)
        assertEquals(packs.size, packs.map { it.id }.toSet().size)
        assertEquals(packs.size, packs.map { it.labelRes }.toSet().size)
    }

    @Test
    fun everyPackHasAUsablePalette() {
        for (pack in FlyIconPacks.all) {
            assertTrue("${pack.id} needs at least three colours", pack.palette.size >= 3)
            for (color in pack.palette) {
                assertEquals("${pack.id} colours must be opaque", 0xFF, (color ushr 24) and 0xFF)
            }
        }
    }

    @Test
    fun everyShapeAndPaletteIsRepresented() {
        val shapes = FlyIconPacks.all.map { it.shape }.toSet()
        assertEquals(IconShape.values().size, shapes.size)
        assertEquals(
            "duplicate pack palettes",
            FlyIconPacks.all.size,
            FlyIconPacks.all.map { it.palette.toList() }.toSet().size,
        )
    }

    @Test
    fun packLookupFallsBackToTheFirstPack() {
        assertEquals("aurora", FlyIconPacks.packOrDefault(null).id)
        assertEquals("aurora", FlyIconPacks.packOrDefault("missing").id)
        assertEquals("neon", FlyIconPacks.pack("neon")?.id)
    }
}
