package com.fly.customizer.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * SQLite schema: favourites, apply history, user themes, icon-pack usage, custom icons
 * and pinned widgets.
 */
class FlyDatabase(context: Context) : SQLiteOpenHelper(context.applicationContext, NAME, null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_FAVORITES)
        db.execSQL(SQL_HISTORY)
        db.execSQL(SQL_THEMES)
        db.execSQL(SQL_ICON_PACKS)
        db.execSQL(SQL_CUSTOM_ICONS)
        db.execSQL(SQL_PINNED_WIDGETS)
        db.execSQL("CREATE INDEX idx_history_time ON apply_history(applied_at DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(SQL_CUSTOM_ICONS)
            db.execSQL(SQL_PINNED_WIDGETS)
        }
    }

    fun clearHistory() {
        writableDatabase.delete("apply_history", null, null)
    }

    fun clearFavorites() {
        writableDatabase.delete("favorite_wallpapers", null, null)
    }

    fun insertHistory(presetId: String, label: String, target: String) {
        val values = ContentValues().apply {
            put("preset_id", presetId)
            put("label", label)
            put("target", target)
            put("applied_at", System.currentTimeMillis())
        }
        writableDatabase.insert("apply_history", null, values)
    }

    fun recentHistory(limit: Int): List<HistoryEntry> {
        val entries = ArrayList<HistoryEntry>()
        readableDatabase.query(
            "apply_history",
            arrayOf("id", "preset_id", "label", "target", "applied_at"),
            null, null, null, null, "applied_at DESC", limit.toString()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                entries.add(
                    HistoryEntry(
                        id = cursor.getLong(0),
                        presetId = cursor.getString(1),
                        label = cursor.getString(2),
                        target = cursor.getString(3),
                        appliedAt = cursor.getLong(4),
                    )
                )
            }
        }
        return entries
    }

    fun historyCount(): Int = scalarCount("SELECT COUNT(*) FROM apply_history")

    fun toggleFavorite(presetId: String, hueShift: Float, label: String): Boolean {
        val db = writableDatabase
        val selection = "preset_id = ? AND hue_shift = ?"
        val args = arrayOf(presetId, hueShift.toString())
        val existing = db.query(
            "favorite_wallpapers", arrayOf("id"), selection, args, null, null, null
        ).use { it.count > 0 }
        return if (existing) {
            db.delete("favorite_wallpapers", selection, args)
            false
        } else {
            val values = ContentValues().apply {
                put("preset_id", presetId)
                put("hue_shift", hueShift)
                put("label", label)
                put("created_at", System.currentTimeMillis())
            }
            db.insert("favorite_wallpapers", null, values)
            true
        }
    }

    fun isFavorite(presetId: String, hueShift: Float): Boolean = readableDatabase.query(
        "favorite_wallpapers", arrayOf("id"),
        "preset_id = ? AND hue_shift = ?",
        arrayOf(presetId, hueShift.toString()), null, null, null
    ).use { it.count > 0 }

    fun favoriteCount(): Int = scalarCount("SELECT COUNT(*) FROM favorite_wallpapers")

    fun favoriteWallpapers(): List<FavoriteWallpaper> {
        val entries = ArrayList<FavoriteWallpaper>()
        readableDatabase.query(
            "favorite_wallpapers",
            arrayOf("preset_id", "hue_shift", "label", "created_at"),
            null, null, null, null, "created_at DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                entries.add(
                    FavoriteWallpaper(
                        presetId = cursor.getString(0),
                        hueShift = cursor.getFloat(1),
                        label = cursor.getString(2),
                        createdAt = cursor.getLong(3),
                    )
                )
            }
        }
        return entries
    }

    fun insertCustomTheme(name: String, baseMode: String, accentId: String): Long {
        val values = ContentValues().apply {
            put("name", name)
            put("base_mode", baseMode)
            put("accent_id", accentId)
            put("created_at", System.currentTimeMillis())
        }
        return writableDatabase.insertWithOnConflict(
            "custom_themes", null, values, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun customThemes(): List<CustomTheme> {
        val themes = ArrayList<CustomTheme>()
        readableDatabase.query(
            "custom_themes",
            arrayOf("id", "name", "base_mode", "accent_id", "created_at"),
            null, null, null, null, "created_at DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                themes.add(
                    CustomTheme(
                        id = cursor.getLong(0),
                        name = cursor.getString(1),
                        baseMode = cursor.getString(2),
                        accentId = cursor.getString(3),
                        createdAt = cursor.getLong(4),
                    )
                )
            }
        }
        return themes
    }

    fun deleteCustomTheme(id: Long) {
        writableDatabase.delete("custom_themes", "id = ?", arrayOf(id.toString()))
    }

    fun recordIconPin(packageName: String) {
        val db = writableDatabase
        val current = db.query(
            "icon_pack_usage", arrayOf("applied_count"), "package_name = ?",
            arrayOf(packageName), null, null, null
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }
        val values = ContentValues().apply {
            put("package_name", packageName)
            put("applied_count", current + 1)
            put("last_used", System.currentTimeMillis())
        }
        db.insertWithOnConflict("icon_pack_usage", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun pinnedShortcutCount(): Int =
        scalarCount("SELECT COALESCE(SUM(applied_count), 0) FROM icon_pack_usage")

    fun lastIconPack(): String? = readableDatabase.query(
        "icon_pack_usage", arrayOf("package_name"), null, null, null, null,
        "last_used DESC", "1"
    ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

    fun setCustomIcon(component: String, path: String) {
        val values = ContentValues().apply {
            put("component", component)
            put("path", path)
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(
            "custom_icons", null, values, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun customIcons(): Map<String, String> {
        val entries = LinkedHashMap<String, String>()
        readableDatabase.query(
            "custom_icons", arrayOf("component", "path"), null, null, null, null, "updated_at DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                entries[cursor.getString(0)] = cursor.getString(1)
            }
        }
        return entries
    }

    fun deleteCustomIcon(component: String) {
        writableDatabase.delete("custom_icons", "component = ?", arrayOf(component))
    }

    fun customIconCount(): Int = scalarCount("SELECT COUNT(*) FROM custom_icons")

    fun recordPinnedWidget(provider: String, label: String, widgetId: Int) {
        val values = ContentValues().apply {
            put("provider", provider)
            put("label", label)
            put("widget_id", widgetId)
            put("pinned_at", System.currentTimeMillis())
        }
        writableDatabase.insert("pinned_widgets", null, values)
    }

    /** Restores a pinned widget record from a backup, keeping its original timestamp. */
    fun insertPinnedWidgetRecord(provider: String, label: String, pinnedAt: Long) {
        val values = ContentValues().apply {
            put("provider", provider)
            put("label", label)
            put("widget_id", -1)
            put("pinned_at", pinnedAt)
        }
        writableDatabase.insert("pinned_widgets", null, values)
    }

    /** Restores a favourite from a backup; duplicate preset/hue pairs are ignored. */
    fun addFavorite(presetId: String, hueShift: Float, label: String) {
        val values = ContentValues().apply {
            put("preset_id", presetId)
            put("hue_shift", hueShift)
            put("label", label)
            put("created_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict(
            "favorite_wallpapers", null, values, SQLiteDatabase.CONFLICT_IGNORE
        )
    }

    fun clearCustomIcons() {
        writableDatabase.delete("custom_icons", null, null)
    }

    /** Deletes every row while keeping the schema, used by the reset action. */
    fun clearAll() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("apply_history", null, null)
            db.delete("favorite_wallpapers", null, null)
            db.delete("custom_themes", null, null)
            db.delete("icon_pack_usage", null, null)
            db.delete("custom_icons", null, null)
            db.delete("pinned_widgets", null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun pinnedWidgetCount(): Int = scalarCount("SELECT COUNT(*) FROM pinned_widgets")

    fun pinnedWidgets(limit: Int): List<PinnedWidget> {
        val entries = ArrayList<PinnedWidget>()
        readableDatabase.query(
            "pinned_widgets",
            arrayOf("id", "provider", "label", "widget_id", "pinned_at"),
            null, null, null, null, "pinned_at DESC", limit.toString()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                entries.add(
                    PinnedWidget(
                        id = cursor.getLong(0),
                        provider = cursor.getString(1),
                        label = cursor.getString(2),
                        widgetId = cursor.getInt(3),
                        pinnedAt = cursor.getLong(4),
                    )
                )
            }
        }
        return entries
    }

    private fun scalarCount(sql: String): Int = readableDatabase.rawQuery(sql, null).use { cursor ->
        if (cursor.moveToFirst()) cursor.getInt(0) else 0
    }

    companion object {
        const val NAME = "fly.db"
        const val VERSION = 2

        private const val SQL_FAVORITES = """
            CREATE TABLE IF NOT EXISTS favorite_wallpapers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                preset_id TEXT NOT NULL,
                hue_shift REAL NOT NULL DEFAULT 0,
                label TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                UNIQUE(preset_id, hue_shift)
            )
        """

        private const val SQL_HISTORY = """
            CREATE TABLE IF NOT EXISTS apply_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                preset_id TEXT NOT NULL,
                label TEXT NOT NULL,
                target TEXT NOT NULL,
                applied_at INTEGER NOT NULL
            )
        """

        private const val SQL_THEMES = """
            CREATE TABLE IF NOT EXISTS custom_themes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                base_mode TEXT NOT NULL,
                accent_id TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                UNIQUE(name)
            )
        """

        private const val SQL_ICON_PACKS = """
            CREATE TABLE IF NOT EXISTS icon_pack_usage (
                package_name TEXT PRIMARY KEY,
                applied_count INTEGER NOT NULL DEFAULT 0,
                last_used INTEGER NOT NULL
            )
        """

        private const val SQL_CUSTOM_ICONS = """
            CREATE TABLE IF NOT EXISTS custom_icons (
                component TEXT PRIMARY KEY,
                path TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """

        private const val SQL_PINNED_WIDGETS = """
            CREATE TABLE IF NOT EXISTS pinned_widgets (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                provider TEXT NOT NULL,
                label TEXT NOT NULL,
                widget_id INTEGER NOT NULL,
                pinned_at INTEGER NOT NULL
            )
        """
    }
}

data class HistoryEntry(
    val id: Long,
    val presetId: String,
    val label: String,
    val target: String,
    val appliedAt: Long,
)

data class CustomTheme(
    val id: Long,
    val name: String,
    val baseMode: String,
    val accentId: String,
    val createdAt: Long,
)

data class FavoriteWallpaper(
    val presetId: String,
    val hueShift: Float,
    val label: String,
    val createdAt: Long,
)

data class PinnedWidget(
    val id: Long,
    val provider: String,
    val label: String,
    val widgetId: Int,
    val pinnedAt: Long,
)
