package com.fly.customizer.ui.themes

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fly.customizer.R
import com.fly.customizer.core.ThemeCatalog
import com.fly.customizer.core.ThemePreset
import com.fly.customizer.databinding.ItemStockThemeBinding

/**
 * Grid of ready-made base + accent combinations.
 */
class StockThemeAdapter(
    private val presets: List<ThemePreset>,
    private var selectedId: String?,
    private val onSelect: (ThemePreset) -> Unit,
) : RecyclerView.Adapter<StockThemeAdapter.Holder>() {

    class Holder(val binding: ItemStockThemeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val inflater = LayoutInflater.from(parent.context)
        return Holder(ItemStockThemeBinding.inflate(inflater, parent, false))
    }

    override fun getItemCount(): Int = presets.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val preset = presets[position]
        val accent = ThemeCatalog.accent(preset.accentId)
        val binding = holder.binding
        val context = binding.root.context

        binding.stockName.setText(preset.labelRes)
        binding.stockMeta.text = context.getString(
            R.string.theme_meta,
            context.getString(preset.baseMode.labelRes),
            context.getString(accent.labelRes),
        )
        binding.stockBase.backgroundTintList = ColorStateList.valueOf(preset.baseMode.surfaceColor)
        binding.stockAccentDot.backgroundTintList = ColorStateList.valueOf(accent.color)

        val selected = preset.id == selectedId
        val density = binding.root.resources.displayMetrics.density
        binding.stockCard.strokeWidth = ((if (selected) 2f else 1f) * density).toInt()
        binding.stockCard.setStrokeColor(if (selected) accent.color else 0x33808080)

        binding.root.setOnClickListener {
            if (selectedId == preset.id) return@setOnClickListener
            selectedId = preset.id
            notifyDataSetChanged()
            onSelect(preset)
        }
    }
}
