package com.fly.customizer.util

import android.util.Base64
import com.fly.customizer.core.Prefs
import com.fly.customizer.data.FlyRepository
import org.json.JSONObject

/**
 * Restores a JSON backup produced by [BackupExporter].
 *
 * Everything is validated against the live catalogues: unknown wallpaper ids and unknown accents
 * are skipped instead of stored, and custom icons are only restored when their PNG payload is
 * present in the file.
 */
object BackupImporter {

    data class Summary(
        val themes: Int,
        val favorites: Int,
        val icons: Int,
        val widgets: Int,
        val appearance: Boolean,
    ) {
        val isEmpty: Boolean
            get() = themes == 0 && favorites == 0 && icons == 0 && widgets == 0 && !appearance
    }

    /** Returns null when the payload is not a Fly backup at all. */
    fun apply(payload: String, repository: FlyRepository, prefs: Prefs): Summary? {
        val root = try {
            JSONObject(payload)
        } catch (error: Exception) {
            return null
        }
        if (root.optString("app") != "Fly") return null

        var themes = 0
        var favorites = 0
        var icons = 0
        var widgets = 0
        var appearance = false

        root.optJSONObject("appearance")?.let { settings ->
            val baseMode = settings.optString("baseMode")
            if (baseMode == "light" || baseMode == "dark" || baseMode == "amoled") {
                prefs.baseMode = baseMode
                appearance = true
            }
            val accent = settings.optString("accent")
            if (ThemeCatalogLookup.accentExists(accent)) {
                prefs.accentId = accent
                appearance = true
            }
            if (settings.has("wallpaperTarget")) {
                prefs.wallpaperTarget = settings.optInt("wallpaperTarget", prefs.wallpaperTarget)
                appearance = true
            }
            if (settings.has("hueShift")) {
                prefs.hueShift = settings.optDouble("hueShift", 0.0).toFloat()
            }
            val live = settings.optString("liveWallpaper")
            if (live.isNotEmpty() && AnimatedWallpaperCatalog.preset(live) != null) {
                prefs.liveWallpaperId = live
            }
        }

        val themeArray = root.optJSONArray("themes")
        if (themeArray != null) {
            for (index in 0 until themeArray.length()) {
                val theme = themeArray.optJSONObject(index) ?: continue
                val name = theme.optString("name").trim()
                val accent = theme.optString("accent")
                val base = theme.optString("baseMode")
                if (name.isEmpty() || !ThemeCatalogLookup.accentExists(accent)) continue
                if (base != "light" && base != "dark" && base != "amoled") continue
                repository.saveTheme(name, base, accent)
                themes++
            }
        }

        val favoriteArray = root.optJSONArray("favorites")
        if (favoriteArray != null) {
            for (index in 0 until favoriteArray.length()) {
                val favorite = favoriteArray.optJSONObject(index) ?: continue
                val presetId = favorite.optString("preset")
                if (WallpaperStudio.preset(presetId) == null) continue
                repository.addFavorite(
                    presetId,
                    favorite.optDouble("hueShift", 0.0).toFloat(),
                    favorite.optString("label", presetId),
                )
                favorites++
            }
        }

        val widgetArray = root.optJSONArray("pinnedWidgets")
        if (widgetArray != null) {
            for (index in 0 until widgetArray.length()) {
                val widget = widgetArray.optJSONObject(index) ?: continue
                val provider = widget.optString("provider")
                if (provider.isEmpty()) continue
                repository.addPinnedWidgetRecord(
                    provider,
                    widget.optString("label", provider),
                    widget.optLong("pinnedAt", System.currentTimeMillis()),
                )
                widgets++
            }
        }

        val iconArray = root.optJSONArray("customIcons")
        if (iconArray != null) {
            for (index in 0 until iconArray.length()) {
                val icon = iconArray.optJSONObject(index) ?: continue
                val component = icon.optString("component")
                val encoded = icon.optString("data")
                if (component.isEmpty() || encoded.isEmpty()) continue
                val bytes = try {
                    Base64.decode(encoded, Base64.NO_WRAP)
                } catch (error: Exception) {
                    null
                } ?: continue
                val path = CustomIconStore.saveBytes(
                    com.fly.customizer.FlyApp.instance,
                    component,
                    bytes,
                ) ?: continue
                repository.setCustomIcon(component, path)
                icons++
            }
        }

        return Summary(themes, favorites, icons, widgets, appearance)
    }

    /** Kept separate so the importer does not depend on the whole theme catalogue object. */
    private object ThemeCatalogLookup {
        fun accentExists(id: String?): Boolean =
            com.fly.customizer.core.ThemeCatalog.accents.any { it.id == id }
    }
}
