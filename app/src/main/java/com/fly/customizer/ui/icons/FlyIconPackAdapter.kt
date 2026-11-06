package com.fly.customizer.ui.icons

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fly.customizer.R
import com.fly.customizer.databinding.ItemIconPackBinding
import com.fly.customizer.util.FlyIconPack
import com.fly.customizer.util.BrandIcons
import com.fly.customizer.util.FlyIconPacks
import com.fly.customizer.util.FlyIconRenderer
import com.google.android.material.color.MaterialColors

/**
 * Horizontal list of Fly's built-in icon packs, each showing a real rendered sample icon.
 */
class FlyIconPackAdapter(
    private val onSelect: (FlyIconPack) -> Unit,
) : RecyclerView.Adapter<FlyIconPackAdapter.Holder>() {

    private var packs: List<FlyIconPack> = FlyIconPacks.all
    private var selectedId: String? = null
    private var appCount: Int = 0
    private var sampleLabel: String = "Fly"

    fun submit(items: List<FlyIconPack>, selected: String?, apps: Int, sample: String) {
        packs = items
        selectedId = selected
        appCount = apps
        sampleLabel = sample
        notifyDataSetChanged()
    }

    fun setSelected(id: String?) {
        selectedId = id
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
        binding.packName.setText(pack.labelRes)
        val brandSample = if (pack.brandAware) BrandIcons.brand("youtube") else null
        binding.packIcon.setImageBitmap(
            FlyIconRenderer.render(
                pack,
                brandSample?.let { binding.root.context.getString(it.labelRes) } ?: sampleLabel,
                brandSample?.id ?: "sample",
                128,
                brandSample?.packages?.first(),
            ),
        )
        binding.packCount.text = binding.root.context.getString(R.string.fly_pack_meta, appCount)

        val density = binding.root.resources.displayMetrics.density
        val selected = pack.id == selectedId
        binding.packCard.strokeWidth = ((if (selected) 2f else 1f) * density).toInt()
        binding.packCard.setStrokeColor(
            if (selected) {
                MaterialColors.getColor(
                    binding.packCard,
                    com.google.android.material.R.attr.colorPrimary,
                )
            } else {
                MaterialColors.getColor(
                    binding.packCard,
                    com.google.android.material.R.attr.colorOutlineVariant,
                )
            },
        )
        binding.root.setOnClickListener { onSelect(pack) }
    }
}
