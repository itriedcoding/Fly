package com.fly.customizer.ui.animations

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.fly.customizer.FlyApp
import com.fly.customizer.R
import com.fly.customizer.core.Prefs
import com.fly.customizer.core.ThemeCatalog
import com.fly.customizer.databinding.FragmentAnimationsBinding
import kotlin.math.roundToInt

/**
 * Animation playground: twelve presets run on real animators while [CurveView] plots the
 * exact easing curve being used.
 */
class AnimationsFragment : Fragment() {

    private var _binding: FragmentAnimationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: Prefs
    private var preset = AnimationPreset.SPRING
    private var scene: Animator? = null
    private var progressAnimator: ValueAnimator? = null

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
        preset = AnimationPreset.from(prefs.animationPreset)
        binding.curveView.setAccentColor(ThemeCatalog.accent(prefs.accentId).color)

        val presetAdapter = AnimationPresetAdapter(
            presets = AnimationPreset.values().toList(),
            selectedId = preset.id,
        ) { selected ->
            preset = selected
            prefs.animationPreset = selected.id
            updatePresetUi()
            play()
        }
        binding.presetList.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.presetList.adapter = presetAdapter

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
        binding.root.post { play() }
    }

    private fun updatePresetUi() {
        binding.txtPresetInfo.setText(preset.infoRes)
        binding.txtSpeed.text = getString(R.string.speed_value, prefs.animationSpeed)
        binding.curveView.setInterpolator(preset.interpolator())
    }

    private fun play() {
        scene?.cancel()
        progressAnimator?.cancel()

        val density = resources.displayMetrics.density
        val interpolator = preset.interpolator()
        val duration = (1100f / prefs.animationSpeed.coerceIn(0.25f, 3f))
            .toLong()
            .coerceIn(180L, 3200L)
        binding.curveView.setInterpolator(interpolator)
        binding.curveView.setProgress(0f)
        binding.txtPresetInfo.setText(preset.infoRes)

        val staggered = preset == AnimationPreset.STAGGER
        binding.animTarget.isVisible = !staggered
        binding.staggerRow.isVisible = staggered

        if (staggered) {
            val dots = listOf(binding.dotOne, binding.dotTwo, binding.dotThree)
            val step = duration / 5
            val childDuration = duration / 2
            val animators = dots.mapIndexed { index, dot ->
                dot.alpha = 0f
                dot.scaleX = 0.2f
                dot.scaleY = 0.2f
                AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(dot, View.ALPHA, 0f, 1f),
                        ObjectAnimator.ofFloat(dot, View.SCALE_X, 0.2f, 1f),
                        ObjectAnimator.ofFloat(dot, View.SCALE_Y, 0.2f, 1f),
                    )
                    this.duration = childDuration
                    this.interpolator = interpolator
                    startDelay = index * step
                }
            }
            val set = AnimatorSet()
            set.playTogether(animators)
            set.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.curveView.setProgress(1f)
                }
            })
            val total = step * (dots.size - 1) + childDuration
            progressAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                this.duration = total
                this.interpolator = LinearInterpolator()
                addUpdateListener { binding.curveView.setProgress(it.animatedValue as Float) }
            }
            set.start()
            progressAnimator?.start()
            scene = set
            return
        }

        val target = binding.animTarget
        target.alpha = 1f
        target.scaleX = 1f
        target.scaleY = 1f
        target.rotation = 0f
        target.rotationY = 0f
        target.translationX = 0f
        target.translationY = 0f

        val set = AnimatorSet()
        when (preset) {
            AnimationPreset.SPRING -> set.playTogether(
                ObjectAnimator.ofFloat(target, View.SCALE_X, 0.35f, 1f),
                ObjectAnimator.ofFloat(target, View.SCALE_Y, 0.35f, 1f),
                ObjectAnimator.ofFloat(target, View.ALPHA, 0f, 1f),
            )
            AnimationPreset.ORBIT -> set.playTogether(
                ObjectAnimator.ofFloat(target, View.ROTATION, 0f, 360f),
                ObjectAnimator.ofFloat(target, View.TRANSLATION_X, -96f * density, 0f),
                ObjectAnimator.ofFloat(target, View.SCALE_X, 0.55f, 1f),
                ObjectAnimator.ofFloat(target, View.SCALE_Y, 0.55f, 1f),
            )
            AnimationPreset.WAVE -> set.playTogether(
                ObjectAnimator.ofFloat(
                    target, View.TRANSLATION_Y,
                    0f, -90f * density, 0f, -48f * density, 0f,
                ),
                ObjectAnimator.ofFloat(target, View.SCALE_X, 1f, 0.86f, 1f),
                ObjectAnimator.ofFloat(target, View.SCALE_Y, 1f, 0.86f, 1f),
            )
            AnimationPreset.FLIP -> set.playTogether(
                ObjectAnimator.ofFloat(target, View.ROTATION_Y, 0f, 360f),
                ObjectAnimator.ofFloat(target, View.SCALE_X, 1f, 0.78f, 1f),
                ObjectAnimator.ofFloat(target, View.SCALE_Y, 1f, 0.78f, 1f),
            )
            AnimationPreset.SLIDE -> {
                target.translationX = -140f * density
                target.alpha = 0f
                set.playTogether(
                    ObjectAnimator.ofFloat(target, View.TRANSLATION_X, -140f * density, 0f),
                    ObjectAnimator.ofFloat(target, View.ALPHA, 0f, 1f),
                )
            }
            AnimationPreset.ZOOM -> {
                target.scaleX = 0.6f
                target.scaleY = 0.6f
                target.alpha = 0f
                set.playTogether(
                    ObjectAnimator.ofFloat(target, View.SCALE_X, 0.6f, 1f),
                    ObjectAnimator.ofFloat(target, View.SCALE_Y, 0.6f, 1f),
                    ObjectAnimator.ofFloat(target, View.ALPHA, 0f, 1f),
                )
            }
            AnimationPreset.DROP -> {
                target.translationY = -180f * density
                target.alpha = 0f
                set.playTogether(
                    ObjectAnimator.ofFloat(target, View.TRANSLATION_Y, -180f * density, 0f),
                    ObjectAnimator.ofFloat(target, View.ALPHA, 0f, 1f),
                )
            }
            AnimationPreset.SHAKE -> set.playTogether(
                ObjectAnimator.ofFloat(
                    target, View.TRANSLATION_X,
                    0f, 20f * density, -20f * density, 14f * density, -14f * density, 0f,
                ),
            )
            AnimationPreset.PENDULUM -> set.playTogether(
                ObjectAnimator.ofFloat(
                    target, View.ROTATION,
                    0f, -24f, 18f, -12f, 8f, 0f,
                ),
            )
            AnimationPreset.JELLY -> set.playTogether(
                ObjectAnimator.ofFloat(target, View.SCALE_X, 1f, 1.22f, 0.9f, 1f),
                ObjectAnimator.ofFloat(target, View.SCALE_Y, 1f, 0.82f, 1.16f, 1f),
            )
            AnimationPreset.SPIRAL -> {
                target.translationX = -110f * density
                target.scaleX = 0.4f
                target.scaleY = 0.4f
                target.alpha = 0f
                set.playTogether(
                    ObjectAnimator.ofFloat(target, View.ROTATION, 0f, 540f),
                    ObjectAnimator.ofFloat(target, View.TRANSLATION_X, -110f * density, 0f),
                    ObjectAnimator.ofFloat(target, View.SCALE_X, 0.4f, 1f),
                    ObjectAnimator.ofFloat(target, View.SCALE_Y, 0.4f, 1f),
                    ObjectAnimator.ofFloat(target, View.ALPHA, 0f, 1f),
                )
            }
            AnimationPreset.STAGGER -> Unit
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
