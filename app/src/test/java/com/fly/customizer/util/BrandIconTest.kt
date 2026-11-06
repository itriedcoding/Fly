package com.fly.customizer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrandIconTest {

    @Test
    fun catalogCoversTheRequestedApps() {
        val brands = BrandIcons.all
        assertEquals("expected twelve brand icons", 12, brands.size)
        assertEquals(brands.size, brands.map { it.id }.toSet().size)
        assertEquals(brands.size, brands.map { it.labelRes }.toSet().size)
    }

    @Test
    fun everyPackageMapsToExactlyOneBrand() {
        val packages = BrandIcons.packages
        assertTrue("expected at least twelve packages", packages.size >= 12)
        assertEquals("duplicate package mapping", packages.size, packages.toSet().size)
        for (packageName in packages) {
            assertTrue("$packageName is not a package name", packageName.contains('.'))
            assertEquals(packageName.lowercase(), packageName)
            assertTrue("$packageName has no brand", BrandIcons.find(packageName) != null)
        }
        assertNull(BrandIcons.find("com.example.notabrand"))
        assertNull(BrandIcons.find(null))
    }

    @Test
    fun everyBrandHasOpaqueColoursAndADistinctGlyph() {
        for (brand in BrandIcons.all) {
            assertEquals("${brand.id} primary", 0xFF, (brand.primary ushr 24) and 0xFF)
            assertEquals("${brand.id} secondary", 0xFF, (brand.secondary ushr 24) and 0xFF)
            assertTrue("${brand.id} has no package", brand.packages.isNotEmpty())
        }
        val glyphs = BrandIcons.all.map { it.glyph }.toSet()
        assertEquals("every glyph should be used once", BrandGlyph.values().size, glyphs.size)
    }

    @Test
    fun brandPacksUseEachTreatment() {
        val packs = FlyIconPacks.brandPacks
        assertTrue("expected four brand packs", packs.size >= 4)
        assertEquals(
            "duplicate brand treatments",
            packs.size,
            packs.map { it.treatment }.toSet().size,
        )
        assertTrue("brand packs must be brand aware", packs.all { it.brandAware })
        assertEquals(
            "brand pack ids must be unique",
            packs.size,
            packs.map { it.id }.toSet().size,
        )
    }
}
