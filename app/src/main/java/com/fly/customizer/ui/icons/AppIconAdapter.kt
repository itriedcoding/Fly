package com.fly.customizer.ui.icons

import android.graphics.Bitmap
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
            onLongPress(app)
            true
        }
    }
}
