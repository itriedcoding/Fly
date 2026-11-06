package com.fly.customizer.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.DisplayMetrics

/**
 * Static device facts shown on the dashboard.
 */
data class DeviceSnapshot(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdkLevel: Int,
    val resolution: String,
    val densityBucket: String,
    val densityDpi: Int,
    val launcherLabel: String,
    val launcherPackage: String?,
    val launcherIcon: Drawable?,
    val launchableApps: Int,
    val installedIconPacks: Int,
    val widgetProviders: Int,
)

object DeviceInfo {

    fun snapshot(context: Context): DeviceSnapshot {
        val metrics = context.resources.displayMetrics
        val launcher = defaultLauncher(context)
        val iconPackLoader = IconPackLoader(context)
        val apps = iconPackLoader.launchableApps()
        return DeviceSnapshot(
            manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            model = Build.MODEL,
            androidVersion = "Android ${Build.VERSION.RELEASE}",
            sdkLevel = Build.VERSION.SDK_INT,
            resolution = "${metrics.widthPixels} x ${metrics.heightPixels}",
            densityBucket = densityBucket(metrics),
            densityDpi = metrics.densityDpi,
            launcherLabel = launcher.first,
            launcherPackage = launcher.second,
            launcherIcon = launcher.third,
            launchableApps = apps.size,
            installedIconPacks = iconPackLoader.installedPacks(apps).size,
            widgetProviders = installedWidgetCount(context),
        )
    }

    fun defaultLauncher(context: Context): Triple<String, String?, Drawable?> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved: ComponentName? = try {
            intent.resolveActivity(context.packageManager)
        } catch (error: Exception) {
            null
        }
        val packageName: String = resolved?.packageName ?: return Triple("Unknown", null, null)
        val label = try {
            val applicationInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (error: PackageManager.NameNotFoundException) {
            packageName
        }
        val icon = try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (error: PackageManager.NameNotFoundException) {
            null
        }
        return Triple(label, packageName, icon)
    }

    fun installedWidgetCount(context: Context): Int = try {
        val manager = android.appwidget.AppWidgetManager.getInstance(context)
        manager.installedProviders.size
    } catch (error: Exception) {
        0
    }

    fun densityBucket(metrics: DisplayMetrics): String = when {
        metrics.densityDpi <= DisplayMetrics.DENSITY_MEDIUM -> "mdpi"
        metrics.densityDpi <= DisplayMetrics.DENSITY_HIGH -> "hdpi"
        metrics.densityDpi <= DisplayMetrics.DENSITY_XHIGH -> "xhdpi"
        metrics.densityDpi <= DisplayMetrics.DENSITY_420 -> "xxhdpi"
        else -> "xxxhdpi"
    }
}
