package com.fly.customizer.ui.icons

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
import com.fly.customizer.compat.HomeShortcutCompat
import com.fly.customizer.databinding.FragmentIconsBinding
import com.fly.customizer.util.IconPackLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Icon pack browser: detects packs, parses appfilter.xml and pins themed shortcuts.
 */
class IconsFragment : Fragment() {

    private var _binding: FragmentIconsBinding? = null
    private val binding get() = _binding!!

    private lateinit var loader: IconPackLoader
    private lateinit var packAdapter: IconPackAdapter
    private lateinit var appAdapter: AppIconAdapter

    private var apps: List<IconPackLoader.AppEntry> = emptyList()
    private var packs: List<IconPackLoader.IconPack> = emptyList()
    private var selectedPack: IconPackLoader.IconPack? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentIconsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loader = IconPackLoader(requireContext().applicationContext)

        packAdapter = IconPackAdapter { pack -> selectPack(pack) }
        binding.packList.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.packList.adapter = packAdapter

        appAdapter = AppIconAdapter { app -> pinShortcut(app) }
        binding.appIconList.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.appIconList.adapter = appAdapter

        loadPacks()
    }

    private fun loadPacks() {
        binding.iconProgress.isVisible = true
        binding.txtSelectedPack.text = getString(R.string.loading)
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                val installedApps = loader.launchableApps()
                installedApps to loader.installedPacks(installedApps)
            }
            if (_binding == null) return@launch
            apps = result.first
            packs = result.second
            binding.iconProgress.isVisible = false
            binding.txtPacksEmpty.isVisible = packs.isEmpty()
            binding.packList.isVisible = packs.isNotEmpty()
            packAdapter.submit(packs, null)

            if (packs.isEmpty()) {
                binding.txtSelectedPack.text = getString(R.string.app_icons_label)
                binding.txtCoverage.text = ""
                appAdapter.submit(apps)
                return@launch
            }
            val remembered = FlyApp.repository.lastIconPack()
            val initial = packs.firstOrNull { it.packageName == remembered } ?: packs.first()
            selectPack(initial)
        }
    }

    private fun selectPack(pack: IconPackLoader.IconPack) {
        selectedPack = pack
        packAdapter.setSelected(pack.packageName)
        binding.txtSelectedPack.text = getString(R.string.selected_pack, pack.label)
        binding.iconProgress.isVisible = true
        val snapshot = apps
        viewLifecycleOwner.lifecycleScope.launch {
            val themed = withContext(Dispatchers.IO) {
                loader.applyPack(pack.packageName, snapshot)
            }
            if (_binding == null) return@launch
            binding.iconProgress.isVisible = false
            val covered = themed.count { it.themedIcon != null }
            binding.txtCoverage.text = getString(R.string.coverage_label, covered, themed.size)
            appAdapter.submit(themed)
        }
    }

    private fun pinShortcut(app: IconPackLoader.AppEntry) {
        val icon = app.themedIcon ?: app.icon
        if (icon == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.pin_failed, getString(R.string.cd_app_icon)),
                Toast.LENGTH_LONG,
            ).show()
            return
        }
        HomeShortcutCompat.pinShortcut(
            requireContext(),
            app.packageName,
            app.className,
            app.label,
            icon,
            object : HomeShortcutCompat.Callback {
                override fun onResult(success: Boolean, message: String) {
                    if (!isAdded) return
                    Toast.makeText(
                        requireContext(),
                        if (success) getString(R.string.pin_success)
                        else getString(R.string.pin_failed, message),
                        Toast.LENGTH_LONG,
                    ).show()
                    if (success) {
                        selectedPack?.let { FlyApp.repository.recordShortcutPin(it.packageName) }
                    }
                }
            },
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
