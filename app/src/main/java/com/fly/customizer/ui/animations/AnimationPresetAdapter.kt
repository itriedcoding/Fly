package com.fly.customizer.ui.animations

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fly.customizer.databinding.ItemAnimationChipBinding

/**
 * Horizontal list of animation presets with a selected state.
 */
class AnimationPresetAdapter(
    private val presets: List<AnimationPreset>,
    private var selectedId: String,
    private val onSelect: (AnimationPreset) -> Unit,
) : RecyclerView.Adapter<AnimationPresetAdapter.Holder>() {

    class Holder(val binding: ItemAnimationChipBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val inflater = LayoutInflater.from(parent.context)
        return Holder(ItemAnimationChipBinding.inflate(inflater, parent, false))
    }

    override fun getItemCount(): Int = presets.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val preset = presets[position]
        val binding = holder.binding
        val context = binding.root.context
        binding.chipLabel.setText(preset.labelRes)
        val selected = preset.id == selectedId
        val accent = resolveAccent(binding)
        binding.chipCard.strokeWidth =
            ((if (selected) 2f else 1f) * binding.root.resources.displayMetrics.density).toInt()
        binding.chipCard.setStrokeColor(if (selected) accent else 0x33808080)
        binding.chipCard.setCardBackgroundColor(
            if (selected) 0x227C5CFF else resolveSurface(binding),
        )
        binding.chipLabel.setTextColor(if (selected) accent else resolveOnSurface(binding))
        binding.root.setOnClickListener {
            if (selectedId == preset.id) return@setOnClickListener
            selectedId = preset.id
            notifyDataSetChanged()
            onSelect(preset)
        }
    }

    fun updateSelection(presetId: String) {
        selectedId = presetId
        notifyDataSetChanged()
    }

    private fun resolveAccent(binding: ItemAnimationChipBinding): Int =
        themeColor(binding.root, com.google.android.material.R.attr.colorPrimary, 0xFF7C5CFF.toInt())

    private fun resolveOnSurface(binding: ItemAnimationChipBinding): Int =
        themeColor(binding.root, com.google.android.material.R.attr.colorOnSurface, 0xFFFFFFFF.toInt())

    private fun resolveSurface(binding: ItemAnimationChipBinding): Int =
        themeColor(
            binding.root,
            com.google.android.material.R.attr.colorSurfaceContainerHigh,
            0xFF1F1F2A.toInt(),
        )

    private fun themeColor(
        view: android.view.View,
        attribute: Int,
        fallback: Int,
    ): Int {
        val values = android.util.TypedValue()
        val found = view.context.theme.resolveAttribute(attribute, values, true)
        return if (found) values.data else fallback
    }
}
