package com.fly.customizer.ui.animations

import android.animation.TimeInterpolator
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import com.fly.customizer.R

/**
 * Motion presets available in the animation studio.
 */
enum class AnimationPreset(
    val id: String,
    val labelRes: Int,
    val infoRes: Int,
) {
    SPRING("spring", R.string.anim_spring, R.string.preset_info_spring),
    ORBIT("orbit", R.string.anim_orbit, R.string.preset_info_orbit),
    WAVE("wave", R.string.anim_wave, R.string.preset_info_wave),
    FLIP("flip", R.string.anim_flip, R.string.preset_info_flip),
    SLIDE("slide", R.string.anim_slide, R.string.preset_info_slide),
    ZOOM("zoom", R.string.anim_zoom, R.string.preset_info_zoom),
    DROP("drop", R.string.anim_drop, R.string.preset_info_drop),
    SHAKE("shake", R.string.anim_shake, R.string.preset_info_shake),
    PENDULUM("pendulum", R.string.anim_pendulum, R.string.preset_info_pendulum),
    JELLY("jelly", R.string.anim_jelly, R.string.preset_info_jelly),
    SPIRAL("spiral", R.string.anim_spiral, R.string.preset_info_spiral),
    STAGGER("stagger", R.string.anim_stagger, R.string.preset_info_stagger);

    fun interpolator(): TimeInterpolator = when (this) {
        SPRING, DROP, STAGGER -> OvershootInterpolator(1.9f)
        ORBIT, ZOOM, SPIRAL -> DecelerateInterpolator(1.8f)
        WAVE, PENDULUM -> AnticipateOvershootInterpolator(2.2f)
        FLIP, JELLY -> AccelerateDecelerateInterpolator()
        SLIDE, SHAKE -> LinearInterpolator()
    }

    companion object {
        fun from(id: String?): AnimationPreset = values().firstOrNull { it.id == id } ?: SPRING
    }
}
