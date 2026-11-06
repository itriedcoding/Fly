package com.fly.customizer.ui.icons

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fly.customizer.databinding.ItemAppIconBinding
import com.fly.customizer.util.IconPackLoader

/**
 * Grid of launchable apps showing the icon resolved through the selected pack.
 */
class AppIconAdapter(
    private val onSelect: (IconPackLoader.AppEntry) -> Unit,
) : RecyclerView.Adapter<AppIconAdapter.Holder>() {

    private var apps: List<IconPackLoader.AppEntry> = emptyList()

    fun submit(items: List<IconPackLoader.AppEntry>) {
        apps = items
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
        holder.binding.appLabel.text = app.label
        holder.binding.appIcon.setImageDrawable(app.themedIcon ?: app.icon)
        holder.binding.root.setOnClickListener { onSelect(app) }
    }
}
