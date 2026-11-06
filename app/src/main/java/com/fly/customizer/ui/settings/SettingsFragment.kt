package com.fly.customizer.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.fly.customizer.BuildConfig
import com.fly.customizer.FlyApp
import com.fly.customizer.R
import com.fly.customizer.core.Prefs
import com.fly.customizer.databinding.FragmentSettingsBinding
import com.fly.customizer.ui.MainActivity
import com.fly.customizer.util.BackupExporter
import com.fly.customizer.util.BackupImporter
import com.fly.customizer.util.NativeFly
import com.fly.customizer.util.StorageManager
import com.fly.customizer.util.ThumbnailCache
import com.fly.customizer.util.UpdateChecker
import com.fly.customizer.util.UpdateInfo
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Settings: appearance/motion, wallpaper rendering defaults, GitHub release updates, backup
 * import/export, storage accounting and a full reset.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: Prefs
    private var latestUpdate: UpdateInfo? = null
    private var suppressCallbacks = true

    private val pickBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importBackup(uri)
    }

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
        bindWallpaperTarget()
        bindQuality()
        bindFps()
        bindSwitches()

        binding.btnOpenThemes.setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_themes)
        }

        binding.btnClearHistory.setOnClickListener {
            FlyApp.repository.clearHistory()
            toast(getString(R.string.history_cleared))
            refreshSummary()
        }
        binding.btnClearFavorites.setOnClickListener {
            FlyApp.repository.clearFavorites()
            toast(getString(R.string.favorites_cleared))
            refreshSummary()
        }
        binding.btnClearCustomIcons.setOnClickListener { removeCustomIcons() }
        binding.btnClearPreviews.setOnClickListener {
            ThumbnailCache.clear()
            toast(getString(R.string.previews_cleared))
        }
        binding.btnExportBackup.setOnClickListener { exportBackup() }
        binding.btnImportBackup.setOnClickListener {
            pickBackup.launch(arrayOf("application/json", "text/plain"))
        }
        binding.btnResetAll.setOnClickListener { confirmReset() }

        binding.txtVersion.text = "${getString(R.string.version_label)} ${BuildConfig.VERSION_NAME}"
        binding.txtNativeVersion.text =
            "${getString(R.string.native_label)} ${runCatching { NativeFly.describe() }.getOrDefault("—")}"
        binding.txtCurrentBuild.text =
            getString(R.string.update_current, BuildConfig.VERSION_NAME)

        binding.btnCheckUpdates.setOnClickListener { checkForUpdates() }
        binding.btnOpenRelease.setOnClickListener { openRelease() }

        refreshSummary()
        binding.root.post { suppressCallbacks = false }
        if (prefs.checkUpdatesOnLaunch) {
            checkForUpdates()
        } else {
            binding.txtUpdateStatus.text = getString(R.string.update_current, BuildConfig.VERSION_NAME)
        }
    }

    private fun bindWallpaperTarget() {
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
    }

    private fun bindQuality() {
        binding.qualityGroup.check(
            when (prefs.wallpaperQuality) {
                Prefs.QUALITY_SMALL -> R.id.btnQualitySmall
                Prefs.QUALITY_FULL -> R.id.btnQualityFull
                else -> R.id.btnQualityBalanced
            },
        )
        binding.qualityGroup.addOnButtonCheckedListener { _, buttonId, checked ->
            if (!checked || suppressCallbacks) return@addOnButtonCheckedListener
            prefs.wallpaperQuality = when (buttonId) {
                R.id.btnQualitySmall -> Prefs.QUALITY_SMALL
                R.id.btnQualityFull -> Prefs.QUALITY_FULL
                else -> Prefs.QUALITY_BALANCED
            }
            ThumbnailCache.clear()
        }
    }

    private fun bindFps() {
        binding.fpsGroup.check(
            when {
                prefs.liveFps <= 24 -> R.id.btnFpsBattery
                prefs.liveFps >= 45 -> R.id.btnFpsSmooth
                else -> R.id.btnFpsBalanced
            },
        )
        binding.fpsGroup.addOnButtonCheckedListener { _, buttonId, checked ->
            if (!checked || suppressCallbacks) return@addOnButtonCheckedListener
            prefs.liveFps = when (buttonId) {
                R.id.btnFpsBattery -> 24
                R.id.btnFpsSmooth -> 45
                else -> 30
            }
        }
    }

    private fun bindSwitches() {
        binding.switchReduceMotion.isChecked = prefs.reduceMotion
        binding.switchReduceMotion.setOnCheckedChangeListener { _, checked ->
            if (suppressCallbacks) return@setOnCheckedChangeListener
            prefs.reduceMotion = checked
        }
        binding.switchHaptics.isChecked = prefs.haptics
        binding.switchHaptics.setOnCheckedChangeListener { _, checked ->
            if (suppressCallbacks) return@setOnCheckedChangeListener
            prefs.haptics = checked
        }
        binding.switchCompact.isChecked = prefs.compactIcons
        binding.switchCompact.setOnCheckedChangeListener { _, checked ->
            if (suppressCallbacks) return@setOnCheckedChangeListener
            prefs.compactIcons = checked
        }
        binding.switchUpdateOnLaunch.isChecked = prefs.checkUpdatesOnLaunch
        binding.switchUpdateOnLaunch.setOnCheckedChangeListener { _, checked ->
            if (suppressCallbacks) return@setOnCheckedChangeListener
            prefs.checkUpdatesOnLaunch = checked
        }
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
            toast(getString(R.string.update_no_browser))
        }
    }

    private fun exportBackup() {
        val context = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                BackupExporter.export(context, FlyApp.repository, prefs)
            }
            if (_binding == null) return@launch
            val message = when (result) {
                BackupExporter.Result.SAVED -> R.string.backup_saved
                BackupExporter.Result.UNSUPPORTED -> R.string.backup_needs_android10
                BackupExporter.Result.FAILED -> R.string.backup_failed
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    private fun importBackup(uri: Uri) {
        val context = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val summary = withContext(Dispatchers.IO) {
                val payload = try {
                    context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                } catch (error: Exception) {
                    null
                }
                if (payload == null) null else BackupImporter.apply(payload, FlyApp.repository, prefs)
            }
            if (_binding == null) return@launch
            when {
                summary == null -> toast(getString(R.string.backup_import_failed))
                summary.isEmpty -> toast(getString(R.string.backup_import_empty))
                else -> {
                    toast(
                        getString(
                            R.string.backup_imported,
                            summary.themes,
                            summary.favorites,
                            summary.icons,
                            summary.widgets,
                        ),
                    )
                    refreshSummary()
                    activity?.recreate()
                }
            }
        }
    }

    private fun removeCustomIcons() {
        val context = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val removed = withContext(Dispatchers.IO) {
                val files = StorageManager.clearIconFiles(context)
                FlyApp.repository.clearCustomIcons()
                files
            }
            if (_binding == null) return@launch
            val message = if (removed == 0) {
                getString(R.string.custom_icons_none)
            } else {
                getString(R.string.custom_icons_cleared, removed)
            }
            toast(message)
            refreshSummary()
        }
    }

    private fun confirmReset() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.reset_all_title)
            .setMessage(R.string.reset_all_message)
            .setPositiveButton(R.string.reset_all_confirm) { _, _ -> performReset() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun performReset() {
        val context = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                StorageManager.clearIconFiles(context)
                FlyApp.repository.clearAllData()
            }
            ThumbnailCache.clear()
            prefs.reset()
            if (_binding == null) return@launch
            Toast.makeText(requireContext(), R.string.reset_done, Toast.LENGTH_LONG).show()
            activity?.recreate()
        }
    }

    private fun refreshSummary() {
        val applies = FlyApp.repository.applyCount()
        val favorites = FlyApp.repository.favoriteCount()
        val themes = FlyApp.repository.themes().size
        val widgets = FlyApp.repository.pinnedWidgetCount()
        binding.txtDataSummary.text =
            getString(R.string.data_summary, applies, favorites, themes, widgets)

        val bytes = StorageManager.totalBytes(requireContext().applicationContext)
        binding.txtStorageSummary.text = getString(
            R.string.storage_summary,
            StorageManager.format(bytes),
            FlyApp.repository.customIconCount(),
        )
    }

    private fun toast(message: String) {
        val context = context ?: return
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
