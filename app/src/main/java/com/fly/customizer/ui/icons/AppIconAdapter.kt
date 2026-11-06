package com.fly.customizer.ui.icons

import android.graphics.Bitmap
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fly.customizer.databinding.ItemAppIconBinding
import com.fly.customizer.util.IconPackLoader

/**
 * Grid of launchable apps showing either the user's custom icon or the icon resolved through the
 * selected pack.
 */
class AppIconAdapter(
    private val onSelect: (IconPackLoader.AppEntry) -> Unit,
    private val onLongPress: (IconPackLoader.AppEntry) -> Unit,
) : RecyclerView.Adapter<AppIconAdapter.Holder>() {

    private var apps: List<IconPackLoader.AppEntry> = emptyList()
    private var customIcons: Map<String, Bitmap> = emptyMap()

    /** Smaller icons for the five column layout. */
    var compact: Boolean = false

    /** Vibrate on long-press, following the settings toggle. */
    var haptics: Boolean = true

    fun submit(items: List<IconPackLoader.AppEntry>, icons: Map<String, Bitmap>) {
        apps = items
        customIcons = icons
        notifyDataSetChanged()
    }

    class Holder(val binding: ItemAppIconBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val inflater = LayoutInflater.from(parent.context)
        return Holder(ItemAppIconBinding.inflate(inflater, parent, false))
    }

    override fun getItemCount(): Int = apps.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val app = apps[position]
        val binding = holder.binding
        binding.appLabel.text = app.label
        val custom = customIcons[app.component]
        if (custom != null && !custom.isRecycled) {
            binding.appIcon.setImageBitmap(custom)
        } else {
            binding.appIcon.setImageDrawable(app.themedIcon ?: app.icon)
        }
        binding.root.setOnClickListener { onSelect(app) }
        binding.root.setOnLongClickListener {
            if (haptics) {
                binding.root.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
            onLongPress(app)
            true
        }

        val density = binding.root.resources.displayMetrics.density
        val iconSize = ((if (compact) 40 else 52) * density).toInt()
        binding.appIcon.layoutParams = binding.appIcon.layoutParams.apply {
            width = iconSize
            height = iconSize
        }
    }
}
