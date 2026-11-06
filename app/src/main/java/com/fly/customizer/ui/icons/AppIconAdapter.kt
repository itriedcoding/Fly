package com.fly.customizer.ui.icons

import android.graphics.Bitmap
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fly.customizer.databinding.ItemAppIconBinding
import com.fly.customizer.util.FlyIconPack
import com.fly.customizer.util.FlyIconRenderer
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

    /** When set, icons are drawn by Fly's built-in pack instead of the system or a third party. */
    var flyPack: FlyIconPack? = null

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
        val density = binding.root.resources.displayMetrics.density
        val iconSize = ((if (compact) 40 else 52) * density).toInt()
        val custom = customIcons[app.component]
        val pack = flyPack
        when {
            custom != null && !custom.isRecycled -> binding.appIcon.setImageBitmap(custom)
            pack != null -> binding.appIcon.setImageBitmap(
                FlyIconRenderer.render(pack, app.label, app.component, iconSize, app.packageName),
            )
            else -> binding.appIcon.setImageDrawable(app.themedIcon ?: app.icon)
        }
        binding.root.setOnClickListener { onSelect(app) }
        binding.root.setOnLongClickListener {
            if (haptics) {
                binding.root.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
            onLongPress(app)
            true
        }

        binding.appIcon.layoutParams = binding.appIcon.layoutParams.apply {
            width = iconSize
            height = iconSize
        }
    }
}
