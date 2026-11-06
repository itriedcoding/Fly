package com.fly.customizer.ui.themes

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.fly.customizer.FlyApp
import com.fly.customizer.R
import com.fly.customizer.core.AccentSpec
import com.fly.customizer.core.BaseMode
import com.fly.customizer.core.Prefs
import com.fly.customizer.core.ThemeCatalog
import com.fly.customizer.data.CustomTheme
import com.fly.customizer.databinding.FragmentThemesBinding

/**
 * Theme studio: base surface, accent colour, dynamic colour and saved themes in SQLite.
 */
class ThemesFragment : Fragment() {

    private var _binding: FragmentThemesBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: Prefs
    private lateinit var accentAdapter: AccentAdapter
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

    private fun applyAndRecreate() {
        suppressCallbacks = true
        activity?.recreate()
    }

    private fun applyTheme(theme: CustomTheme) {
        prefs.baseMode = theme.baseMode
        prefs.accentId = theme.accentId
        suppressCallbacks = true
        activity?.recreate()
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
