package com.fly.customizer.ui.animations;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

/**
 * Draws the easing curve of the current animation together with a live progress marker.
 */
public class CurveView extends View {

    private final Paint curvePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path curvePath = new Path();
    private TimeInterpolator interpolator = new LinearInterpolator();
    private float progress = 0f;

    public CurveView(Context context) {
        super(context);
        init();
    }

    public CurveView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CurveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        curvePaint.setStyle(Paint.Style.STROKE);
        curvePaint.setStrokeWidth(getResources().getDisplayMetrics().density * 2.5f);
        curvePaint.setStrokeCap(Paint.Cap.ROUND);
        curvePaint.setColor(0xFF7C5CFF);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(getResources().getDisplayMetrics().density * 1f);
        gridPaint.setColor(0x33FFFFFF);

        markerPaint.setStyle(Paint.Style.FILL);
        markerPaint.setColor(0xFF7C5CFF);

        markerInnerPaint.setStyle(Paint.Style.FILL);
        markerInnerPaint.setColor(0xFFFFFFFF);
    }

    public void setAccentColor(int color) {
        curvePaint.setColor(color);
        markerPaint.setColor(color);
        invalidate();
    }

    public void setInterpolator(TimeInterpolator newInterpolator) {
        this.interpolator = newInterpolator == null ? new LinearInterpolator() : newInterpolator;
        rebuildPath();
        invalidate();
    }

    public void setProgress(float newProgress) {
        this.progress = Math.max(0f, Math.min(1f, newProgress));
        invalidate();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        rebuildPath();
    }

    private void rebuildPath() {
        curvePath.reset();
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        float padding = getResources().getDisplayMetrics().density * 8f;
        float usableWidth = width - padding * 2f;
        float usableHeight = height - padding * 2f;
        for (int step = 0; step <= 64; step++) {
            float t = step / 64f;
            float value = interpolator.getInterpolation(t);
            float x = padding + usableWidth * t;
            float y = height - padding - usableHeight * value;
            if (step == 0) {
                curvePath.moveTo(x, y);
            } else {
                curvePath.lineTo(x, y);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float padding = getResources().getDisplayMetrics().density * 8f;
        float width = getWidth();
        float height = getHeight();
        if (width <= padding * 2f || height <= padding * 2f) {
            return;
        }

        // Grid.
        for (int line = 0; line <= 2; line++) {
            float y = padding + (height - padding * 2f) * line / 2f;
            canvas.drawLine(padding, y, width - padding, y, gridPaint);
        }

        canvas.drawPath(curvePath, curvePaint);

        float markerX = padding + (width - padding * 2f) * progress;
        float value = interpolator.getInterpolation(progress);
        float markerY = height - padding - (height - padding * 2f) * value;
        float radius = getResources().getDisplayMetrics().density * 6f;
        canvas.drawCircle(markerX, markerY, radius, markerPaint);
        canvas.drawCircle(markerX, markerY, radius * 0.45f, markerInnerPaint);
    }
}
