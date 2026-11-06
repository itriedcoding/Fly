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

    companion object {
        private const val FILE_NAME = "fly_prefs"
        private const val KEY_BASE_MODE = "base_mode"
        private const val KEY_ACCENT = "accent"
        private const val KEY_DYNAMIC = "dynamic_color"
        private const val KEY_TARGET = "wallpaper_target"
        private const val KEY_HUE = "hue_shift"
        private const val KEY_TAB = "last_tab"
        private const val KEY_ANIMATION = "animation_preset"
        private const val KEY_SPEED = "animation_speed"

        const val TARGET_BOTH = 0
        const val TARGET_HOME = 1
        const val TARGET_LOCK = 2
    }
}
