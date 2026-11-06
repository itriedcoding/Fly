package com.fly.customizer.ui.wallpapers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.fly.customizer.FlyApp
import com.fly.customizer.R
import com.fly.customizer.core.Prefs
import com.fly.customizer.databinding.FragmentWallpapersBinding
import com.fly.customizer.util.ThumbnailCache
import com.fly.customizer.util.WallpaperPreset
import com.fly.customizer.util.WallpaperStudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Wallpaper studio: remix the palette, choose the target surface and apply for real.
 */
class WallpapersFragment : Fragment() {

    private var _binding: FragmentWallpapersBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: Prefs
    private lateinit var adapter: WallpaperAdapter
    private var hueShift = 0f
    private var applying = false
    private var favorites: Set<String> = emptySet()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentWallpapersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = FlyApp.prefs
        hueShift = prefs.hueShift
        favorites = loadFavorites()

        adapter = WallpaperAdapter(
            scope = viewLifecycleOwner.lifecycleScope,
            onApply = { preset -> applyWallpaper(preset) },
            onFavorite = { preset -> toggleFavorite(preset) },
        )
        binding.wallpaperGrid.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.wallpaperGrid.adapter = adapter
        adapter.submit(WallpaperStudio.presets, hueShift, favorites)

        binding.targetGroup.check(
            when (prefs.wallpaperTarget) {
                Prefs.TARGET_HOME -> R.id.btnTargetHome
                Prefs.TARGET_LOCK -> R.id.btnTargetLock
                else -> R.id.btnTargetBoth
            },
        )
        binding.targetGroup.addOnButtonCheckedListener { _, buttonId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            prefs.wallpaperTarget = when (buttonId) {
                R.id.btnTargetHome -> Prefs.TARGET_HOME
                R.id.btnTargetLock -> Prefs.TARGET_LOCK
                else -> Prefs.TARGET_BOTH
            }
        }

        binding.btnRemix.setOnClickListener {
            hueShift = (hueShift + 25f) % 360f
            prefs.hueShift = hueShift
            ThumbnailCache.clear()
            updateHueLabel()
            adapter.notifyDataSetChanged()
        }

        updateHueLabel()
    }

    private fun updateHueLabel() {
        binding.txtHueShift.text = getString(R.string.hue_shift_value, hueShift.roundToInt())
    }

    private fun applyWallpaper(preset: WallpaperPreset) {
        if (applying) return
        applying = true
        binding.applyProgress.isVisible = true
        val context = requireContext().applicationContext
        val target = prefs.wallpaperTarget
        val shift = hueShift
        viewLifecycleOwner.lifecycleScope.launch {
            val size = WallpaperStudio.suggestedSize(context)
            val bitmap = withContext(Dispatchers.Default) {
                WallpaperStudio.render(preset, size.first, size.second, shift)
            }
            val success = WallpaperStudio.applyToSystem(context, bitmap, target)
            bitmap.recycle()
            binding.applyProgress.isVisible = false
            applying = false
            if (!isAdded) return@launch
            val targetLabel = getString(WallpaperStudio.targetLabelRes(target))
            if (success) {
                FlyApp.repository.recordApply(preset.id, getString(preset.labelRes), targetLabel)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.applied_toast, targetLabel),
                    Toast.LENGTH_SHORT,
                ).show()
            } else {
                Toast.makeText(requireContext(), R.string.apply_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun toggleFavorite(preset: WallpaperPreset) {
        val added = FlyApp.repository.toggleFavorite(preset.id, hueShift, getString(preset.labelRes))
        favorites = loadFavorites()
        adapter.submit(WallpaperStudio.presets, hueShift, favorites)
        Toast.makeText(
            requireContext(),
            if (added) R.string.favorite_added else R.string.favorite_removed,
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun loadFavorites(): Set<String> =
        WallpaperStudio.presets
            .filter { FlyApp.repository.isFavorite(it.id, hueShift) }
            .map { it.id }
            .toSet()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
