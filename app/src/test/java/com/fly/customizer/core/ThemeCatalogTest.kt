package com.fly.customizer.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeCatalogTest {

    @Test
    fun stockThemesAreUniqueAndComplete() {
        val themes = ThemeCatalog.stockThemes
        assertTrue("expected at least 24 stock themes", themes.size >= 24)
        assertEquals("duplicate theme ids", themes.size, themes.map { it.id }.toSet().size)
        assertEquals("duplicate labels", themes.size, themes.map { it.labelRes }.toSet().size)
        for (theme in themes) {
            assertTrue("unknown accent ${theme.accentId}", ThemeCatalog.accents.any { it.id == theme.accentId })
            assertTrue("missing label for ${theme.id}", theme.labelRes != 0)
        }
    }

    @Test
    fun everyBaseModeHasThemes() {
        for (mode in BaseMode.values()) {
            assertTrue("no theme for ${mode.id}", ThemeCatalog.stockThemes.any { it.baseMode == mode })
        }
    }

    @Test
    fun accentsAreUniqueAndResolvable() {
        val accents = ThemeCatalog.accents
        assertTrue("expected at least 12 accents", accents.size >= 12)
        assertEquals(accents.size, accents.map { it.id }.toSet().size)
        assertEquals(accents.size, accents.map { it.overlayRes }.toSet().size)
        for (accent in accents) {
            assertEquals(accent, ThemeCatalog.accent(accent.id))
            assertTrue(accent.labelRes != 0)
        }
        assertEquals(accents.first(), ThemeCatalog.accent("does-not-exist"))
    }

    @Test
    fun nearestAccentPicksTheExactSwatch() {
        val emerald = ThemeCatalog.accents.first { it.id == "emerald" }
        assertEquals(emerald, ThemeCatalog.nearestAccent(emerald.color))
    }
}
