package com.fly.customizer.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * SQLite schema of the app: favourites, apply history, user themes and icon pack choices.
 */
class FlyDatabase(context: Context) : SQLiteOpenHelper(context.applicationContext, NAME, null, VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE favorite_wallpapers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                preset_id TEXT NOT NULL,
                hue_shift REAL NOT NULL DEFAULT 0,
                label TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                UNIQUE(preset_id, hue_shift)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE apply_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                preset_id TEXT NOT NULL,
                label TEXT NOT NULL,
                target TEXT NOT NULL,
                applied_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE custom_themes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                base_mode TEXT NOT NULL,
                accent_id TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                UNIQUE(name)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE icon_pack_usage (
                package_name TEXT PRIMARY KEY,
                applied_count INTEGER NOT NULL DEFAULT 0,
                last_used INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_history_time ON apply_history(applied_at DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS favorite_wallpapers")
        db.execSQL("DROP TABLE IF EXISTS apply_history")
        db.execSQL("DROP TABLE IF EXISTS custom_themes")
        db.execSQL("DROP TABLE IF EXISTS icon_pack_usage")
        onCreate(db)
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

    fun pinnedShortcutCount(): Int = scalarCount("SELECT COALESCE(SUM(applied_count), 0) FROM icon_pack_usage")

    fun lastIconPack(): String? = readableDatabase.query(
        "icon_pack_usage", arrayOf("package_name"), null, null, null, null,
        "last_used DESC", "1"
    ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

    private fun scalarCount(sql: String): Int = readableDatabase.rawQuery(sql, null).use { cursor ->
        if (cursor.moveToFirst()) cursor.getInt(0) else 0
    }

    companion object {
        const val NAME = "fly.db"
        const val VERSION = 1
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
