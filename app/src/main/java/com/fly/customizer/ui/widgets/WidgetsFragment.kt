package com.fly.customizer.ui.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fly.customizer.R
import com.fly.customizer.databinding.FragmentWidgetsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Lists every widget provider reported by AppWidgetManager and requests pinning.
 */
class WidgetsFragment : Fragment() {

    private var _binding: FragmentWidgetsBinding? = null
    private val binding get() = _binding!!

    private lateinit var manager: AppWidgetManager
    private lateinit var adapter: WidgetAdapter
    private var pinSupported = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentWidgetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        manager = AppWidgetManager.getInstance(requireContext())
        pinSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            manager.isRequestPinAppWidgetSupported
        binding.txtWidgetSupport.setText(
            if (pinSupported) R.string.widgets_supported else R.string.widget_pin_unsupported,
        )

        adapter = WidgetAdapter { info -> pinWidget(info) }
        binding.widgetList.layoutManager = LinearLayoutManager(requireContext())
        binding.widgetList.adapter = adapter

        loadProviders()
    }

    private fun loadProviders() {
        val context = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val entries = withContext(Dispatchers.IO) {
                val providers = manager.installedProviders
                providers
                    .filter { it.provider != null }
                    .mapNotNull { info ->
                        try {
                            val label = info.loadLabel(context.packageManager)
                            val density = context.resources.displayMetrics.densityDpi
                            val preview = try {
                                info.loadPreviewImage(context, density)
                            } catch (error: Exception) {
                                null
                            }
                            WidgetAdapter.Item(
                                info = info,
                                label = label,
                                preview = preview,
                                meta = context.getString(
                                    R.string.widget_sizes,
                                    (info.minWidth / 2).coerceAtLeast(1),
                                    (info.minHeight / 2).coerceAtLeast(1),
                                    if (info.resizeMode != AppWidgetProviderInfo.RESIZE_NONE) {
                                        context.getString(R.string.widget_resizable)
                                    } else {
                                        context.getString(R.string.widget_fixed)
                                    },
                                ),
                            )
                        } catch (error: Exception) {
                            null
                        }
                    }
                    .sortedBy { it.label.lowercase() }
            }
            if (_binding == null) return@launch
            adapter.submit(entries)
            binding.txtWidgetsEmpty.isVisible = entries.isEmpty()
        }
    }

    private fun pinWidget(info: AppWidgetProviderInfo) {
        if (!pinSupported) {
            Toast.makeText(requireContext(), R.string.widget_pin_unsupported, Toast.LENGTH_LONG).show()
            return
        }
        val requested = manager.requestPinAppWidget(info.provider, null, null)
        Toast.makeText(
            requireContext(),
            if (requested) R.string.widget_pin_requested else R.string.widget_pin_unsupported,
            Toast.LENGTH_LONG,
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
