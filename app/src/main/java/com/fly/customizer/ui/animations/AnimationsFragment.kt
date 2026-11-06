package com.fly.customizer.ui.animations

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.fly.customizer.FlyApp
import com.fly.customizer.R
import com.fly.customizer.core.Prefs
import com.fly.customizer.core.ThemeCatalog
import com.fly.customizer.databinding.FragmentAnimationsBinding
import kotlin.math.roundToInt

/**
 * Animation playground: real animators run against the stage view while the
 * exact easing curve is drawn by [CurveView].
 */
class AnimationsFragment : Fragment() {

    private var _binding: FragmentAnimationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: Prefs
    private var preset = Preset.SPRING
    private var scene: Animator? = null
    private var progressAnimator: ValueAnimator? = null
    private var suppressCallbacks = true

    private enum class Preset(val id: String, val labelRes: Int, val infoRes: Int) {
        SPRING("spring", R.string.anim_spring, R.string.preset_info_spring),
        ORBIT("orbit", R.string.anim_orbit, R.string.preset_info_orbit),
        WAVE("wave", R.string.anim_wave, R.string.preset_info_wave),
        FLIP("flip", R.string.anim_flip, R.string.preset_info_flip);

        fun interpolator(): TimeInterpolator = when (this) {
            SPRING -> OvershootInterpolator(1.9f)
            ORBIT -> DecelerateInterpolator(1.8f)
            WAVE -> AnticipateOvershootInterpolator(2.2f)
            FLIP -> AccelerateDecelerateInterpolator()
        }

        companion object {
            fun from(id: String?): Preset = values().firstOrNull { it.id == id } ?: SPRING
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAnimationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = FlyApp.prefs
        preset = Preset.from(prefs.animationPreset)

        binding.curveView.setAccentColor(ThemeCatalog.accent(prefs.accentId).color)
        binding.presetGroup.check(
            when (preset) {
                Preset.SPRING -> R.id.btnAnimSpring
                Preset.ORBIT -> R.id.btnAnimOrbit
                Preset.WAVE -> R.id.btnAnimWave
                Preset.FLIP -> R.id.btnAnimFlip
            },
        )
        binding.presetGroup.addOnButtonCheckedListener { _, buttonId, checked ->
            if (!checked || suppressCallbacks) return@addOnButtonCheckedListener
            preset = when (buttonId) {
                R.id.btnAnimOrbit -> Preset.ORBIT
                R.id.btnAnimWave -> Preset.WAVE
                R.id.btnAnimFlip -> Preset.FLIP
                else -> Preset.SPRING
            }
            prefs.animationPreset = preset.id
            updatePresetUi()
            play()
        }

        binding.speedSeek.progress = (prefs.animationSpeed * 100f).roundToInt().coerceIn(25, 300)
        binding.speedSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val speed = progress.coerceAtLeast(25) / 100f
                prefs.animationSpeed = speed
                binding.txtSpeed.text = getString(R.string.speed_value, speed)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
        })

        binding.btnPlayAnim.setOnClickListener { play() }
        updatePresetUi()
        binding.root.post {
            suppressCallbacks = false
            play()
        }
    }

    private fun updatePresetUi() {
        binding.txtPresetInfo.setText(preset.infoRes)
        binding.txtSpeed.text = getString(R.string.speed_value, prefs.animationSpeed)
        binding.curveView.setInterpolator(preset.interpolator())
    }

    private fun play() {
        scene?.cancel()
        progressAnimator?.cancel()

        val target = binding.animTarget
        val density = resources.displayMetrics.density
        target.alpha = 1f
        target.scaleX = 1f
        target.scaleY = 1f
        target.rotation = 0f
        target.rotationY = 0f
        target.translationX = 0f
        target.translationY = 0f

        val interpolator = preset.interpolator()
        val duration = (1100f / prefs.animationSpeed.coerceIn(0.25f, 3f)).toLong().coerceIn(180L, 3200L)
        val set = AnimatorSet()

        when (preset) {
            Preset.SPRING -> set.playTogether(
                ObjectAnimator.ofFloat(target, View.SCALE_X, 0.35f, 1f),
                ObjectAnimator.ofFloat(target, View.SCALE_Y, 0.35f, 1f),
                ObjectAnimator.ofFloat(target, View.ALPHA, 0f, 1f),
            )
            Preset.ORBIT -> set.playTogether(
                ObjectAnimator.ofFloat(target, View.ROTATION, 0f, 360f),
                ObjectAnimator.ofFloat(target, View.TRANSLATION_X, -96f * density, 0f),
                ObjectAnimator.ofFloat(target, View.SCALE_X, 0.55f, 1f),
                ObjectAnimator.ofFloat(target, View.SCALE_Y, 0.55f, 1f),
            )
            Preset.WAVE -> set.playTogether(
                ObjectAnimator.ofFloat(
                    target, View.TRANSLATION_Y,
                    0f, -90f * density, 0f, -48f * density, 0f,
                ),
                ObjectAnimator.ofFloat(target, View.SCALE_X, 1f, 0.86f, 1f),
                ObjectAnimator.ofFloat(target, View.SCALE_Y, 1f, 0.86f, 1f),
            )
            Preset.FLIP -> set.playTogether(
                ObjectAnimator.ofFloat(target, View.ROTATION_Y, 0f, 360f),
                ObjectAnimator.ofFloat(target, View.SCALE_X, 1f, 0.78f, 1f),
                ObjectAnimator.ofFloat(target, View.SCALE_Y, 1f, 0.78f, 1f),
            )
        }

        set.duration = duration
        set.interpolator = interpolator
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                binding.curveView.setProgress(1f)
            }
        })

        progressAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            this.interpolator = LinearInterpolator()
            addUpdateListener { binding.curveView.setProgress(it.animatedValue as Float) }
        }

        binding.curveView.setProgress(0f)
        set.start()
        progressAnimator?.start()
        scene = set
    }

    override fun onDestroyView() {
        scene?.cancel()
        progressAnimator?.cancel()
        super.onDestroyView()
        _binding = null
    }
}
