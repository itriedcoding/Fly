package com.fly.customizer.ui.home

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.fly.customizer.FlyApp
import com.fly.customizer.R
import com.fly.customizer.databinding.FragmentHomeBinding
import com.fly.customizer.databinding.ItemHistoryBinding
import com.fly.customizer.ui.MainActivity
import com.fly.customizer.ui.animations.AnimationsFragment
import com.fly.customizer.util.AnimatedWallpaperCatalog
import com.fly.customizer.util.DeviceInfo
import com.fly.customizer.util.LiveWallpaperController
import com.fly.customizer.util.NativeFly
import com.fly.customizer.util.WallpaperAccent
import com.fly.customizer.util.WallpaperStudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Dashboard: current wallpaper palette, device facts, live package counts, quick navigation and
 * apply history.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnOpenWallpapers.setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_wallpapers)
        }
        binding.btnOpenThemes.setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_themes)
        }
        binding.btnOpenStudio.setOnClickListener {
            (activity as? MainActivity)?.openScreen(AnimationsFragment())
        }

        binding.engineVersion.text = runCatching { NativeFly.describe() }
            .getOrDefault(getString(R.string.native_label))
        binding.engineDetail.text = getString(
            R.string.engine_detail,
            binding.engineVersion.text,
            WallpaperStudio.presets.size,
        )

        loadDeviceSnapshot()
        loadWallpaperCard()
        refreshLiveWallpaper()
        renderHistory()
    }

    override fun onResume() {
        super.onResume()
        renderHistory()
        refreshLiveWallpaper()
    }

    private fun refreshLiveWallpaper() {
        if (_binding == null) return
        val running = LiveWallpaperController.isRunning(requireContext())
        val preset = AnimatedWallpaperCatalog.presetOrDefault(FlyApp.prefs.liveWallpaperId)
        binding.txtLiveWallpaper.text = if (running) {
            getString(R.string.live_running, getString(preset.labelRes))
        } else {
            getString(R.string.live_not_running)
        }
    }

    private fun loadDeviceSnapshot() {
        val context = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val snapshot = withContext(Dispatchers.IO) { DeviceInfo.snapshot(context) }
            if (_binding == null) return@launch
            binding.statApps.text = snapshot.launchableApps.toString()
            binding.statPacks.text = snapshot.installedIconPacks.toString()
            binding.statWidgets.text = snapshot.widgetProviders.toString()
            binding.deviceModel.text = getString(
                R.string.device_model_value, snapshot.manufacturer, snapshot.model,
            )
            binding.deviceAndroid.text = getString(
                R.string.device_android_value, snapshot.androidVersion, snapshot.sdkLevel,
            )
            binding.deviceDisplay.text = getString(
                R.string.device_display_value,
                snapshot.resolution,
                snapshot.densityBucket,
                snapshot.densityDpi,
            )
            val launcher = snapshot.launcherPackage?.let { "${snapshot.launcherLabel} ($it)" }
                ?: snapshot.launcherLabel
            binding.deviceLauncher.text = getString(R.string.device_launcher_value, launcher)
        }
    }

    private fun loadWallpaperCard() {
        val context = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val preview = withContext(Dispatchers.IO) { WallpaperAccent.preview(context) }
            if (_binding == null) return@launch
            if (preview == null) {
                binding.wallpaperPreview.isVisible = false
                binding.paletteRow.isVisible = false
                binding.txtPaletteCaption.isVisible = false
                binding.txtWallpaperEmpty.isVisible = true
                return@launch
            }
            binding.txtWallpaperEmpty.isVisible = false
            binding.wallpaperPreview.setImageBitmap(preview.bitmap)
            renderPalette(preview.colors)
            binding.txtPaletteCaption.text =
                getString(R.string.palette_caption, preview.colors.size)
        }
    }

    private fun renderPalette(colors: IntArray) {
        val density = resources.displayMetrics.density
        val size = (24 * density).toInt()
        val margin = (8 * density).toInt()
        binding.paletteRow.removeAllViews()
        for (color in colors) {
            val dot = View(requireContext())
            val params = LinearLayout.LayoutParams(size, size)
            params.marginEnd = margin
            dot.layoutParams = params
            dot.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
                setStroke((1 * density).toInt().coerceAtLeast(1), 0x33FFFFFF)
            }
            binding.paletteRow.addView(dot)
        }
    }

    private fun renderHistory() {
        binding.recentContainer.removeAllViews()
        val entries = FlyApp.repository.recentApplies(5)
        binding.recentEmpty.isVisible = entries.isEmpty()
        for (entry in entries) {
            val row = ItemHistoryBinding.inflate(layoutInflater, binding.recentContainer, false)
            row.historyLabel.text = entry.label
            val whenText = DateUtils.getRelativeTimeSpanString(
                entry.appliedAt,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
            )
            row.historyMeta.text = getString(R.string.recent_meta, entry.target, whenText)
            binding.recentContainer.addView(row.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.paletteRow.removeAllViews()
        binding.wallpaperPreview.setImageDrawable(null)
        _binding = null
    }
}
