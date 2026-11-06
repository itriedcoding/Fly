package com.fly.customizer.util

import android.content.Context
import android.util.Log
import java.io.File
import java.util.Formatter

/**
 * Real storage figures and cleanup for the app's private files.
 */
object StorageManager {

    private const val TAG = "StorageManager"
    private const val FOLDER = "custom_icons"

    fun iconsFolder(context: Context): File = File(context.filesDir, FOLDER)

    fun iconBytes(context: Context): Long = try {
        iconsFolder(context).listFiles()?.sumOf { it.length() } ?: 0L
    } catch (error: Exception) {
        Log.w(TAG, "Unable to measure icon storage", error)
        0L
    }

    fun databaseBytes(context: Context): Long = try {
        context.getDatabasePath(FlyDatabaseName.NAME).length()
    } catch (error: Exception) {
        Log.w(TAG, "Unable to measure database size", error)
        0L
    }

    fun totalBytes(context: Context): Long = iconBytes(context) + databaseBytes(context)

    /** Human readable size for the settings summary. */
    fun format(bytes: Long): String = try {
        Formatter().format("%.1f MB", bytes / (1024.0 * 1024.0)).toString()
    } catch (error: Exception) {
        "$bytes bytes"
    }

    /**
     * Deletes every stored custom icon file. Returns how many files were removed.
     */
    fun clearIconFiles(context: Context): Int {
        val folder = iconsFolder(context)
        val files = folder.listFiles() ?: return 0
        var removed = 0
        for (file in files) {
            if (file.isFile && file.delete()) {
                removed++
            }
        }
        return removed
    }

    /** Nested object so this file does not need to import the database just for its name. */
    private object FlyDatabaseName {
        const val NAME = "fly.db"
    }
}
