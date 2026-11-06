package com.fly.customizer.util

import android.app.Activity
import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.fly.customizer.core.Prefs
import com.fly.customizer.wallpaper.FlyWallpaperService

/**
 * Bridges the animated wallpaper catalogue with the system live wallpaper picker.
 *
 * The selection is stored in [Prefs] before the picker opens and the service reads it when it
 * starts, so the animated wallpaper can also be picked from the system wallpapers screen.
 */
object LiveWallpaperController {

    private const val TAG = "LiveWallpaper"

    enum class Result { LAUNCHED, CHOOSER, UNSUPPORTED, FAILED }

    fun component(context: Context): ComponentName =
        ComponentName(context.packageName, FlyWallpaperService::class.java.name)

    fun isSupported(context: Context): Boolean = try {
        WallpaperManager.getInstance(context).isWallpaperSupported
    } catch (error: Exception) {
        Log.w(TAG, "WallpaperManager unavailable", error)
        false
    }

    /** True when Fly's own live wallpaper is the active wallpaper. */
    fun isRunning(context: Context): Boolean {
        val info = try {
            WallpaperManager.getInstance(context).wallpaperInfo
        } catch (error: Exception) {
            Log.w(TAG, "Unable to read wallpaper info", error)
            null
        } ?: return false
        return info.packageName == context.packageName &&
            info.serviceName == FlyWallpaperService::class.java.name
    }

    /**
     * Opens the system preview for Fly's live wallpaper with [preset] pre-selected.
     *
     * The direct preview needs a launcher that handles ACTION_CHANGE_LIVE_WALLPAPER; when that is
     * missing we fall back to the generic live wallpaper picker.
     */
    fun apply(activity: Activity, preset: AnimatedWallpaper): Result {
        if (!isSupported(activity)) return Result.UNSUPPORTED
        Prefs(activity).liveWallpaperId = preset.id

        val direct = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component(activity))
        }
        return try {
            activity.startActivity(direct)
            Result.LAUNCHED
        } catch (error: ActivityNotFoundException) {
            Log.i(TAG, "Direct live wallpaper preview unavailable", error)
            try {
                activity.startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
                Result.CHOOSER
            } catch (error: ActivityNotFoundException) {
                Log.w(TAG, "No live wallpaper picker on this device", error)
                Result.FAILED
            }
        } catch (error: SecurityException) {
            Log.w(TAG, "Live wallpaper picker refused the request", error)
            Result.FAILED
        }
    }
}
