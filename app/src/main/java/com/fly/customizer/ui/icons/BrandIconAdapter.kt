package com.fly.customizer.ui.icons

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fly.customizer.R
import com.fly.customizer.util.BrandIcon
import com.fly.customizer.util.BrandIcons
import com.fly.customizer.util.FlyIconPacks
import com.fly.customizer.util.FlyIconRenderer

/**
 * Shows the twelve supported brands as real rendered icons, using the signature brand pack.
 */
class BrandIconAdapter(
    private val onSelect: () -> Unit,
) : RecyclerView.Adapter<BrandIconAdapter.Holder>() {

    private var brands: List<BrandIcon> = BrandIcons.all

    fun submit(items: List<BrandIcon>) {
        brands = items
        notifyDataSetChanged()
    }

    class Holder(val root: LinearLayout, val icon: ImageView, val label: TextView) :
        RecyclerView.ViewHolder(root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val context = parent.context
        val density = context.resources.displayMetrics.density
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            layoutParams = RecyclerView.LayoutParams(
                (96 * density).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            setPadding(0, (6 * density).toInt(), 0, (6 * density).toInt())
        }
        val image = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                (56 * density).toInt(),
                (56 * density).toInt(),
            )
            contentDescription = context.getString(R.string.brand_icons_label)
        }
        val text = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            gravity = android.view.Gravity.CENTER
            textSize = 11f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(
                com.google.android.material.color.MaterialColors.getColor(
                    this,
                    com.google.android.material.R.attr.colorOnSurfaceVariant,
                ),
            )
        }
        container.addView(image)
        container.addView(text)
        return Holder(container, image, text)
    }

    override fun getItemCount(): Int = brands.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val brand = brands[position]
        val context = holder.root.context
        holder.icon.setImageBitmap(
            FlyIconRenderer.render(
                FlyIconPacks.signatureBrandPack,
                context.getString(brand.labelRes),
                brand.id,
                160,
                brand.packages.first(),
            ),
        )
        holder.label.setText(brand.labelRes)
        holder.root.setOnClickListener { onSelect() }
    }
}
