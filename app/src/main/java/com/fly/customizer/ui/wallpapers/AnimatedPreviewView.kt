package com.fly.customizer.ui.wallpapers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.fly.customizer.core.Prefs
import com.fly.customizer.util.AnimatedWallpaper
import com.fly.customizer.util.AnimatedWallpaperCatalog

/**
 * Draws a real animation loop for an [AnimatedWallpaper] using libflycore.so. The loop runs only
 * while the view is attached, renders at a small resolution, and stops when reduce motion is on.
 */
class AnimatedPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private val handler = Handler(Looper.getMainLooper())
    private val destination = Rect()

    private var preset: AnimatedWallpaper = AnimatedWallpaperCatalog.presets.first()
    private var pixels: IntArray? = null
    private var bitmap: Bitmap? = null
    private var frameWidth = 0
    private var frameHeight = 0
    private var phase = 0f
    private var lastFrameAt = 0L
    private var reduceMotion = false

    private val tick = object : Runnable {
        override fun run() {
            if (!isAttachedToWindow) return
            advance()
            invalidate()
            if (!reduceMotion) handler.postDelayed(this, FRAME_DELAY)
        }
    }

    fun setPreset(value: AnimatedWallpaper) {
        if (value.id == preset.id) return
        preset = value
        phase = 0f
        invalidate()
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        allocate(width, height)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        reduceMotion = Prefs(context).reduceMotion
        lastFrameAt = SystemClock.elapsedRealtime()
        handler.post(tick)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(tick)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val current = bitmap ?: return
        if (current.isRecycled) return
        destination.set(0, 0, width, height)
        canvas.drawBitmap(current, null, destination, paint)
    }

    private fun allocate(width: Int, height: Int) {
        val targetWidth = (width / 3).coerceIn(24, 360)
        val targetHeight = (height / 3).coerceIn(24, 640)
        if (targetWidth == frameWidth && targetHeight == frameHeight) return
        release()
        frameWidth = targetWidth
        frameHeight = targetHeight
        try {
            pixels = IntArray(frameWidth * frameHeight)
            bitmap = Bitmap.createBitmap(frameWidth, frameHeight, Bitmap.Config.ARGB_8888)
        } catch (error: OutOfMemoryError) {
            Log.e(TAG, "Preview buffer too large", error)
            frameWidth = 0
            frameHeight = 0
        }
    }

    private fun advance() {
        val target = bitmap ?: return
        val buffer = pixels ?: return
        val now = SystemClock.elapsedRealtime()
        if (!reduceMotion) {
            val delta = (now - lastFrameAt).coerceIn(0L, 250L)
            phase += delta / 1000f * preset.speed
            if (phase > 720f) phase -= 720f
        }
        lastFrameAt = now
        AnimatedWallpaperCatalog.renderFrame(preset, buffer, frameWidth, frameHeight, phase)
        target.setPixels(buffer, 0, frameWidth, 0, 0, frameWidth, frameHeight)
    }

    private fun release() {
        bitmap?.recycle()
        bitmap = null
        pixels = null
    }

    private companion object {
        const val TAG = "AnimatedPreview"
        const val FRAME_DELAY = 60L
    }
}
