package com.fly.customizer.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.drawable.Drawable
import android.util.Log
import org.xmlpull.v1.XmlPullParser

/**
 * Reads icon packs that are installed on the device, exactly the way launchers do:
 * through their advertised theme intents and their `appfilter.xml` mapping.
 */
class IconPackLoader(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val resourceCache = HashMap<String, Resources?>()

    data class IconPack(
        val packageName: String,
        val label: String,
        val icon: Drawable?,
        val themedAppCount: Int,
        val totalAppCount: Int,
    )

    data class AppEntry(
        val label: String,
        val packageName: String,
        val className: String,
        val component: String,
        val icon: Drawable?,
        val themedIcon: Drawable?,
    )

    /** Icon packs declare themselves with one of these intent actions. */
    private val themeActions = listOf(
        "org.adw.launcher.THEMES",
        "com.gau.go.launcherex.theme",
        "com.novalauncher.THEME",
        "com.anddoes.launcher.THEME",
        "com.teslacoilsw.launcher.THEME",
        "ch.deletescape.lawnchair.ICONPACK",
        "app.lawnchair.ICONPACK",
    )

    fun launchableApps(): List<AppEntry> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = try {
            packageManager.queryIntentActivities(intent, 0)
        } catch (error: Exception) {
            Log.e(TAG, "Unable to query launcher activities", error)
            return emptyList()
        }
        return resolved
            .asSequence()
            .filter { it.activityInfo != null && it.activityInfo.packageName != context.packageName }
            .map { info ->
                val activity = info.activityInfo
                val label = info.loadLabel(packageManager).toString()
                AppEntry(
                    label = label,
                    packageName = activity.packageName,
                    className = activity.name,
                    component = AppFilterParser.keyOf(activity.packageName, activity.name),
                    icon = info.loadIcon(packageManager),
                    themedIcon = null,
                )
            }
            .distinctBy { it.component }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    /** Icon packs installed on the device, with coverage for the current app list. */
    fun installedPacks(appEntries: List<AppEntry>? = null): List<IconPack> {
        val packages = LinkedHashSet<String>()
        for (action in themeActions) {
            val intent = Intent(action).addCategory(Intent.CATEGORY_DEFAULT)
            val found = try {
                packageManager.queryIntentActivities(intent, 0)
            } catch (error: Exception) {
                emptyList()
            }
            found.forEach { packages.add(it.activityInfo.packageName) }
        }

        val apps = appEntries ?: launchableApps()
        val packs = ArrayList<IconPack>(packages.size)
        for (packageName in packages) {
            val appFilter = loadAppFilter(packageName)
            val label = try {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
            } catch (error: PackageManager.NameNotFoundException) {
                packageName
            }
            val icon = try {
                packageManager.getApplicationIcon(packageName)
            } catch (error: PackageManager.NameNotFoundException) {
                null
            }
            val themed = apps.count { appFilter.containsKey(it.component) }
            packs.add(
                IconPack(
                    packageName = packageName,
                    label = label,
                    icon = icon,
                    themedAppCount = themed,
                    totalAppCount = apps.size,
                )
            )
        }
        return packs.sortedWith(compareByDescending<IconPack> { it.themedAppCount }.thenBy { it.label.lowercase() })
    }

    /** Parses the pack's `appfilter.xml`, from either a resource or an asset. */
    fun loadAppFilter(packageName: String): Map<String, String> {
        val resources = resourcesFor(packageName) ?: return emptyMap()
        val resourceId = resources.getIdentifier("appfilter", "xml", packageName)
        if (resourceId != 0) {
            val parser: XmlResourceParser? = try {
                resources.getXml(resourceId)
            } catch (error: Exception) {
                null
            }
            if (parser != null) {
                val parsed = parseResourceXml(parser)
                parser.close()
                if (parsed.isNotEmpty()) return parsed
            }
        }
        val assetFiles = listOf("appfilter.xml", "xml/appfilter.xml")
        for (asset in assetFiles) {
            try {
                resources.assets.open(asset).bufferedReader().use { reader ->
                    val parsed = AppFilterParser.parse(reader.readText())
                    if (parsed.isNotEmpty()) return parsed
                }
            } catch (error: Exception) {
                // Asset is optional, try the next known location.
            }
        }
        return emptyMap()
    }

    fun resolveDrawable(packageName: String, drawableName: String): Drawable? {
        val resources = resourcesFor(packageName) ?: return null
        val id = try {
            resources.getIdentifier(drawableName, "drawable", packageName)
        } catch (error: Exception) {
            0
        }
        if (id == 0) return null
        return try {
            @Suppress("DEPRECATION")
            resources.getDrawable(id, null)
        } catch (error: Resources.NotFoundException) {
            null
        } catch (error: Exception) {
            null
        }
    }

    /** Resolves every app through the selected pack. */
    fun applyPack(packageName: String, apps: List<AppEntry>): List<AppEntry> {
        val appFilter = loadAppFilter(packageName)
        if (appFilter.isEmpty()) return apps
        return apps.map { app ->
            val drawableName = appFilter[app.component]
            val themed = if (drawableName != null) resolveDrawable(packageName, drawableName) else null
            app.copy(themedIcon = themed)
        }
    }

    private fun resourcesFor(packageName: String): Resources? {
        if (resourceCache.containsKey(packageName)) return resourceCache[packageName]
        val resources = try {
            packageManager.getResourcesForApplication(packageName)
        } catch (error: PackageManager.NameNotFoundException) {
            null
        } catch (error: Exception) {
            null
        }
        resourceCache[packageName] = resources
        return resources
    }

    private fun parseResourceXml(parser: XmlResourceParser): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        try {
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && "item".equals(parser.name, ignoreCase = true)) {
                    val component = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")
                    if (!component.isNullOrBlank() && !drawable.isNullOrBlank()) {
                        AppFilterParser.normaliseComponent(component)?.let { result[it] = drawable }
                    }
                }
                event = parser.next()
            }
        } catch (error: Exception) {
            Log.w(TAG, "Malformed appfilter resource", error)
        }
        return result
    }

    companion object {
        private const val TAG = "IconPackLoader"
    }
}
