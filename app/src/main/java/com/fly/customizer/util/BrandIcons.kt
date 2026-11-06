package com.fly.customizer.util

import com.fly.customizer.R

/**
 * Simplified, original glyph used for a well-known app. These are stylistic reinterpretations
 * drawn with Canvas, not the official brand artwork.
 */
enum class BrandGlyph { NOTE, PLAY, FACE, GHOST, CROSS, DOLLAR, BLOCK, WAVES, RING, PLANE, ENVELOPE, EFF }

/**
 * A stylised icon for one well-known app, matched by its real package name.
 */
data class BrandIcon(
    val id: String,
    val labelRes: Int,
    val packages: List<String>,
    val primary: Int,
    val secondary: Int,
    val glyph: BrandGlyph,
) {
    override fun equals(other: Any?): Boolean = other is BrandIcon && other.id == id
    override fun hashCode(): Int = id.hashCode()
}

/**
 * Package-name lookup for the brand-aware icon packs.
 *
 * Only packages that actually exist are listed; the icon renderer falls back to a monogram for
 * every other app, so nothing is invented at runtime.
 */
object BrandIcons {

    val all: List<BrandIcon> = listOf(
        BrandIcon(
            id = "tiktok",
            labelRes = R.string.brand_tiktok,
            packages = listOf(
                "com.zhiliaoapp.musically",
                "com.ss.android.ugc.trill",
                "com.zhiliaoapp.musically.go",
            ),
            primary = 0xFF010101.toInt(),
            secondary = 0xFF25F4EE.toInt(),
            glyph = BrandGlyph.NOTE,
        ),
        BrandIcon(
            id = "youtube",
            labelRes = R.string.brand_youtube,
            packages = listOf("com.google.android.youtube"),
            primary = 0xFFCC0000.toInt(),
            secondary = 0xFFFF4444.toInt(),
            glyph = BrandGlyph.PLAY,
        ),
        BrandIcon(
            id = "discord",
            labelRes = R.string.brand_discord,
            packages = listOf("com.discord"),
            primary = 0xFF5865F2.toInt(),
            secondary = 0xFF9AA4FF.toInt(),
            glyph = BrandGlyph.FACE,
        ),
        BrandIcon(
            id = "snapchat",
            labelRes = R.string.brand_snapchat,
            packages = listOf("com.snapchat.android"),
            primary = 0xFFFFD400.toInt(),
            secondary = 0xFFFFF59D.toInt(),
            glyph = BrandGlyph.GHOST,
        ),
        BrandIcon(
            id = "x",
            labelRes = R.string.brand_x,
            packages = listOf("com.twitter.android", "com.x.android"),
            primary = 0xFF111111.toInt(),
            secondary = 0xFF5A5A5A.toInt(),
            glyph = BrandGlyph.CROSS,
        ),
        BrandIcon(
            id = "cashapp",
            labelRes = R.string.brand_cashapp,
            packages = listOf("com.squareup.cash"),
            primary = 0xFF00B33C.toInt(),
            secondary = 0xFF6DFFA0.toInt(),
            glyph = BrandGlyph.DOLLAR,
        ),
        BrandIcon(
            id = "roblox",
            labelRes = R.string.brand_roblox,
            packages = listOf("com.roblox.client"),
            primary = 0xFFD11F1F.toInt(),
            secondary = 0xFFFF7A7A.toInt(),
            glyph = BrandGlyph.BLOCK,
        ),
        BrandIcon(
            id = "spotify",
            labelRes = R.string.brand_spotify,
            packages = listOf("com.spotify.music"),
            primary = 0xFF1DB954.toInt(),
            secondary = 0xFF7BF2A6.toInt(),
            glyph = BrandGlyph.WAVES,
        ),
        BrandIcon(
            id = "chime",
            labelRes = R.string.brand_chime,
            packages = listOf("com.chime.chime"),
            primary = 0xFF12B76A.toInt(),
            secondary = 0xFF8DF0BE.toInt(),
            glyph = BrandGlyph.RING,
        ),
        BrandIcon(
            id = "telegram",
            labelRes = R.string.brand_telegram,
            packages = listOf("org.telegram.messenger", "org.thunderdog.challegram"),
            primary = 0xFF229ED9.toInt(),
            secondary = 0xFF9BDCF5.toInt(),
            glyph = BrandGlyph.PLANE,
        ),
        BrandIcon(
            id = "gmail",
            labelRes = R.string.brand_gmail,
            packages = listOf("com.google.android.gm"),
            primary = 0xFFD93A2B.toInt(),
            secondary = 0xFFFF8A7A.toInt(),
            glyph = BrandGlyph.ENVELOPE,
        ),
        BrandIcon(
            id = "facebook",
            labelRes = R.string.brand_facebook,
            packages = listOf("com.facebook.katana", "com.facebook.lite"),
            primary = 0xFF1877F2.toInt(),
            secondary = 0xFF8AB8FF.toInt(),
            glyph = BrandGlyph.EFF,
        ),
    )

    private val byPackage: Map<String, BrandIcon> = buildMap {
        for (brand in all) {
            for (packageName in brand.packages) {
                put(packageName, brand)
            }
        }
    }

    fun find(packageName: String?): BrandIcon? = packageName?.let { byPackage[it] }

    fun brand(id: String?): BrandIcon? = all.firstOrNull { it.id == id }

    /** Every declared package name, used by the catalogue test. */
    val packages: List<String> get() = all.flatMap { it.packages }
}
