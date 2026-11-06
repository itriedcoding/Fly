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

/**
 * A built-in Fly icon pack.
 *
 * Every icon is drawn on device from the app's own label and a stable hash of its component, so
 * these packs theme real installed apps without shipping hundreds of static PNGs and without
 * pretending a third-party pack is installed.
 */
data class FlyIconPack(
    val id: String,
    val labelRes: Int,
    val palette: IntArray,
    val shape: IconShape,
    val outline: Boolean = false,
) {
    override fun equals(other: Any?): Boolean = other is FlyIconPack && other.id == id
    override fun hashCode(): Int = id.hashCode()
}

/**
 * The built-in pack catalogue: twelve packs across six shapes and twelve palettes.
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
    )

    fun pack(id: String?): FlyIconPack? = all.firstOrNull { it.id == id }

    fun packOrDefault(id: String?): FlyIconPack = pack(id) ?: all.first()

    val shapeCount: Int get() = IconShape.values().size
}

/**
 * Draws icon-pack artwork with Canvas: gradient fill, soft highlight, a shape mask and the app's
 * own monogram. Colours are chosen with the native luminance function so the letter always has
 * real contrast against its background.
 */
object FlyIconRenderer {

    private val cache = object : LruCache<String, Bitmap>(80) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    fun render(pack: FlyIconPack, label: String, key: String, sizePx: Int): Bitmap {
        val size = sizePx.coerceIn(48, 512)
        val cacheKey = "${pack.id}:$key:$size"
        cache.get(cacheKey)?.takeIf { !it.isRecycled }?.let { return it }

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val strokeWidth = size * 0.045f
        val path = shapePath(pack.shape, size.toFloat(), if (pack.outline) strokeWidth else 0f)

        val seed = key.hashCode()
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
            pack.palette,
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

        val midColor = pack.palette[1]
        val ink = if (NativeFly.luminance(midColor) > 0.55f) 0xFF15151E.toInt() else Color.WHITE
        val letterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = size * 0.44f
            setShadowLayer(
                size * 0.02f,
                0f,
                size * 0.012f,
                if (ink == Color.WHITE) 0x55000000 else 0x33000000,
            )
        }
        val metrics = letterPaint.fontMetrics
        val baseline = size / 2f - (metrics.ascent + metrics.descent) / 2f
        canvas.drawText(monogram(label), size / 2f, baseline, letterPaint)
        canvas.restore()

        if (pack.outline) {
            canvas.drawPath(
                path,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    this.strokeWidth = strokeWidth
                    strokeJoin = Paint.Join.ROUND
                    color = if (ink == Color.WHITE) 0x66FFFFFF else 0x5515151E
                },
            )
        }

        cache.put(cacheKey, bitmap)
        return bitmap
    }

    fun clear() = cache.evictAll()

    private fun monogram(label: String): String {
        val letter = label.trim().firstOrNull { it.isLetterOrDigit() } ?: return "•"
        return letter.toString().uppercase()
    }

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
