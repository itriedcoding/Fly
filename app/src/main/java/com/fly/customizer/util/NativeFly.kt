package com.fly.customizer.util

/**
 * JNI surface of the Fly native engine (`libflycore.so`, C++17 + C11).
 */
object NativeFly {

    init {
        System.loadLibrary("flycore")
    }

    /**
     * Fills [pixels] (width * height ARGB_8888 values) with one of the native gradient styles.
     *
     * Styles: 0 diagonal, 1 radial glow, 2 mesh, 3 structured bands.
     */
    external fun generateGradient(
        pixels: IntArray,
        width: Int,
        height: Int,
        palette: IntArray,
        style: Int,
        grain: Int,
    )

    /**
     * Fills [pixels] with one frame of an animated wallpaper.
     *
     * Styles: 0 flowing curtains, 1 pulsing rings, 2 orbit, 3 plasma, 4 comet sweep, 5 caustics.
     * [phase] is the position inside the animation cycle in turns (0..1).
     */
    external fun generateFrame(
        pixels: IntArray,
        width: Int,
        height: Int,
        palette: IntArray,
        style: Int,
        grain: Int,
        phase: Float,
    )

    /** Separable box blur blended with the original detail. */
    external fun boxBlur(pixels: IntArray, width: Int, height: Int, radius: Int)

    /** WCAG contrast ratio between two opaque colours. */
    external fun contrastRatio(foreground: Int, background: Int): Float

    /** WCAG relative luminance of an opaque colour. */
    external fun luminance(color: Int): Float

    /** Up to [maxColors] dominant colours of a pixel buffer, most frequent first. */
    external fun dominantColors(pixels: IntArray, maxColors: Int): IntArray

    /** Stable fingerprint of a pixel buffer, used to identify generated artwork. */
    external fun paletteHash(pixels: IntArray): Int

    external fun version(): String

    /** Human readable info about the loaded native library. */
    fun describe(): String = version()
}
