package com.fly.customizer.util

import android.graphics.Bitmap
import com.fly.customizer.R
import kotlin.math.max

/**
 * A live wallpaper recipe: palette and one of the native time-based styles.
 *
 * [speed] is how many full animation cycles run per second, so 0.05 means one cycle every
 * twenty seconds.
 */
data class AnimatedWallpaper(
    val id: String,
    val labelRes: Int,
    val palette: IntArray,
    val style: Int,
    val grain: Int,
    val speed: Float,
) {
    override fun equals(other: Any?): Boolean = other is AnimatedWallpaper && other.id == id
    override fun hashCode(): Int = id.hashCode()
}

/**
 * Animated wallpapers rendered frame by frame in libflycore.so and shown by
 * [com.fly.customizer.wallpaper.FlyWallpaperService].
 */
object AnimatedWallpaperCatalog {

    val presets: List<AnimatedWallpaper> = listOf(
        AnimatedWallpaper(
            id = "drift_aurora",
            labelRes = R.string.live_drift_aurora,
            palette = intArrayOf(0xFF0B1026.toInt(), 0xFF1F2E6E.toInt(), 0xFF4C7BFF.toInt(), 0xFF48E0C8.toInt()),
            style = 0,
            grain = 2,
            speed = 0.05f,
        ),
        AnimatedWallpaper(
            id = "pulse_rings",
            labelRes = R.string.live_pulse_rings,
            palette = intArrayOf(0xFF07030F.toInt(), 0xFF3A1060.toInt(), 0xFF9C4DFF.toInt(), 0xFFFF7AD9.toInt()),
            style = 1,
            grain = 3,
            speed = 0.08f,
        ),
        AnimatedWallpaper(
            id = "orbit",
            labelRes = R.string.live_orbit,
            palette = intArrayOf(0xFF01060F.toInt(), 0xFF0B3D5C.toInt(), 0xFF23B5D3.toInt(), 0xFFE8FBFF.toInt()),
            style = 2,
            grain = 2,
            speed = 0.06f,
        ),
        AnimatedWallpaper(
            id = "plasma_tide",
            labelRes = R.string.live_plasma_tide,
            palette = intArrayOf(0xFF12021F.toInt(), 0xFF5B0F6E.toInt(), 0xFFE0407A.toInt(), 0xFFFFC46B.toInt()),
            style = 3,
            grain = 3,
            speed = 0.10f,
        ),
        AnimatedWallpaper(
            id = "comet_sweep",
            labelRes = R.string.live_comet_sweep,
            palette = intArrayOf(0xFF02040A.toInt(), 0xFF10265C.toInt(), 0xFF5A8CFF.toInt(), 0xFFFFFFFF.toInt()),
            style = 4,
            grain = 2,
            speed = 0.12f,
        ),
        AnimatedWallpaper(
            id = "caustics",
            labelRes = R.string.live_caustics,
            palette = intArrayOf(0xFF01100F.toInt(), 0xFF0A5C52.toInt(), 0xFF3FE0B0.toInt(), 0xFFD9FFF6.toInt()),
            style = 5,
            grain = 3,
            speed = 0.07f,
        ),
    )

    /** Number of animated styles implemented by the native engine. */
    val styleCount: Int get() = 6

    fun preset(id: String?): AnimatedWallpaper? = presets.firstOrNull { it.id == id }

    fun presetOrDefault(id: String?): AnimatedWallpaper = preset(id) ?: presets.first()

    /** Fills [pixels] with one animation frame at [phase] (0..1 per cycle). */
    fun renderFrame(
        preset: AnimatedWallpaper,
        pixels: IntArray,
        width: Int,
        height: Int,
        phase: Float,
    ) {
        NativeFly.generateFrame(
            pixels,
            width,
            height,
            preset.palette,
            preset.style,
            preset.grain,
            phase,
        )
    }

    /** Standalone bitmap of one frame, used for documentation and thumbnails. */
    fun renderBitmap(preset: AnimatedWallpaper, width: Int, height: Int, phase: Float): Bitmap {
        val safeWidth = max(1, width)
        val safeHeight = max(1, height)
        val pixels = IntArray(safeWidth * safeHeight)
        renderFrame(preset, pixels, safeWidth, safeHeight, phase)
        return Bitmap.createBitmap(pixels, safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
    }

    fun randomPreset(excludeId: String? = null): AnimatedWallpaper {
        val candidates = presets.filter { it.id != excludeId }.ifEmpty { presets }
        return candidates[randomIndex(candidates.size)]
    }

    private fun randomIndex(size: Int): Int = (Math.random() * size).toInt().coerceIn(0, size - 1)
}
