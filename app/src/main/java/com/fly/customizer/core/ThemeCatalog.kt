package com.fly.customizer.core

import android.app.Activity
import android.os.Build
import com.fly.customizer.R
import com.fly.customizer.util.ColorMath
import com.google.android.material.color.DynamicColors

/**
 * Available base surfaces of the app.
 */
enum class BaseMode(
    val id: String,
    val styleRes: Int,
    val labelRes: Int,
    val surfaceColor: Int,
) {
    LIGHT("light", R.style.Theme_Fly_Light, R.string.mode_light, 0xFFFAFAFC.toInt()),
    DARK("dark", R.style.Theme_Fly_Dark, R.string.mode_dark, 0xFF17171F.toInt()),
    AMOLED("amoled", R.style.Theme_Fly_Amoled, R.string.mode_amoled, 0xFF000000.toInt());

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
 * A ready-made base + accent combination from the stock theme catalogue.
 */
data class ThemePreset(
    val id: String,
    val labelRes: Int,
    val baseMode: BaseMode,
    val accentId: String,
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
        AccentSpec(
            id = "teal",
            labelRes = R.string.accent_teal,
            color = 0xFF12B5A5.toInt(),
            containerColor = 0xFFD7F7F2.toInt(),
            onContainerColor = 0xFF0B3F39.toInt(),
            overlayRes = R.style.ThemeOverlay_Fly_Teal,
        ),
        AccentSpec(
            id = "coral",
            labelRes = R.string.accent_coral,
            color = 0xFFFF6B5B.toInt(),
            containerColor = 0xFFFFE3DF.toInt(),
            onContainerColor = 0xFF5A1D16.toInt(),
            overlayRes = R.style.ThemeOverlay_Fly_Coral,
        ),
        AccentSpec(
            id = "orchid",
            labelRes = R.string.accent_orchid,
            color = 0xFFC05CFF.toInt(),
            containerColor = 0xFFF6E0FF.toInt(),
            onContainerColor = 0xFF4A1263.toInt(),
            overlayRes = R.style.ThemeOverlay_Fly_Orchid,
        ),
        AccentSpec(
            id = "slate",
            labelRes = R.string.accent_slate,
            color = 0xFF64748B.toInt(),
            containerColor = 0xFFE2E8F0.toInt(),
            onContainerColor = 0xFF2B3444.toInt(),
            overlayRes = R.style.ThemeOverlay_Fly_Slate,
        ),
        AccentSpec(
            id = "sand",
            labelRes = R.string.accent_sand,
            color = 0xFFC9A227.toInt(),
            containerColor = 0xFFFBF0D2.toInt(),
            onContainerColor = 0xFF463606.toInt(),
            overlayRes = R.style.ThemeOverlay_Fly_Sand,
        ),
        AccentSpec(
            id = "crimson",
            labelRes = R.string.accent_crimson,
            color = 0xFFD7263D.toInt(),
            containerColor = 0xFFFFE0E4.toInt(),
            onContainerColor = 0xFF5A0D17.toInt(),
            overlayRes = R.style.ThemeOverlay_Fly_Crimson,
        ),
    )

    /** Sixteen ready-made themes across all three base surfaces. */
    val stockThemes: List<ThemePreset> = listOf(
        ThemePreset("obsidian", R.string.theme_obsidian, BaseMode.AMOLED, "violet"),
        ThemePreset("midnight_ink", R.string.theme_midnight_ink, BaseMode.DARK, "azure"),
        ThemePreset("aurora_dark", R.string.theme_aurora_dark, BaseMode.DARK, "emerald"),
        ThemePreset("neon_tokyo", R.string.theme_neon_tokyo, BaseMode.DARK, "rose"),
        ThemePreset("ember_theme", R.string.theme_ember_theme, BaseMode.DARK, "coral"),
        ThemePreset("cyber_lime", R.string.theme_cyber_lime, BaseMode.DARK, "citrine"),
        ThemePreset("carbon", R.string.theme_carbon, BaseMode.AMOLED, "slate"),
        ThemePreset("ocean_deep", R.string.theme_ocean_deep, BaseMode.AMOLED, "teal"),
        ThemePreset("plum_noir", R.string.theme_plum_noir, BaseMode.AMOLED, "orchid"),
        ThemePreset("mint_frost", R.string.theme_mint_frost, BaseMode.LIGHT, "emerald"),
        ThemePreset("paper_violet", R.string.theme_paper_violet, BaseMode.LIGHT, "violet"),
        ThemePreset("sunrise_theme", R.string.theme_sunrise_theme, BaseMode.LIGHT, "amber"),
        ThemePreset("glacier_light", R.string.theme_glacier_light, BaseMode.LIGHT, "azure"),
        ThemePreset("blossom", R.string.theme_blossom, BaseMode.LIGHT, "rose"),
        ThemePreset("sandstone", R.string.theme_sandstone, BaseMode.LIGHT, "sand"),
        ThemePreset("blueprint", R.string.theme_blueprint, BaseMode.LIGHT, "slate"),
        ThemePreset("graphite", R.string.theme_graphite, BaseMode.DARK, "slate"),
        ThemePreset("pine", R.string.theme_pine, BaseMode.AMOLED, "emerald"),
        ThemePreset("tide", R.string.theme_tide, BaseMode.LIGHT, "teal"),
        ThemePreset("carnival", R.string.theme_carnival, BaseMode.DARK, "amber"),
        ThemePreset("quartz", R.string.theme_quartz, BaseMode.LIGHT, "orchid"),
        ThemePreset("vineyard", R.string.theme_vineyard, BaseMode.AMOLED, "rose"),
        ThemePreset("frost", R.string.theme_frost, BaseMode.LIGHT, "citrine"),
        ThemePreset("dusk", R.string.theme_dusk, BaseMode.DARK, "violet"),
    )

    fun accent(id: String?): AccentSpec = accents.firstOrNull { it.id == id } ?: accents.first()

    /** Accent closest to an arbitrary colour, used for wallpaper matching. */
    fun nearestAccent(color: Int): AccentSpec =
        accents.minByOrNull { ColorMath.distance(it.color, color) } ?: accents.first()
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
