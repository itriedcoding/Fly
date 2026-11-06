package com.fly.customizer.ui.themes

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fly.customizer.R
import com.fly.customizer.core.BaseMode
import com.fly.customizer.core.ThemeCatalog
import com.fly.customizer.data.CustomTheme
import com.fly.customizer.databinding.ItemSavedThemeBinding

/**
 * Saved theme list backed by SQLite rows.
 */
class SavedThemeAdapter(
    private val onApply: (CustomTheme) -> Unit,
    private val onDelete: (CustomTheme) -> Unit,
) : RecyclerView.Adapter<SavedThemeAdapter.Holder>() {

    private var themes: List<CustomTheme> = emptyList()

    fun submit(items: List<CustomTheme>) {
        themes = items
        notifyDataSetChanged()
    }

    class Holder(val binding: ItemSavedThemeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val inflater = LayoutInflater.from(parent.context)
        return Holder(ItemSavedThemeBinding.inflate(inflater, parent, false))
    }

    override fun getItemCount(): Int = themes.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val theme = themes[position]
        val binding = holder.binding
        val accent = ThemeCatalog.accent(theme.accentId)
        binding.themeName.text = theme.name
        binding.themeMeta.text = binding.root.context.getString(
            R.string.theme_meta,
            binding.root.context.getString(BaseMode.from(theme.baseMode).labelRes),
            binding.root.context.getString(accent.labelRes),
        )
        binding.themeSwatch.backgroundTintList = ColorStateList.valueOf(accent.color)
        binding.btnThemeApply.setOnClickListener { onApply(theme) }
        binding.btnThemeDelete.setOnClickListener { onDelete(theme) }
    }
}
