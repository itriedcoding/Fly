package com.fly.customizer.ui.wallpapers

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.fly.customizer.R
import com.fly.customizer.databinding.ItemWallpaperBinding
import com.fly.customizer.util.ThumbnailCache
import com.fly.customizer.util.WallpaperPreset
import com.fly.customizer.util.WallpaperStudio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Renders wallpaper previews natively and reports apply/favourite taps.
 */
class WallpaperAdapter(
    private val scope: CoroutineScope,
    private val onApply: (WallpaperPreset) -> Unit,
    private val onFavorite: (WallpaperPreset) -> Unit,
) : RecyclerView.Adapter<WallpaperAdapter.Holder>() {

    private data class Item(
        val preset: WallpaperPreset,
        val hueShift: Float,
        val favorite: Boolean,
    )

    private var items: List<Item> = emptyList()

    fun submit(presets: List<WallpaperPreset>, hueShift: Float, favorites: Set<String>) {
        items = presets.map { Item(it, hueShift, favorites.contains(it.id)) }
        notifyDataSetChanged()
    }

    class Holder(val binding: ItemWallpaperBinding) : RecyclerView.ViewHolder(binding.root) {
        var pendingKey: String? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val inflater = LayoutInflater.from(parent.context)
        return Holder(ItemWallpaperBinding.inflate(inflater, parent, false))
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        val binding = holder.binding

        binding.textLabel.setText(item.preset.labelRes)
        binding.btnFavorite.imageTintList = ColorStateList.valueOf(
            if (item.favorite) {
                binding.root.context.getColor(R.color.accent_amber)
            } else {
                binding.root.context.getColor(R.color.accent_on)
            },
        )
        binding.root.setOnClickListener { onApply(item.preset) }
        binding.btnFavorite.setOnClickListener { onFavorite(item.preset) }

        val key = ThumbnailCache.key(
            item.preset.id, item.hueShift, WIDTH, HEIGHT,
        )
        holder.pendingKey = key
        val cached = ThumbnailCache.get(key)
        if (cached != null && !cached.isRecycled) {
            binding.imagePreview.setImageBitmap(cached)
            return
        }
        binding.imagePreview.setImageDrawable(null)
        scope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                ThumbnailCache.get(key) ?: WallpaperStudio.render(
                    item.preset, WIDTH, HEIGHT, item.hueShift,
                ).also { ThumbnailCache.put(key, it) }
            }
            if (holder.pendingKey == key && !bitmap.isRecycled) {
                binding.imagePreview.isVisible = true
                binding.imagePreview.setImageBitmap(bitmap)
            }
        }
    }

    private companion object {
        const val WIDTH = 480
        const val HEIGHT = 900
    }
}
