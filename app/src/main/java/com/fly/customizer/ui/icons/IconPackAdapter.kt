package com.fly.customizer.ui.icons

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fly.customizer.R
import com.fly.customizer.databinding.ItemIconPackBinding
import com.fly.customizer.util.IconPackLoader

/**
 * Horizontal list of installed icon packs.
 */
class IconPackAdapter(
    private val onSelect: (IconPackLoader.IconPack) -> Unit,
) : RecyclerView.Adapter<IconPackAdapter.Holder>() {

    private var packs: List<IconPackLoader.IconPack> = emptyList()
    private var selectedPackage: String? = null

    fun submit(items: List<IconPackLoader.IconPack>, selected: String?) {
        packs = items
        selectedPackage = selected
        notifyDataSetChanged()
    }

    fun setSelected(packageName: String) {
        selectedPackage = packageName
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedPackage = null
        notifyDataSetChanged()
    }

    class Holder(val binding: ItemIconPackBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val inflater = LayoutInflater.from(parent.context)
        return Holder(ItemIconPackBinding.inflate(inflater, parent, false))
    }

    override fun getItemCount(): Int = packs.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val pack = packs[position]
        val binding = holder.binding
        binding.packName.text = pack.label
        binding.packIcon.setImageDrawable(pack.icon)
        binding.packCount.text = binding.root.context.getString(
            R.string.coverage_label, pack.themedAppCount, pack.totalAppCount,
        )
        val density = binding.root.resources.displayMetrics.density
        val selected = pack.packageName == selectedPackage
        binding.packCard.strokeWidth = ((if (selected) 2f else 1f) * density).toInt()
        binding.packCard.setStrokeColor(
            if (selected) {
                binding.root.context.getColor(R.color.accent_violet)
            } else {
                binding.root.context.getColor(R.color.outline_dark)
            },
        )
        binding.root.setOnClickListener { onSelect(pack) }
    }
}
