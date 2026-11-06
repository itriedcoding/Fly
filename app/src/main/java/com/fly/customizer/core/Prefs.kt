package com.fly.customizer.core

import android.content.Context

/**
 * Typed access to the persisted user preferences.
 */
class Prefs(context: Context) {

    private val store = context.applicationContext
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var baseMode: String
        get() = store.getString(KEY_BASE_MODE, BaseMode.DARK.id) ?: BaseMode.DARK.id
        set(value) = store.edit().putString(KEY_BASE_MODE, value).apply()

    var accentId: String
        get() = store.getString(KEY_ACCENT, ThemeCatalog.accents.first().id)
            ?: ThemeCatalog.accents.first().id
        set(value) = store.edit().putString(KEY_ACCENT, value).apply()

    var dynamicColor: Boolean
        get() = store.getBoolean(KEY_DYNAMIC, false)
        set(value) = store.edit().putBoolean(KEY_DYNAMIC, value).apply()

    /** One of [TARGET_BOTH], [TARGET_HOME] or [TARGET_LOCK]. */
    var wallpaperTarget: Int
        get() = store.getInt(KEY_TARGET, TARGET_BOTH)
        set(value) = store.edit().putInt(KEY_TARGET, value).apply()

    /** Wallpaper rotation applied by the remix button, in degrees. */
    var hueShift: Float
        get() = store.getFloat(KEY_HUE, 0f)
        set(value) = store.edit().putFloat(KEY_HUE, value).apply()

    var lastTab: Int
        get() = store.getInt(KEY_TAB, 0)
        set(value) = store.edit().putInt(KEY_TAB, value).apply()

    var animationPreset: String
        get() = store.getString(KEY_ANIMATION, "spring") ?: "spring"
        set(value) = store.edit().putString(KEY_ANIMATION, value).apply()

    var animationSpeed: Float
        get() = store.getFloat(KEY_SPEED, 1f)
        set(value) = store.edit().putFloat(KEY_SPEED, value).apply()

    /** Render size for generated wallpapers, one of [QUALITY_SMALL], [QUALITY_BALANCED], [QUALITY_FULL]. */
    var wallpaperQuality: Int
        get() = store.getInt(KEY_QUALITY, QUALITY_BALANCED)
        set(value) = store.edit().putInt(KEY_QUALITY, value).apply()

    /** Target frame rate for the live wallpaper engine. */
    var liveFps: Int
        get() = store.getInt(KEY_LIVE_FPS, 30).coerceIn(MIN_FPS, MAX_FPS)
        set(value) = store.edit().putInt(KEY_LIVE_FPS, value.coerceIn(MIN_FPS, MAX_FPS)).apply()

    /** Id of the animated wallpaper the live wallpaper service should render. */
    var liveWallpaperId: String
        get() = store.getString(KEY_LIVE_ID, "drift_aurora") ?: "drift_aurora"
        set(value) = store.edit().putString(KEY_LIVE_ID, value).apply()

    /** Freezes animation playback in previews and the live wallpaper engine. */
    var reduceMotion: Boolean
        get() = store.getBoolean(KEY_REDUCE_MOTION, false)
        set(value) = store.edit().putBoolean(KEY_REDUCE_MOTION, value).apply()

    var haptics: Boolean
        get() = store.getBoolean(KEY_HAPTICS, true)
        set(value) = store.edit().putBoolean(KEY_HAPTICS, value).apply()

    /** Smaller icons and tighter padding in the app grid. */
    var compactIcons: Boolean
        get() = store.getBoolean(KEY_COMPACT, false)
        set(value) = store.edit().putBoolean(KEY_COMPACT, value).apply()

    var checkUpdatesOnLaunch: Boolean
        get() = store.getBoolean(KEY_UPDATE_ON_LAUNCH, true)
        set(value) = store.edit().putBoolean(KEY_UPDATE_ON_LAUNCH, value).apply()

    /** Clears every stored preference, returning the app to its defaults. */
    fun reset() {
        store.edit().clear().apply()
    }

    companion object {
        const val QUALITY_SMALL = 0
        const val QUALITY_BALANCED = 1
        const val QUALITY_FULL = 2

        const val MIN_FPS = 12
        const val MAX_FPS = 60

        private const val FILE_NAME = "fly_prefs"
        private const val KEY_BASE_MODE = "base_mode"
        private const val KEY_ACCENT = "accent"
        private const val KEY_DYNAMIC = "dynamic_color"
        private const val KEY_TARGET = "wallpaper_target"
        private const val KEY_HUE = "hue_shift"
        private const val KEY_TAB = "last_tab"
        private const val KEY_ANIMATION = "animation_preset"
        private const val KEY_SPEED = "animation_speed"
        private const val KEY_QUALITY = "wallpaper_quality"
        private const val KEY_LIVE_FPS = "live_fps"
        private const val KEY_LIVE_ID = "live_wallpaper_id"
        private const val KEY_REDUCE_MOTION = "reduce_motion"
        private const val KEY_HAPTICS = "haptics"
        private const val KEY_COMPACT = "compact_icons"
        private const val KEY_UPDATE_ON_LAUNCH = "update_on_launch"

        const val TARGET_BOTH = 0
        const val TARGET_HOME = 1
        const val TARGET_LOCK = 2
    }
}
