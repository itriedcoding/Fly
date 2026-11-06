package com.fly.customizer.widgets

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.fly.customizer.FlyApp
import com.fly.customizer.R

/**
 * Success callback for `AppWidgetManager.requestPinAppWidget`.
 *
 * The system broadcasts this intent with [AppWidgetManager.EXTRA_APPWIDGET_ID] filled in once the
 * launcher has actually placed the widget, so the app only records confirmed pins.
 */
class WidgetPinReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PINNED && intent.action != null) {
            return
        }
        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.w(TAG, "Pin callback without a widget id")
            return
        }
        val provider = intent.getStringExtra(EXTRA_PROVIDER).orEmpty()
        val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()
        try {
            FlyApp.repository.recordPinnedWidget(provider, label, widgetId)
        } catch (error: Exception) {
            Log.e(TAG, "Unable to record pinned widget", error)
        }
        val display = label.ifBlank { context.getString(R.string.widgets_title) }
        Toast.makeText(
            context,
            context.getString(R.string.widget_pinned_toast, display),
            Toast.LENGTH_SHORT,
        ).show()
    }

    companion object {
        private const val TAG = "WidgetPinReceiver"
        const val ACTION_PINNED = "com.fly.customizer.action.WIDGET_PINNED"
        const val EXTRA_PROVIDER = "com.fly.customizer.extra.WIDGET_PROVIDER"
        const val EXTRA_LABEL = "com.fly.customizer.extra.WIDGET_LABEL"
    }
}
