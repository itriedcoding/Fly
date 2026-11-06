package com.fly.customizer.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log

/**
 * Samples the current system wallpaper and returns its dominant colour.
 */
object WallpaperAccent {

    private const val TAG = "WallpaperAccent"
    private const val SAMPLE_SIZE = 96

    fun dominantColor(context: Context): Int? {
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
            val bitmap = Bitmap.createBitmap(SAMPLE_SIZE, SAMPLE_SIZE, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, SAMPLE_SIZE, SAMPLE_SIZE)
            drawable.draw(canvas)
            val pixels = IntArray(SAMPLE_SIZE * SAMPLE_SIZE)
            bitmap.getPixels(pixels, 0, SAMPLE_SIZE, 0, 0, SAMPLE_SIZE, SAMPLE_SIZE)
            bitmap.recycle()
            NativeFly.dominantColors(pixels, 8).firstOrNull()
        } catch (error: Exception) {
            Log.w(TAG, "Unable to sample wallpaper", error)
            null
        } catch (error: OutOfMemoryError) {
            Log.w(TAG, "Wallpaper sample too large", error)
            null
        }
    }
}
