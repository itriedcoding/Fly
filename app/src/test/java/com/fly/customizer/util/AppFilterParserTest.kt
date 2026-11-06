package com.fly.customizer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppFilterParserTest {

    @Test
    fun parsesComponentInfoEntries() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <item component="ComponentInfo{com.android.chrome/com.google.android.apps.chrome.Main}" drawable="chrome_icon"/>
                <item component="ComponentInfo{com.spotify.music/com.spotify.music.MainActivity}" drawable="spotify_icon"/>
            </resources>
        """.trimIndent()

        val parsed = AppFilterParser.parse(xml)

        assertEquals(2, parsed.size)
        assertEquals("chrome_icon", parsed["com.android.chrome/com.google.android.apps.chrome.Main"])
        assertEquals("spotify_icon", parsed["com.spotify.music/com.spotify.music.MainActivity"])
    }

    @Test
    fun expandsRelativeClassNames() {
        val xml = """<item component="ComponentInfo{com.example.app/.MainActivity}" drawable="example"/>"""

        val parsed = AppFilterParser.parse(xml)

        assertEquals("example", parsed["com.example.app/com.example.app.MainActivity"])
    }

    @Test
    fun normalisesPlainComponentStrings() {
        assertEquals(
            "com.example/com.example.Main",
            AppFilterParser.normaliseComponent("com.example/com.example.Main"),
        )
        assertEquals(
            "com.example/com.example.Main",
            AppFilterParser.normaliseComponent("  ComponentInfo{com.example/.Main}  "),
        )
        assertNull(AppFilterParser.normaliseComponent("not-a-component"))
        assertNull(AppFilterParser.normaliseComponent("com.example/"))
        assertNull(AppFilterParser.normaliseComponent(""))
    }

    @Test
    fun ignoresMalformedItems() {
        val xml = """
            <resources>
                <item component="ComponentInfo{com.good/com.good.Main}" drawable="good"/>
                <item drawable="orphan"/>
                <item component="ComponentInfo{com.bad}" drawable="bad"/>
                <item component="ComponentInfo{com.other/com.other.Main}"/>
            </resources>
        """.trimIndent()

        val parsed = AppFilterParser.parse(xml)

        assertEquals(1, parsed.size)
        assertTrue(parsed.containsKey("com.good/com.good.Main"))
    }

    @Test
    fun handlesEmptyInput() {
        assertTrue(AppFilterParser.parse("").isEmpty())
        assertTrue(AppFilterParser.parse("   ").isEmpty())
    }

    @Test
    fun readsAttributesInAnyOrder() {
        val xml = """<item drawable="reverse" component="ComponentInfo{com.order/com.order.Main}"/>"""

        assertEquals("reverse", AppFilterParser.parse(xml)["com.order/com.order.Main"])
    }
}
