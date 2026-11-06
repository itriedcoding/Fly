package com.fly.customizer.ui.widgets

import android.appwidget.AppWidgetProviderInfo
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fly.customizer.R
import com.fly.customizer.databinding.ItemWidgetBinding

/**
 * Installed widget providers with preview images and live home-screen counts.
 */
class WidgetAdapter(
    private val onPin: (Item) -> Unit,
) : RecyclerView.Adapter<WidgetAdapter.Holder>() {

    data class Item(
        val info: AppWidgetProviderInfo,
        val label: String,
        val preview: Drawable?,
        val meta: String,
    )

    private var items: List<Item> = emptyList()

    fun submit(entries: List<Item>) {
        items = entries
        notifyDataSetChanged()
    }

    class Holder(val binding: ItemWidgetBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val inflater = LayoutInflater.from(parent.context)
        return Holder(ItemWidgetBinding.inflate(inflater, parent, false))
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        val binding = holder.binding
        binding.widgetName.text = item.label
        binding.widgetMeta.text = item.meta
        if (item.preview != null) {
            binding.widgetPreview.setImageDrawable(item.preview)
        } else {
            binding.widgetPreview.setImageResource(R.drawable.ic_widgets)
        }
        binding.btnPinWidget.setOnClickListener { onPin(item) }
    }
}
