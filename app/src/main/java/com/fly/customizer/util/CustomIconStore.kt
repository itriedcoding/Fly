package com.fly.customizer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Copies a user-selected image into `files/custom_icons` and decodes it back at icon sizes.
 *
 * The mapping from app component to file lives in SQLite; these files are the real pixels.
 */
object CustomIconStore {

    private const val TAG = "CustomIconStore"
    private const val MAX_STORED = 512
    private const val FOLDER = "custom_icons"

    fun save(context: Context, component: String, uri: Uri): String? {
        return try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_STORED)
            }
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null

            val scaled = if (decoded.width > MAX_STORED || decoded.height > MAX_STORED) {
                val ratio = MAX_STORED.toFloat() / maxOf(decoded.width, decoded.height)
                val width = (decoded.width * ratio).toInt().coerceAtLeast(1)
                val height = (decoded.height * ratio).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(decoded, width, height, true)
            } else {
                decoded
            }

            val folder = File(context.filesDir, FOLDER).apply { mkdirs() }
            val target = File(folder, fileName(component))
            FileOutputStream(target).use { output ->
                if (!scaled.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    return null
                }
            }
            if (scaled !== decoded) decoded.recycle()
            scaled.recycle()
            target.absolutePath
        } catch (error: Exception) {
            Log.e(TAG, "Unable to store custom icon", error)
            null
        } catch (error: OutOfMemoryError) {
            Log.e(TAG, "Icon image too large", error)
            null
        }
    }

    fun load(path: String, maxSize: Int = 192): Bitmap? {
        return try {
            val file = File(path)
            if (!file.exists()) return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0) return null
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSize)
            }
            BitmapFactory.decodeFile(path, options)
        } catch (error: Exception) {
            Log.w(TAG, "Unable to load custom icon", error)
            null
        }
    }

    fun delete(path: String) {
        try {
            val file = File(path)
            if (file.exists() && !file.delete()) {
                Log.w(TAG, "Could not delete ${file.name}")
            }
        } catch (error: Exception) {
            Log.w(TAG, "Unable to delete custom icon", error)
        }
    }

    private fun sampleSize(width: Int, height: Int, target: Int): Int {
        var sample = 1
        var longest = maxOf(width, height)
        while (longest / 2 >= target) {
            longest /= 2
            sample *= 2
        }
        return sample
    }

    private fun fileName(component: String): String =
        String.format(Locale.US, "%08x.png", component.hashCode())
}
