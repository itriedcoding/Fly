package com.fly.customizer.util

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * A release as published on the project's GitHub Releases page.
 */
data class UpdateInfo(
    val tag: String,
    val name: String,
    val htmlUrl: String,
    val apkUrl: String?,
    val publishedAt: String,
)

/**
 * Checks the public GitHub Releases API for a newer Fly build.
 */
object UpdateChecker {

    private const val TAG = "UpdateChecker"
    const val RELEASES_PAGE = "https://github.com/itriedcoding/Fly/releases/latest"
    private const val API_LATEST = "https://api.github.com/repos/itriedcoding/Fly/releases/latest"

    /** Numeric version parts, ignoring any `v` prefix or `-suffix`. */
    fun versionParts(version: String): List<Int> {
        val cleaned = version.trim().removePrefix("v").removePrefix("V")
        val numeric = cleaned.takeWhile { it.isDigit() || it == '.' }
        return numeric.split('.').filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }
    }

    /** True when [latest] is a higher version than [current]. */
    fun isNewer(current: String, latest: String): Boolean {
        val currentParts = versionParts(current)
        val latestParts = versionParts(latest)
        if (latestParts.isEmpty()) return false
        val size = maxOf(currentParts.size, latestParts.size)
        for (index in 0 until size) {
            val currentValue = currentParts.getOrElse(index) { 0 }
            val latestValue = latestParts.getOrElse(index) { 0 }
            if (latestValue > currentValue) return true
            if (latestValue < currentValue) return false
        }
        return false
    }

    /** Parses the JSON payload of a GitHub release. */
    fun parseRelease(body: String): UpdateInfo? {
        return try {
            val json = JSONObject(body)
            val tag = json.optString("tag_name")
            if (tag.isBlank()) return null
            val assets = json.optJSONArray("assets")
            var apkUrl: String? = null
            if (assets != null) {
                for (index in 0 until assets.length()) {
                    val asset = assets.optJSONObject(index) ?: continue
                    val name = asset.optString("name")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        break
                    }
                }
            }
            UpdateInfo(
                tag = tag,
                name = json.optString("name", tag),
                htmlUrl = json.optString("html_url", RELEASES_PAGE),
                apkUrl = apkUrl,
                publishedAt = json.optString("published_at"),
            )
        } catch (error: Exception) {
            Log.w(TAG, "Malformed release payload", error)
            null
        }
    }

    /** Blocking network call, run it off the main thread. */
    fun fetchLatest(timeoutMs: Int = 8000): UpdateInfo? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(API_LATEST).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Fly-Android")
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "GitHub responded ${connection.responseCode}")
                return null
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parseRelease(body)
        } catch (error: Exception) {
            Log.w(TAG, "Unable to read GitHub releases", error)
            null
        } finally {
            connection?.disconnect()
        }
    }
}
