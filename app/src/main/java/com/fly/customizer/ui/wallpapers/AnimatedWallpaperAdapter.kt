package com.fly.customizer.ui.wallpapers

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fly.customizer.R
import com.fly.customizer.databinding.ItemAnimatedWallpaperBinding
import com.fly.customizer.util.AnimatedWallpaper
import com.fly.customizer.util.AnimatedWallpaperCatalog

/**
 * Grid of animated wallpapers, each with a live native preview.
 */
class AnimatedWallpaperAdapter(
    private val onApply: (AnimatedWallpaper) -> Unit,
) : RecyclerView.Adapter<AnimatedWallpaperAdapter.Holder>() {

    private var items: List<AnimatedWallpaper> = emptyList()
    private var activeId: String? = null

    fun submit(presets: List<AnimatedWallpaper>, active: String?) {
        items = presets
        activeId = active
        notifyDataSetChanged()
    }

    class Holder(val binding: ItemAnimatedWallpaperBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val inflater = LayoutInflater.from(parent.context)
        return Holder(ItemAnimatedWallpaperBinding.inflate(inflater, parent, false))
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        val binding = holder.binding
        binding.preview.setPreset(item)
        binding.textLabel.setText(item.labelRes)
        binding.textMeta.text = binding.root.context.getString(
            R.string.live_meta,
            WallpaperCatalogHelper.styleName(item.style),
            (1f / item.speed).toInt(),
        )
        binding.txtActive.visibility =
            if (item.id == activeId) android.view.View.VISIBLE else android.view.View.GONE
        binding.root.setOnClickListener { onApply(item) }
    }
}

/**
 * Friendly names for the native animated styles, used in the wallpaper grid caption.
 */
object WallpaperCatalogHelper {

    fun styleName(style: Int): String = when (style) {
        0 -> "curtains"
        1 -> "rings"
        2 -> "orbit"
        3 -> "plasma"
        4 -> "comet"
        else -> "caustics"
    }

    /** Safe lookup used by the dashboard status card. */
    fun labelOf(id: String?): String? {
        val preset = AnimatedWallpaperCatalog.preset(id) ?: return null
        return preset.id
    }
}
