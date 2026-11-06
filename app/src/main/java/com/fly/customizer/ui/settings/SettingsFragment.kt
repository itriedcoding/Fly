package com.fly.customizer.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.fly.customizer.BuildConfig
import com.fly.customizer.FlyApp
import com.fly.customizer.R
import com.fly.customizer.core.Prefs
import com.fly.customizer.databinding.FragmentSettingsBinding
import com.fly.customizer.ui.MainActivity
import com.fly.customizer.util.NativeFly

/**
 * Settings: wallpaper defaults, stored SQLite data and build information.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: Prefs
    private var suppressCallbacks = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = FlyApp.prefs
        binding.settingsTargetGroup.check(
            when (prefs.wallpaperTarget) {
                Prefs.TARGET_HOME -> R.id.btnTargetHomeSettings
                Prefs.TARGET_LOCK -> R.id.btnTargetLockSettings
                else -> R.id.btnTargetBothSettings
            },
        )
        binding.settingsTargetGroup.addOnButtonCheckedListener { _, buttonId, checked ->
            if (!checked || suppressCallbacks) return@addOnButtonCheckedListener
            prefs.wallpaperTarget = when (buttonId) {
                R.id.btnTargetHomeSettings -> Prefs.TARGET_HOME
                R.id.btnTargetLockSettings -> Prefs.TARGET_LOCK
                else -> Prefs.TARGET_BOTH
            }
        }

        binding.btnOpenThemes.setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_themes)
        }

        binding.btnClearHistory.setOnClickListener {
            FlyApp.repository.clearHistory()
            Toast.makeText(requireContext(), R.string.history_cleared, Toast.LENGTH_SHORT).show()
            refreshSummary()
        }
        binding.btnClearFavorites.setOnClickListener {
            FlyApp.repository.clearFavorites()
            Toast.makeText(requireContext(), R.string.favorites_cleared, Toast.LENGTH_SHORT).show()
            refreshSummary()
        }

        binding.txtVersion.text = "${getString(R.string.version_label)} ${BuildConfig.VERSION_NAME}"
        binding.txtNativeVersion.text =
            "${getString(R.string.native_label)} ${runCatching { NativeFly.describe() }.getOrDefault("—")}"

        refreshSummary()
        binding.root.post { suppressCallbacks = false }
    }

    private fun refreshSummary() {
        val applies = FlyApp.repository.applyCount()
        val favorites = FlyApp.repository.favoriteCount()
        val themes = FlyApp.repository.themes().size
        binding.txtDataSummary.text = getString(R.string.data_summary, applies, favorites, themes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
