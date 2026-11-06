package com.fly.customizer.ui.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fly.customizer.FlyApp
import com.fly.customizer.R
import com.fly.customizer.databinding.FragmentWidgetsBinding
import com.fly.customizer.widgets.WidgetPinReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Lists every widget provider reported by AppWidgetManager, requests pinning with a real success
 * callback and shows how many of them are live on the home screen.
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

        adapter = WidgetAdapter { item -> pinWidget(item) }
        binding.widgetList.layoutManager = LinearLayoutManager(requireContext())
        binding.widgetList.adapter = adapter

        loadProviders()
        refreshSummary()
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
                            val active = activeCount(info)
                            val base = context.getString(
                                R.string.widget_sizes,
                                (info.minWidth / 2).coerceAtLeast(1),
                                (info.minHeight / 2).coerceAtLeast(1),
                                if (info.resizeMode != AppWidgetProviderInfo.RESIZE_NONE) {
                                    context.getString(R.string.widget_resizable)
                                } else {
                                    context.getString(R.string.widget_fixed)
                                },
                            )
                            WidgetAdapter.Item(
                                info = info,
                                label = label,
                                preview = preview,
                                meta = if (active > 0) {
                                    "$base · ${context.getString(R.string.widgets_active, active)}"
                                } else {
                                    base
                                },
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
            refreshSummary()
        }
    }

    private fun activeCount(info: AppWidgetProviderInfo): Int {
        val provider = info.provider ?: return 0
        return try {
            manager.getAppWidgetIds(provider).size
        } catch (error: SecurityException) {
            Log.w(TAG, "Cannot read widget ids for $provider", error)
            0
        }
    }

    private fun refreshSummary() {
        if (_binding == null) return
        val pinned = FlyApp.repository.pinnedWidgetCount()
        var active = 0
        try {
            for (info in manager.installedProviders) {
                active += activeCount(info)
            }
        } catch (error: Exception) {
            Log.w(TAG, "Unable to count installed providers", error)
        }
        binding.tvPinnedSummary.text =
            getString(R.string.widgets_pinned_summary, pinned, active)
    }

    private fun pinWidget(item: WidgetAdapter.Item) {
        val provider = item.info.provider ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Toast.makeText(requireContext(), R.string.widget_pin_old_android, Toast.LENGTH_LONG).show()
            return
        }
        if (!pinSupported) {
            Toast.makeText(requireContext(), R.string.widget_pin_unsupported, Toast.LENGTH_LONG).show()
            return
        }

        // The system fills in EXTRA_APPWIDGET_ID on this intent once the launcher pins the widget.
        val callbackIntent = Intent(requireContext(), WidgetPinReceiver::class.java)
            .setAction(WidgetPinReceiver.ACTION_PINNED)
            .putExtra(WidgetPinReceiver.EXTRA_PROVIDER, provider.flattenToString())
            .putExtra(WidgetPinReceiver.EXTRA_LABEL, item.label)
        val callback = PendingIntent.getBroadcast(
            requireContext(),
            provider.hashCode(),
            callbackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )

        val requested = try {
            manager.requestPinAppWidget(provider, null, callback)
        } catch (error: Exception) {
            Log.e(TAG, "Pin request failed for $provider", error)
            false
        }
        if (!requested) {
            Toast.makeText(requireContext(), R.string.widget_pin_declined, Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(requireContext(), R.string.widget_pin_requested, Toast.LENGTH_LONG).show()
        binding.root.postDelayed({
            if (_binding != null) {
                loadProviders()
                refreshSummary()
            }
        }, 1500L)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "WidgetsFragment"
    }
}
