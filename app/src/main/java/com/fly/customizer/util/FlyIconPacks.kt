package com.fly.customizer.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.LruCache
import com.fly.customizer.R
import kotlin.math.cos
import kotlin.math.sin

/** Shape used by a built-in icon pack. */
enum class IconShape { CIRCLE, SQUIRCLE, ROUNDED, HEXAGON, PENTAGON, DIAMOND }

/** How a brand-aware pack paints the matched app instead of a monogram. */
enum class BrandTreatment { SIGNATURE, DARK, LIGHT, MONO }

/**
 * A built-in Fly icon pack.
 *
 * Brand-aware packs use the stylised glyphs in [BrandIcons] for the twelve supported apps and fall
 * back to an on-device monogram for everything else.
 */
data class FlyIconPack(
    val id: String,
    val labelRes: Int,
    val palette: IntArray,
    val shape: IconShape,
    val outline: Boolean = false,
    val brandAware: Boolean = false,
    val treatment: BrandTreatment = BrandTreatment.SIGNATURE,
) {
    override fun equals(other: Any?): Boolean = other is FlyIconPack && other.id == id
    override fun hashCode(): Int = id.hashCode()
}

/**
 * The built-in pack catalogue: twelve palette packs plus four brand-aware packs.
 */
object FlyIconPacks {

    val all: List<FlyIconPack> = listOf(
        FlyIconPack(
            id = "aurora",
            labelRes = R.string.fly_pack_aurora,
            palette = intArrayOf(0xFF241752.toInt(), 0xFF6D3BF5.toInt(), 0xFF29C7E0.toInt()),
            shape = IconShape.SQUIRCLE,
        ),
        FlyIconPack(
            id = "midnight",
            labelRes = R.string.fly_pack_midnight,
            palette = intArrayOf(0xFF05060F.toInt(), 0xFF101A38.toInt(), 0xFF2C4FA8.toInt()),
            shape = IconShape.CIRCLE,
        ),
        FlyIconPack(
            id = "neon",
            labelRes = R.string.fly_pack_neon,
            palette = intArrayOf(0xFF0A0618.toInt(), 0xFF7B2BFF.toInt(), 0xFF35F0D0.toInt()),
            shape = IconShape.HEXAGON,
        ),
        FlyIconPack(
            id = "pastel",
            labelRes = R.string.fly_pack_pastel,
            palette = intArrayOf(0xFF8A7FE8.toInt(), 0xFFF2B8D0.toInt(), 0xFFBFE6F2.toInt()),
            shape = IconShape.ROUNDED,
        ),
        FlyIconPack(
            id = "mono",
            labelRes = R.string.fly_pack_mono,
            palette = intArrayOf(0xFF111114.toInt(), 0xFF3A3A44.toInt(), 0xFF8A8A9A.toInt()),
            shape = IconShape.DIAMOND,
            outline = true,
        ),
        FlyIconPack(
            id = "sunset",
            labelRes = R.string.fly_pack_sunset,
            palette = intArrayOf(0xFF241400.toInt(), 0xFFFF7A3C.toInt(), 0xFFFFD9A0.toInt()),
            shape = IconShape.SQUIRCLE,
        ),
        FlyIconPack(
            id = "forest",
            labelRes = R.string.fly_pack_forest,
            palette = intArrayOf(0xFF04140A.toInt(), 0xFF144D26.toInt(), 0xFF4FBF6A.toInt()),
            shape = IconShape.PENTAGON,
        ),
        FlyIconPack(
            id = "ocean",
            labelRes = R.string.fly_pack_ocean,
            palette = intArrayOf(0xFF03211E.toInt(), 0xFF0C6B5F.toInt(), 0xFF4FE0C0.toInt()),
            shape = IconShape.CIRCLE,
        ),
        FlyIconPack(
            id = "candy",
            labelRes = R.string.fly_pack_candy,
            palette = intArrayOf(0xFF2B0620.toInt(), 0xFFFF7BC8.toInt(), 0xFFFFE0F2.toInt()),
            shape = IconShape.ROUNDED,
        ),
        FlyIconPack(
            id = "slate",
            labelRes = R.string.fly_pack_slate,
            palette = intArrayOf(0xFF0A0D12.toInt(), 0xFF4C6478.toInt(), 0xFFAFC4D4.toInt()),
            shape = IconShape.HEXAGON,
        ),
        FlyIconPack(
            id = "copper",
            labelRes = R.string.fly_pack_copper,
            palette = intArrayOf(0xFF1C0F07.toInt(), 0xFF8A4B22.toInt(), 0xFFE39A54.toInt()),
            shape = IconShape.PENTAGON,
        ),
        FlyIconPack(
            id = "cyber",
            labelRes = R.string.fly_pack_cyber,
            palette = intArrayOf(0xFF02040A.toInt(), 0xFF10265C.toInt(), 0xFF5A8CFF.toInt()),
            shape = IconShape.DIAMOND,
        ),
        FlyIconPack(
            id = "signature",
            labelRes = R.string.fly_pack_signature,
            palette = intArrayOf(0xFF141420.toInt(), 0xFF3E3E58.toInt(), 0xFFB8B8D0.toInt()),
            shape = IconShape.SQUIRCLE,
            brandAware = true,
            treatment = BrandTreatment.SIGNATURE,
        ),
        FlyIconPack(
            id = "brand_dark",
            labelRes = R.string.fly_pack_brand_dark,
            palette = intArrayOf(0xFF07070C.toInt(), 0xFF1B1B26.toInt(), 0xFF3A3A4C.toInt()),
            shape = IconShape.HEXAGON,
            brandAware = true,
            treatment = BrandTreatment.DARK,
        ),
        FlyIconPack(
            id = "brand_light",
            labelRes = R.string.fly_pack_brand_light,
            palette = intArrayOf(0xFFF7F7FC.toInt(), 0xFFDCDCE8.toInt(), 0xFF8A8AA0.toInt()),
            shape = IconShape.ROUNDED,
            brandAware = true,
            treatment = BrandTreatment.LIGHT,
        ),
        FlyIconPack(
            id = "brand_mono",
            labelRes = R.string.fly_pack_brand_mono,
            palette = intArrayOf(0xFF111114.toInt(), 0xFF2E2E38.toInt(), 0xFF6E6E80.toInt()),
            shape = IconShape.DIAMOND,
            brandAware = true,
            treatment = BrandTreatment.MONO,
        ),
    )

