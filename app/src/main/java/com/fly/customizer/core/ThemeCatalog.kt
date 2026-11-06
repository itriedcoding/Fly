package com.fly.customizer.core

import android.app.Activity
import android.os.Build
import com.fly.customizer.R
import com.google.android.material.color.DynamicColors

/**
 * Available base surfaces of the app.
 */
enum class BaseMode(val id: String, val styleRes: Int, val labelRes: Int) {
    LIGHT("light", R.style.Theme_Fly_Light, R.string.mode_light),
    DARK("dark", R.style.Theme_Fly_Dark, R.string.mode_dark),
    AMOLED("amoled", R.style.Theme_Fly_Amoled, R.string.mode_amoled);

    companion object {
        fun from(id: String?): BaseMode = values().firstOrNull { it.id == id } ?: DARK
    }
}

/**
 * An accent colour plus the theme overlay resource that applies it.
 */
data class AccentSpec(
    val id: String,
    val labelRes: Int,
    val color: Int,
    val containerColor: Int,
    val onContainerColor: Int,
    val overlayRes: Int,
)

/**
 * All theming options the app can apply at runtime.
 */
object ThemeCatalog {

    val accents: List<AccentSpec> = listOf(
        AccentSpec(
            id = "violet",
            labelRes = R.string.accent_violet,
            color = 0xFF7C5CFF.toInt(),
            containerColor = 0xFFEDE9FE.toInt(),
            onContainerColor = 0xFF3A2A78.toInt(),
            overlayRes = R.style.ThemeOverlay_Fly_Violet,
        ),
        AccentSpec(
            id = "azure",
            labelRes = R.string.accent_azure,
            color = 0xFF2E8BFF.toInt(),
            containerColor = 0xFFE0EEFF.toInt(),
            onContainerColor = 0xFF133B66.toInt(),
            overlayRes = R.style.ThemeOverlay_Fly_Azure,
        ),
        AccentSpec(
            id = "emerald",
            labelRes = R.string.accent_emerald,
            color = 0xFF16B364.toInt(),
            containerColor = 0xFFDCF8E7.toInt(),
            onContainerColor = 0xFF0B4029.toInt(),
            overlayRes = R.style.ThemeOverlay_Fly_Emerald,
        ),
        AccentSpec(
            id = "amber",
            labelRes = R.string.accent_amber,
            color = 0xFFF0A020.toInt(),
            containerColor = 0xFFFFF0D4.toInt(),
            onContainerColor = 0xFF4A3208.toInt(),
            overlayRes = R.style.ThemeOverlay_Fly_Amber,
        ),
        AccentSpec(
            id = "rose",
            labelRes = R.string.accent_rose,
            color = 0xFFF0417A.toInt(),
            containerColor = 0xFFFFE1EA.toInt(),
            onContainerColor = 0xFF571228.toInt(),
            overlayRes = R.style.ThemeOverlay_Fly_Rose,
        ),
        AccentSpec(
            id = "citrine",
            labelRes = R.string.accent_citrine,
            color = 0xFFB7D423.toInt(),
            containerColor = 0xFFF0F8CE.toInt(),
            onContainerColor = 0xFF3A4408.toInt(),
            overlayRes = R.style.ThemeOverlay_Fly_Citrine,
        ),
    )

    fun accent(id: String?): AccentSpec = accents.firstOrNull { it.id == id } ?: accents.first()
}

/**
 * Applies the stored theme to an Activity. Must run before `super.onCreate`.
 */
object ThemeController {

    fun apply(activity: Activity, prefs: Prefs) {
        val base = BaseMode.from(prefs.baseMode)
        activity.setTheme(base.styleRes)
        activity.theme.applyStyle(ThemeCatalog.accent(prefs.accentId).overlayRes, true)
        if (prefs.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (DynamicColors.isDynamicColorAvailable()) {
                DynamicColors.applyToActivityIfAvailable(activity)
            }
        }
    }
}
