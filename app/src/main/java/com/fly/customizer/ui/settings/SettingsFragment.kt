package com.fly.customizer.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.fly.customizer.BuildConfig
import com.fly.customizer.FlyApp
import com.fly.customizer.R
import com.fly.customizer.core.Prefs
import com.fly.customizer.databinding.FragmentSettingsBinding
import com.fly.customizer.ui.MainActivity
import com.fly.customizer.util.NativeFly
import com.fly.customizer.util.UpdateChecker
import com.fly.customizer.util.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Settings: wallpaper defaults, stored SQLite data, GitHub release updates and build info.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: Prefs
    private var latestUpdate: UpdateInfo? = null
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
        binding.txtCurrentBuild.text =
            getString(R.string.update_current, BuildConfig.VERSION_NAME)

        binding.btnCheckUpdates.setOnClickListener { checkForUpdates() }
        binding.btnOpenRelease.setOnClickListener { openRelease() }

        refreshSummary()
        binding.root.post { suppressCallbacks = false }
        checkForUpdates()
    }

    private fun checkForUpdates() {
        if (_binding == null) return
        binding.progressUpdate.isVisible = true
        binding.txtUpdateStatus.setText(R.string.update_checking)
        viewLifecycleOwner.lifecycleScope.launch {
            val info = withContext(Dispatchers.IO) { UpdateChecker.fetchLatest() }
            if (_binding == null) return@launch
            binding.progressUpdate.isVisible = false
            latestUpdate = info
            val current = BuildConfig.VERSION_NAME
            when {
                info == null -> {
                    binding.txtUpdateStatus.text = getString(R.string.update_unreachable, current)
                    binding.btnOpenRelease.setText(R.string.update_open_releases)
                }
                UpdateChecker.isNewer(current, info.tag) -> {
                    binding.txtUpdateStatus.text =
                        getString(R.string.update_available, info.tag, current)
                    binding.btnOpenRelease.setText(R.string.update_download)
                }
                else -> {
                    binding.txtUpdateStatus.text = getString(R.string.update_up_to_date, current)
                    binding.btnOpenRelease.setText(R.string.update_open_releases)
                }
            }
        }
    }

    private fun openRelease() {
        val url = latestUpdate?.apkUrl ?: latestUpdate?.htmlUrl ?: UpdateChecker.RELEASES_PAGE
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (error: Exception) {
            Toast.makeText(requireContext(), R.string.update_no_browser, Toast.LENGTH_LONG).show()
        }
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