    fun pack(id: String?): FlyIconPack? = all.firstOrNull { it.id == id }

    fun packOrDefault(id: String?): FlyIconPack = pack(id) ?: all.first()

    val shapeCount: Int get() = IconShape.values().size

    val brandPacks: List<FlyIconPack> get() = all.filter { it.brandAware }

    val signatureBrandPack: FlyIconPack get() = pack("signature") ?: all.first()
}

/**
 * Draws icon-pack artwork with Canvas: gradient fill, soft highlight, a shape mask and either the
 * app's own monogram or a stylised brand glyph. Colours are checked with the native luminance
 * function so the artwork always has real contrast.
 */
object FlyIconRenderer {

    private const val DARK_INK = 0xFF15151E.toInt()

    private val cache = object : LruCache<String, Bitmap>(96) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    private data class IconTheme(val background: IntArray, val glyph: Int, val detail: Int)

    fun render(
        pack: FlyIconPack,
        label: String,
        key: String,
        sizePx: Int,
        packageName: String? = null,
    ): Bitmap {
        val size = sizePx.coerceIn(48, 512)
        val brand = if (pack.brandAware) BrandIcons.find(packageName) else null
        val cacheKey = "${pack.id}:${brand?.id ?: "-"}:$key:$size"
        cache.get(cacheKey)?.takeIf { !it.isRecycled }?.let { return it }

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val theme = themeFor(pack, brand)
        val strokeWidth = size * 0.045f
        val path = shapePath(pack.shape, size.toFloat(), if (pack.outline) strokeWidth else 0f)

        val seed = (brand?.primary ?: key.hashCode())
        val angle = 25.0 + (seed and 0x7F) * 2.0
        val radians = Math.toRadians(angle)
        val dx = (cos(radians) * size * 0.5).toFloat()
        val dy = (sin(radians) * size * 0.5).toFloat()

        canvas.save()
        canvas.clipPath(path)

        val gradient = LinearGradient(
            size / 2f - dx,
            size / 2f - dy,
            size / 2f + dx,
            size / 2f + dy,
            theme.background,
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(
            0f,
            0f,
            size.toFloat(),
            size.toFloat(),
            Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = gradient },
        )

        val highlight = RadialGradient(
            size * 0.28f,
            size * 0.2f,
            size * 0.62f,
            0x44FFFFFF,
            0x00FFFFFF,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(
            size * 0.28f,
            size * 0.2f,
            size * 0.62f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = highlight },
        )

        if (brand != null) {
            BrandGlyphRenderer.draw(canvas, brand.glyph, theme.glyph, theme.detail, size.toFloat())
        } else {
            val letterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.glyph
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = size * 0.44f
                setShadowLayer(
                    size * 0.02f,
                    0f,
                    size * 0.012f,
                    if (theme.glyph == Color.WHITE) 0x55000000 else 0x33000000,
                )
            }
            val metrics = letterPaint.fontMetrics
            val baseline = size / 2f - (metrics.ascent + metrics.descent) / 2f
            canvas.drawText(monogram(label), size / 2f, baseline, letterPaint)
        }
        canvas.restore()

