package com.fly.customizer.data

import android.content.Context

/**
 * Repository facade over [FlyDatabase] used by the UI layer.
 */
class FlyRepository(context: Context) {

    private val database = FlyDatabase(context)

    fun recordApply(presetId: String, label: String, target: String) =
        database.insertHistory(presetId, label, target)

    fun recentApplies(limit: Int = 5): List<HistoryEntry> = database.recentHistory(limit)

    fun applyCount(): Int = database.historyCount()

    fun toggleFavorite(presetId: String, hueShift: Float, label: String): Boolean =
        database.toggleFavorite(presetId, hueShift, label)

    fun isFavorite(presetId: String, hueShift: Float): Boolean =
        database.isFavorite(presetId, hueShift)

    fun favoriteCount(): Int = database.favoriteCount()

    fun favoriteWallpapers(): List<FavoriteWallpaper> = database.favoriteWallpapers()

    fun saveTheme(name: String, baseMode: String, accentId: String): Long =
        database.insertCustomTheme(name, baseMode, accentId)

    fun themes(): List<CustomTheme> = database.customThemes()

    fun deleteTheme(id: Long) = database.deleteCustomTheme(id)

    fun recordShortcutPin(packageName: String) = database.recordIconPin(packageName)

    fun pinnedShortcutCount(): Int = database.pinnedShortcutCount()

    fun lastIconPack(): String? = database.lastIconPack()

    fun setCustomIcon(component: String, path: String) = database.setCustomIcon(component, path)

    fun customIcons(): Map<String, String> = database.customIcons()

    fun deleteCustomIcon(component: String) = database.deleteCustomIcon(component)

    fun customIconCount(): Int = database.customIconCount()

    fun recordPinnedWidget(provider: String, label: String, widgetId: Int) =
        database.recordPinnedWidget(provider, label, widgetId)

    fun pinnedWidgetCount(): Int = database.pinnedWidgetCount()

    fun pinnedWidgets(limit: Int = 10): List<PinnedWidget> = database.pinnedWidgets(limit)

    fun clearHistory() = database.clearHistory()

    fun clearFavorites() = database.clearFavorites()
}
