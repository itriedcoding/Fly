package com.fly.customizer.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.fly.customizer.core.Prefs
import com.fly.customizer.data.FlyRepository
import org.json.JSONArray
import org.json.JSONObject

/**
 * Writes a JSON snapshot of the user's Fly data into `Downloads/Fly`.
 */
object BackupExporter {

    private const val TAG = "BackupExporter"

    enum class Result { SAVED, UNSUPPORTED, FAILED }

    fun buildJson(repository: FlyRepository, prefs: Prefs): String {
        val root = JSONObject()
        root.put("app", "Fly")
        root.put("exportedAt", System.currentTimeMillis())

        val appearance = JSONObject()
        appearance.put("baseMode", prefs.baseMode)
        appearance.put("accent", prefs.accentId)
        appearance.put("wallpaperTarget", prefs.wallpaperTarget)
        appearance.put("hueShift", prefs.hueShift.toDouble())
        root.put("appearance", appearance)

        val themes = JSONArray()
        for (theme in repository.themes()) {
            themes.put(
                JSONObject()
                    .put("name", theme.name)
                    .put("baseMode", theme.baseMode)
                    .put("accent", theme.accentId)
            )
        }
        root.put("themes", themes)

        val favorites = JSONArray()
        for (favorite in repository.favoriteWallpapers()) {
            favorites.put(
                JSONObject()
                    .put("preset", favorite.presetId)
                    .put("hueShift", favorite.hueShift.toDouble())
                    .put("label", favorite.label)
            )
        }
        root.put("favorites", favorites)

        val widgets = JSONArray()
        for (widget in repository.pinnedWidgets(Int.MAX_VALUE)) {
            widgets.put(
                JSONObject()
                    .put("provider", widget.provider)
                    .put("label", widget.label)
                    .put("pinnedAt", widget.pinnedAt)
            )
        }
        root.put("pinnedWidgets", widgets)

        val customIcons = JSONArray()
        for ((component, path) in repository.customIcons()) {
            customIcons.put(JSONObject().put("component", component).put("file", path.substringAfterLast('/')))
        }
        root.put("customIcons", customIcons)

        return root.toString(2)
    }

    fun export(context: Context, repository: FlyRepository, prefs: Prefs): Result {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return Result.UNSUPPORTED
        }
        return try {
            val payload = buildJson(repository, prefs)
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(
                    MediaStore.Downloads.DISPLAY_NAME,
                    "fly-backup-${System.currentTimeMillis()}.json",
                )
                put(MediaStore.Downloads.MIME_TYPE, "application/json")
                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOWNLOADS}/Fly",
                )
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return Result.FAILED
            resolver.openOutputStream(uri)?.use { stream ->
                stream.write(payload.toByteArray())
            } ?: return Result.FAILED
            val publish = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            resolver.update(uri, publish, null, null)
            Result.SAVED
        } catch (error: Exception) {
            Log.e(TAG, "Backup export failed", error)
            Result.FAILED
        }
    }
}
