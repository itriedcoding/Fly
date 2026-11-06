package com.fly.customizer.ui.themes

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.fly.customizer.core.AccentSpec
import com.fly.customizer.databinding.ItemAccentBinding

/**
 * Horizontal accent swatch picker.
 */
class AccentAdapter(
    private val accents: List<AccentSpec>,
    private var selectedId: String,
    private val onSelect: (AccentSpec) -> Unit,
) : RecyclerView.Adapter<AccentAdapter.Holder>() {

    class Holder(val binding: ItemAccentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val inflater = LayoutInflater.from(parent.context)
        return Holder(ItemAccentBinding.inflate(inflater, parent, false))
    }

    override fun getItemCount(): Int = accents.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val accent = accents[position]
        val binding = holder.binding
        binding.accentSwatch.backgroundTintList = ColorStateList.valueOf(accent.color)
        binding.accentName.setText(accent.labelRes)
        val selected = accent.id == selectedId
        binding.accentCheck.isVisible = selected
        binding.accentCheck.imageTintList = ColorStateList.valueOf(accent.color)
        val density = binding.root.resources.displayMetrics.density
        binding.accentCard.strokeWidth = ((if (selected) 2f else 1f) * density).toInt()
        binding.accentCard.setStrokeColor(
            if (selected) accent.color else accent.color and 0x33FFFFFF,
        )
        binding.root.setOnClickListener {
            if (selectedId == accent.id) return@setOnClickListener
            selectedId = accent.id
            notifyDataSetChanged()
            onSelect(accent)
        }
    }

    fun updateSelection(accentId: String) {
        selectedId = accentId
        notifyDataSetChanged()
    }
}
