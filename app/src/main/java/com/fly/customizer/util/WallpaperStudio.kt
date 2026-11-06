package com.fly.customizer.util

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.fly.customizer.R
import com.fly.customizer.core.Prefs
import kotlin.math.max
import kotlin.random.Random

/**
 * A generative wallpaper recipe: palette, gradient style and grain.
 */
data class WallpaperPreset(
    val id: String,
    val labelRes: Int,
    val palette: IntArray,
    val style: Int,
    val grain: Int,
    val blurRadius: Int = 0,
) {
    override fun equals(other: Any?): Boolean = other is WallpaperPreset && other.id == id
    override fun hashCode(): Int = id.hashCode()
}

/**
 * Renders and applies wallpapers with the native engine.
 */
object WallpaperStudio {

    private const val TAG = "WallpaperStudio"

    val presets: List<WallpaperPreset> = listOf(
        WallpaperPreset(
            id = "aurora",
            labelRes = R.string.wallpaper_aurora,
            palette = intArrayOf(0xFF241752.toInt(), 0xFF6D3BF5.toInt(), 0xFF29C7E0.toInt()),
            style = 0,
            grain = 4,
        ),
        WallpaperPreset(
            id = "ember",
            labelRes = R.string.wallpaper_ember,
            palette = intArrayOf(0xFF1A0B10.toInt(), 0xFF7A1F34.toInt(), 0xFFFF7A3C.toInt(), 0xFFFFD9A0.toInt()),
            style = 1,
            grain = 3,
        ),
        WallpaperPreset(
            id = "mint",
            labelRes = R.string.wallpaper_mint,
            palette = intArrayOf(0xFF04211C.toInt(), 0xFF0E5A47.toInt(), 0xFF3DDC97.toInt(), 0xFFDFFCEF.toInt()),
            style = 0,
            grain = 2,
        ),
        WallpaperPreset(
            id = "midnight",
            labelRes = R.string.wallpaper_midnight,
            palette = intArrayOf(0xFF05060F.toInt(), 0xFF101A38.toInt(), 0xFF2C4FA8.toInt(), 0xFF6E8BFF.toInt()),
            style = 1,
            grain = 3,
        ),
        WallpaperPreset(
            id = "rosewater",
            labelRes = R.string.wallpaper_rosewater,
            palette = intArrayOf(0xFF2A0A1B.toInt(), 0xFF8A1F52.toInt(), 0xFFFF7BA8.toInt(), 0xFFFFE1EC.toInt()),
            style = 3,
            grain = 2,
        ),
        WallpaperPreset(
            id = "solar",
            labelRes = R.string.wallpaper_solar,
            palette = intArrayOf(0xFF241400.toInt(), 0xFF7A4400.toInt(), 0xFFFFB020.toInt(), 0xFFFFE9B8.toInt()),
            style = 0,
            grain = 4,
            blurRadius = 3,
        ),
        WallpaperPreset(
            id = "glacier",
            labelRes = R.string.wallpaper_glacier,
            palette = intArrayOf(0xFF061318.toInt(), 0xFF12405A.toInt(), 0xFF5FC7E8.toInt(), 0xFFE8FBFF.toInt()),
            style = 2,
            grain = 2,
        ),
        WallpaperPreset(
            id = "void",
            labelRes = R.string.wallpaper_void,
            palette = intArrayOf(0xFF000000.toInt(), 0xFF12121C.toInt(), 0xFF2A2A3E.toInt(), 0xFF4B4B66.toInt()),
            style = 3,
            grain = 2,
        ),
    )

    fun preset(id: String): WallpaperPreset? = presets.firstOrNull { it.id == id }