        if (pack.outline) {
            canvas.drawPath(
                path,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    this.strokeWidth = strokeWidth
                    strokeJoin = Paint.Join.ROUND
                    color = withAlpha(theme.detail, 0x66)
                },
            )
        }

        cache.put(cacheKey, bitmap)
        return bitmap
    }

    fun clear() = cache.evictAll()

    private fun themeFor(pack: FlyIconPack, brand: BrandIcon?): IconTheme {
        if (brand == null) {
            val ink = if (NativeFly.luminance(pack.palette[1]) > 0.55f) DARK_INK else Color.WHITE
            return IconTheme(pack.palette, ink, ink)
        }
        return when (pack.treatment) {
            BrandTreatment.SIGNATURE -> {
                val background = intArrayOf(
                    brand.primary,
                    mix(brand.primary, brand.secondary, 0.45f),
                    brand.secondary,
                )
                val ink = if (NativeFly.luminance(background[1]) > 0.55f) DARK_INK else Color.WHITE
                IconTheme(background, ink, withAlpha(ink, 0x55))
            }
            BrandTreatment.DARK -> IconTheme(
                intArrayOf(0xFF07070C.toInt(), 0xFF141420.toInt(), brand.primary),
                Color.WHITE,
                brand.secondary,
            )
            BrandTreatment.LIGHT -> {
                val ink = darken(brand.primary, 0.45f)
                IconTheme(
                    intArrayOf(0xFFF7F7FC.toInt(), 0xFFE8E8F2.toInt(), lighten(brand.secondary, 0.6f)),
                    ink,
                    withAlpha(ink, 0x55),
                )
            }
            BrandTreatment.MONO -> IconTheme(
                intArrayOf(0xFF111114.toInt(), 0xFF2A2A33.toInt(), 0xFF3E3E4A.toInt()),
                Color.WHITE,
                withAlpha(Color.WHITE, 0x66),
            )
        }
    }

    private fun monogram(label: String): String {
        val letter = label.trim().firstOrNull { it.isLetterOrDigit() } ?: return "•"
        return letter.toString().uppercase()
    }

    private fun mix(from: Int, to: Int, amount: Float): Int {
        val t = amount.coerceIn(0f, 1f)
        val red = (Color.red(from) + (Color.red(to) - Color.red(from)) * t).toInt()
        val green = (Color.green(from) + (Color.green(to) - Color.green(from)) * t).toInt()
        val blue = (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t).toInt()
        return Color.rgb(red, green, blue)
    }

    private fun darken(color: Int, amount: Float): Int = mix(color, Color.BLACK, amount)

    private fun lighten(color: Int, amount: Float): Int = mix(color, Color.WHITE, amount)

    private fun withAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or ((alpha and 0xFF) shl 24)

    private fun shapePath(shape: IconShape, size: Float, inset: Float): Path {
        val path = Path()
        val rect = RectF(inset, inset, size - inset, size - inset)
        val radius = size / 2f - inset
        when (shape) {
            IconShape.CIRCLE -> path.addOval(rect, Path.Direction.CW)
            IconShape.SQUIRCLE ->
                path.addRoundRect(rect, size * 0.26f, size * 0.26f, Path.Direction.CW)
            IconShape.ROUNDED ->
                path.addRoundRect(rect, size * 0.17f, size * 0.17f, Path.Direction.CW)
            IconShape.HEXAGON -> polygon(path, size / 2f, size / 2f, radius, 6, -90f)
            IconShape.PENTAGON -> polygon(path, size / 2f, size / 2f, radius, 5, -90f)
            IconShape.DIAMOND -> polygon(path, size / 2f, size / 2f, radius, 4, -90f)
        }
        return path
    }

    private fun polygon(
        path: Path,
        centerX: Float,
        centerY: Float,
        radius: Float,
        sides: Int,
        startDegrees: Float,
    ) {
        for (index in 0 until sides) {
            val angle = Math.toRadians((startDegrees + index * 360f / sides).toDouble())
            val x = centerX + (radius * cos(angle)).toFloat()
            val y = centerY + (radius * sin(angle)).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
    }
}
