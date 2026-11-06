package com.fly.customizer.ui.icons

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.fly.customizer.FlyApp
import com.fly.customizer.R
import com.fly.customizer.compat.HomeShortcutCompat
import com.fly.customizer.databinding.FragmentIconsBinding
import com.fly.customizer.util.CustomIconStore
import com.fly.customizer.util.IconPackLoader
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Icon browser: detects packs, parses appfilter.xml, searches apps, assigns user-picked custom
 * icons and pins themed shortcuts.
 */
class IconsFragment : Fragment() {

    private var _binding: FragmentIconsBinding? = null
    private val binding get() = _binding!!

    private lateinit var loader: IconPackLoader
    private lateinit var packAdapter: IconPackAdapter
    private lateinit var appAdapter: AppIconAdapter

    private var apps: List<IconPackLoader.AppEntry> = emptyList()
    private var displayedApps: List<IconPackLoader.AppEntry> = emptyList()
    private var packs: List<IconPackLoader.IconPack> = emptyList()
    private var selectedPack: IconPackLoader.IconPack? = null
    private var customIcons: Map<String, Bitmap> = emptyMap()
    private var pendingApp: IconPackLoader.AppEntry? = null

    private val pickIcon = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        handlePickedIcon(uri)
    }

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

        appAdapter = AppIconAdapter(
            onSelect = { app -> pinShortcut(app) },
            onLongPress = { app -> showAppOptions(app) },
        )
        val iconPrefs = FlyApp.prefs
        appAdapter.compact = iconPrefs.compactIcons
        appAdapter.haptics = iconPrefs.haptics
        binding.appIconList.layoutManager =
            GridLayoutManager(requireContext(), if (iconPrefs.compactIcons) 5 else 4)
        binding.appIconList.adapter = appAdapter

        binding.inputSearch.doAfterTextChanged { applySearchFilter() }

        loadPacks()
        reloadCustomIcons()
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
                submitApps(apps)
                return@launch
            }
            val remembered = FlyApp.repository.lastIconPack()
            val initial = packs.firstOrNull { it.packageName == remembered } ?: packs.first()
            selectPack(initial)
        }
    }

    private fun reloadCustomIcons() {
        viewLifecycleOwner.lifecycleScope.launch {
            customIcons = loadCustomIcons()
            if (_binding == null) return@launch
            applySearchFilter()
        }
    }

    private suspend fun loadCustomIcons(): Map<String, Bitmap> = withContext(Dispatchers.IO) {
        val loaded = LinkedHashMap<String, Bitmap>()
        for ((component, path) in FlyApp.repository.customIcons()) {
            val bitmap = CustomIconStore.load(path) ?: continue
            loaded[component] = bitmap
        }
        loaded
    }

    private fun handlePickedIcon(uri: android.net.Uri?) {
        val app = pendingApp
        pendingApp = null
        if (uri == null || app == null) return
        val context = context ?: return
        lifecycleScope.launch {
            val path = withContext(Dispatchers.IO) {
                CustomIconStore.save(context.applicationContext, app.component, uri)
            }
            if (_binding == null) return@launch
            if (path == null) {
                toast(getString(R.string.custom_icon_failed))
                return@launch
            }
            val previous = FlyApp.repository.customIcons()[app.component]
            FlyApp.repository.setCustomIcon(app.component, path)
            if (previous != null && previous != path) {
                withContext(Dispatchers.IO) { CustomIconStore.delete(previous) }
            }
            customIcons = loadCustomIcons()
            if (_binding == null) return@launch
            applySearchFilter()
            toast(getString(R.string.custom_icon_saved, app.label))
        }
    }

    private fun showAppOptions(app: IconPackLoader.AppEntry) {
        val hasCustom = FlyApp.repository.customIcons().containsKey(app.component)
        val options = ArrayList<String>(3)
        options.add(getString(R.string.custom_icon_set))
        if (hasCustom) options.add(getString(R.string.custom_icon_remove))
        options.add(getString(R.string.pin_shortcut))

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(app.label)
            .setItems(options.toTypedArray()) { _, which ->
                when {
                    which == 0 -> {
                        pendingApp = app
                        pickIcon.launch("image/*")
                    }
                    hasCustom && which == 1 -> removeCustomIcon(app)
                    else -> pinShortcut(app)
                }
            }
            .show()
    }

    private fun removeCustomIcon(app: IconPackLoader.AppEntry) {
        val path = FlyApp.repository.customIcons()[app.component]
        FlyApp.repository.deleteCustomIcon(app.component)
        if (path != null) {
            val context = context?.applicationContext
            if (context != null) {
                lifecycleScope.launch(Dispatchers.IO) { CustomIconStore.delete(path) }
            }
        }
        customIcons = customIcons - app.component
        applySearchFilter()
        toast(getString(R.string.custom_icon_removed, app.label))
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
            submitApps(themed)
        }
    }

    private fun submitApps(entries: List<IconPackLoader.AppEntry>) {
        displayedApps = entries
        applySearchFilter()
    }

    private fun applySearchFilter() {
        val binding = _binding ?: return
        val query = binding.inputSearch.text?.toString()?.trim().orEmpty()
        val filtered = if (query.isEmpty()) {
            displayedApps
        } else {
            displayedApps.filter { it.label.contains(query, ignoreCase = true) }
        }
        appAdapter.submit(filtered, customIcons)
        binding.txtSearchEmpty.isVisible = filtered.isEmpty() && displayedApps.isNotEmpty()
    }

    private fun pinShortcut(app: IconPackLoader.AppEntry) {
        val custom = customIcons[app.component]?.takeIf { !it.isRecycled }?.let {
            BitmapDrawable(resources, it)
        }
        val icon = custom ?: app.themedIcon ?: app.icon
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

    private fun toast(message: String) {
        val context = context ?: return
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