    /** Renders the preset at the requested size with the native gradient engine. */
    fun render(preset: WallpaperPreset, width: Int, height: Int, hueShift: Float): Bitmap {
        val safeWidth = max(1, width)
        val safeHeight = max(1, height)
        val pixels = IntArray(safeWidth * safeHeight)
        NativeFly.generateGradient(pixels, safeWidth, safeHeight, preset.palette, preset.style, preset.grain)
        if (preset.blurRadius > 0) {
            NativeFly.boxBlur(pixels, safeWidth, safeHeight, preset.blurRadius)
        }
        if (hueShift != 0f) {
            for (index in pixels.indices) {
                pixels[index] = ColorMath.shiftHue(pixels[index], hueShift)
            }
        }
        return Bitmap.createBitmap(pixels, safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
    }

    /** Full resolution size for the current display, capped to keep memory use sane. */
    fun suggestedSize(context: Context): Pair<Int, Int> {
        val metrics = context.resources.displayMetrics
        val targetWidth = metrics.widthPixels.coerceIn(720, 1440)
        val ratio = metrics.heightPixels.toFloat() / metrics.widthPixels.toFloat()
        val targetHeight = (targetWidth * ratio).toInt().coerceAtLeast(targetWidth)
        return targetWidth to targetHeight
    }

    fun targetFlags(target: Int): Int = when (target) {
        Prefs.TARGET_HOME -> WallpaperManager.FLAG_SYSTEM
        Prefs.TARGET_LOCK -> WallpaperManager.FLAG_LOCK
        else -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
    }

    fun targetLabelRes(target: Int): Int = when (target) {
        Prefs.TARGET_HOME -> R.string.target_home
        Prefs.TARGET_LOCK -> R.string.target_lock
        else -> R.string.target_both
    }

    /** Writes the bitmap to the system wallpaper. Returns true when Android accepted it. */
    fun applyToSystem(context: Context, bitmap: Bitmap, target: Int): Boolean {
        return try {
            val manager = WallpaperManager.getInstance(context)
            if (!manager.isWallpaperSupported) {
                Log.w(TAG, "Wallpapers are not supported on this device")
                return false
            }
            manager.setBitmap(bitmap, null, true, targetFlags(target))
            true
        } catch (error: Exception) {
            Log.e(TAG, "Failed to apply wallpaper", error)
            false
        } catch (error: OutOfMemoryError) {
            Log.e(TAG, "Wallpaper bitmap too large", error)
            false
        }
    }

    /** Result of trying to write a wallpaper into the gallery. */
    enum class SaveResult { SAVED, UNSUPPORTED, FAILED }

    /** Writes the bitmap to `Pictures/Fly` through MediaStore (Android 10+). */
    fun saveToGallery(context: Context, bitmap: Bitmap, baseName: String): SaveResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return SaveResult.UNSUPPORTED
        }
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "$baseName-${System.currentTimeMillis()}.png",
            )
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/Fly",
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = try {
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        } catch (error: Exception) {
            Log.e(TAG, "Unable to create gallery entry", error)
            null
        } ?: return SaveResult.FAILED

        return try {
            val stream = resolver.openOutputStream(uri)
                ?: return SaveResult.FAILED
            stream.use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    return SaveResult.FAILED
                }
            }
            val publish = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            resolver.update(uri, publish, null, null)
            SaveResult.SAVED
        } catch (error: Exception) {
            Log.e(TAG, "Unable to write wallpaper to gallery", error)
            try {
                resolver.delete(uri, null, null)
            } catch (ignored: Exception) {
                // Nothing else to clean up.
            }
            SaveResult.FAILED
        }
    }

    /** A random preset, optionally avoiding one the caller just applied. */
    fun randomPreset(excludeId: String? = null): WallpaperPreset {
        val candidates = presets.filter { it.id != excludeId }.ifEmpty { presets }
        return candidates[Random.nextInt(candidates.size)]
    }

    fun isLockScreenSupported(context: Context): Boolean {
        return try {
            WallpaperManager.getInstance(context).isSetWallpaperAllowed
        } catch (error: Exception) {
            false
        }
    }
}

/**
 * Small in-memory cache for generated thumbnails.
 */
object ThumbnailCache {

    private val cache = object : android.util.LruCache<String, Bitmap>(24) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    fun key(presetId: String, hueShift: Float, width: Int, height: Int): String =
        "$presetId:${hueShift.toInt()}:$width:$height"

    fun get(key: String): Bitmap? = cache.get(key)

    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }

    fun clear() = cache.evictAll()
}
