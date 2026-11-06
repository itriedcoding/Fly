package com.fly.customizer.ui.themes

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.fly.customizer.FlyApp
import com.fly.customizer.R
import com.fly.customizer.core.BaseMode
import com.fly.customizer.core.Prefs
import com.fly.customizer.core.ThemeCatalog
import com.fly.customizer.core.ThemePreset
import com.fly.customizer.data.CustomTheme
import com.fly.customizer.databinding.FragmentThemesBinding
import com.fly.customizer.util.WallpaperAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Theme studio: base surface, twelve accents, sixteen stock themes, wallpaper matching
 * and user themes stored in SQLite.
 */
class ThemesFragment : Fragment() {

    private var _binding: FragmentThemesBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: Prefs
    private lateinit var accentAdapter: AccentAdapter
    private lateinit var stockAdapter: StockThemeAdapter
    private lateinit var savedAdapter: SavedThemeAdapter
    private var suppressCallbacks = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentThemesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = FlyApp.prefs

        binding.baseGroup.check(
            when (BaseMode.from(prefs.baseMode)) {
                BaseMode.LIGHT -> R.id.btnModeLight
                BaseMode.DARK -> R.id.btnModeDark
                BaseMode.AMOLED -> R.id.btnModeAmoled
            },
        )
        binding.baseGroup.addOnButtonCheckedListener { _, buttonId, isChecked ->
            if (!isChecked || suppressCallbacks) return@addOnButtonCheckedListener
            val mode = when (buttonId) {
                R.id.btnModeLight -> BaseMode.LIGHT
                R.id.btnModeDark -> BaseMode.DARK
                else -> BaseMode.AMOLED
            }
            if (mode.id == prefs.baseMode) return@addOnButtonCheckedListener
            prefs.baseMode = mode.id
            applyAndRecreate()
        }

        binding.dynamicSwitch.isEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        binding.dynamicSwitch.isChecked = prefs.dynamicColor
        binding.dynamicSwitch.setOnCheckedChangeListener { _, checked ->
            if (suppressCallbacks || checked == prefs.dynamicColor) return@setOnCheckedChangeListener
            prefs.dynamicColor = checked
            applyAndRecreate()
        }

        accentAdapter = AccentAdapter(ThemeCatalog.accents, prefs.accentId) { accent ->
            prefs.accentId = accent.id
            applyAndRecreate()
        }
        binding.accentList.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.accentList.adapter = accentAdapter

        stockAdapter = StockThemeAdapter(
            presets = ThemeCatalog.stockThemes,
            selectedId = currentStockThemeId(),
            onSelect = { preset -> applyStockTheme(preset) },
        )
        binding.stockThemeList.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.stockThemeList.adapter = stockAdapter

        binding.btnMatchWallpaper.setOnClickListener { matchWallpaperAccent() }

        savedAdapter = SavedThemeAdapter(
            onApply = { theme -> applyTheme(theme) },
            onDelete = { theme -> deleteTheme(theme) },
        )
        binding.savedThemeList.layoutManager = LinearLayoutManager(requireContext())
        binding.savedThemeList.adapter = savedAdapter

        binding.btnSaveTheme.setOnClickListener { saveTheme() }

        refreshSavedThemes()
        binding.root.post { suppressCallbacks = false }
    }

    private fun currentStockThemeId(): String? = ThemeCatalog.stockThemes
        .firstOrNull { it.baseMode.id == prefs.baseMode && it.accentId == prefs.accentId }
        ?.id

    private fun applyAndRecreate() {
        suppressCallbacks = true
        activity?.recreate()
    }

    private fun applyStockTheme(preset: ThemePreset) {
        prefs.baseMode = preset.baseMode.id
        prefs.accentId = preset.accentId
        Toast.makeText(
            requireContext(),
            getString(R.string.stock_theme_applied, getString(preset.labelRes)),
            Toast.LENGTH_SHORT,
        ).show()
        applyAndRecreate()
    }

    private fun matchWallpaperAccent() {
        binding.btnMatchWallpaper.isEnabled = false
        binding.btnMatchWallpaper.setText(R.string.loading)
        val context = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val color = withContext(Dispatchers.IO) { WallpaperAccent.dominantColor(context) }
            if (_binding == null) return@launch
            binding.btnMatchWallpaper.isEnabled = true
            binding.btnMatchWallpaper.setText(R.string.btn_match_wallpaper)
            if (color == null) {
                Toast.makeText(
                    requireContext(),
                    R.string.accent_wallpaper_failed,
                    Toast.LENGTH_LONG,
                ).show()
                return@launch
            }
            val accent = ThemeCatalog.nearestAccent(color)
            prefs.accentId = accent.id
            Toast.makeText(
                requireContext(),
                getString(R.string.accent_wallpaper_applied, getString(accent.labelRes)),
                Toast.LENGTH_SHORT,
            ).show()
            applyAndRecreate()
        }
    }

    private fun applyTheme(theme: CustomTheme) {
        prefs.baseMode = theme.baseMode
        prefs.accentId = theme.accentId
        applyAndRecreate()
    }

    private fun saveTheme() {
        val name = binding.inputThemeName.text?.toString()?.trim().orEmpty()
        if (name.isEmpty()) {
            binding.themeNameLayout.error = getString(R.string.theme_name_required)
            return
        }
        binding.themeNameLayout.error = null
        FlyApp.repository.saveTheme(name, prefs.baseMode, prefs.accentId)
        binding.inputThemeName.setText("")
        refreshSavedThemes()
        Toast.makeText(requireContext(), R.string.theme_saved, Toast.LENGTH_SHORT).show()
    }

    private fun deleteTheme(theme: CustomTheme) {
        FlyApp.repository.deleteTheme(theme.id)
        refreshSavedThemes()
        Toast.makeText(requireContext(), R.string.theme_deleted, Toast.LENGTH_SHORT).show()
    }

    private fun refreshSavedThemes() {
        val themes = FlyApp.repository.themes()
        savedAdapter.submit(themes)
        binding.txtSavedEmpty.isVisible = themes.isEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
