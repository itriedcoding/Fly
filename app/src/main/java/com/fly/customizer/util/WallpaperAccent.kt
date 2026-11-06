package com.fly.customizer.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log

/**
 * Samples the current system wallpaper: dominant colour for accent matching and a small
 * preview bitmap for the dashboard.
 */
object WallpaperAccent {

    private const val TAG = "WallpaperAccent"
    private const val SAMPLE_SIZE = 96

    /** A rendered preview plus the colours sampled from the real wallpaper. */
    data class Preview(val bitmap: Bitmap, val colors: IntArray)

    fun dominantColor(context: Context): Int? = drawWallpaper(context, SAMPLE_SIZE, SAMPLE_SIZE)
        ?.let { bitmap ->
            val pixels = IntArray(SAMPLE_SIZE * SAMPLE_SIZE)
            bitmap.getPixels(pixels, 0, SAMPLE_SIZE, 0, 0, SAMPLE_SIZE, SAMPLE_SIZE)
            bitmap.recycle()
            NativeFly.dominantColors(pixels, 8).firstOrNull()
        }

    /** Wallpaper thumbnail (for the dashboard) and its dominant colours. */
    fun preview(context: Context, width: Int = 360, height: Int = 480): Preview? {
        val bitmap = drawWallpaper(context, width, height) ?: return null
        val sampleWidth = 64
        val sampleHeight = 64
        val sample = try {
            Bitmap.createScaledBitmap(bitmap, sampleWidth, sampleHeight, true)
        } catch (error: Exception) {
            bitmap.recycle()
            return null
        }
        val pixels = IntArray(sampleWidth * sampleHeight)
        sample.getPixels(pixels, 0, sampleWidth, 0, 0, sampleWidth, sampleHeight)
        sample.recycle()
        val colors = NativeFly.dominantColors(pixels, 5)
        return Preview(bitmap, colors)
    }

    private fun drawWallpaper(context: Context, width: Int, height: Int): Bitmap? {
        val manager = try {
            WallpaperManager.getInstance(context)
        } catch (error: Exception) {
            Log.w(TAG, "WallpaperManager unavailable", error)
            return null
        }
        val drawable = try {
            manager.drawable
        } catch (error: Exception) {
            Log.w(TAG, "Unable to read wallpaper drawable", error)
            null
        } ?: return null

        return try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
            bitmap
        } catch (error: Exception) {
            Log.w(TAG, "Unable to render wallpaper preview", error)
            null
        } catch (error: OutOfMemoryError) {
            Log.w(TAG, "Wallpaper preview too large", error)
            null
        }
    }
}
