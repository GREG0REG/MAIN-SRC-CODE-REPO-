package com.example.event_countdown

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews

class EventCountdownWidgetProvider : AppWidgetProvider() {
    companion object {
        private const val PREFS_NAME = "FlutterSharedPreferences"
        private const val KEY_TITLE = "event_title"
        private const val KEY_COUNTDOWN = "countdown_text"
        private const val KEY_BG_COLOR = "widget_bg_color"
        private const val KEY_PROGRESS = "widget_progress_percent"
        private const val DEFAULT_START = "#00BFA5"
        private const val DEFAULT_END = "#7C4DFF"

        fun updateWidgetDirectly(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            title: String,
            countdown: String,
            bgColorStartStr: String?,
            bgColorEndStr: String?,
            @Suppress("UNUSED_PARAMETER") textColorStr: String?,
            progressPercent: Int = 65,
            showProgress: Boolean = true,
            pulseEnabled: Boolean = false,
            isUrgent: Boolean = false,
            isAmoled: Boolean = false,
            isHighContrast: Boolean = false,
            bgImagePath: String? = null,
        ) {
            try {
                val views = RemoteViews(context.packageName, R.layout.event_widget_layout)

                views.setTextViewText(R.id.widget_title, title)
                views.setTextViewText(R.id.widget_countdown, countdown)

                val colorStart = WidgetArtRenderer.parseColor(bgColorStartStr, Color.parseColor(DEFAULT_START))
                val colorEnd = WidgetArtRenderer.parseColor(bgColorEndStr, Color.parseColor(DEFAULT_END))
                val clampedProgress = progressPercent.coerceIn(0, 100)

                val cardBitmap = WidgetArtRenderer.renderCard(
                    context, colorStart, colorEnd, isAmoled, isHighContrast, bgImagePath
                )
                views.setImageViewBitmap(R.id.widget_card_bg, cardBitmap)

                val textColor = if (isHighContrast) WidgetArtRenderer.bestTextColor(colorStart) else Color.WHITE
                views.setTextColor(R.id.widget_title, textColor)
                views.setTextColor(R.id.widget_countdown, textColor)

                if (showProgress) {
                    views.setViewVisibility(R.id.widget_ring, View.VISIBLE)
                    val ringBitmap = WidgetArtRenderer.renderRing(
                        colorStart, colorEnd, clampedProgress / 100f,
                        isAmoled, isHighContrast, pulseEnabled, isUrgent
                    )
                    views.setImageViewBitmap(R.id.widget_ring, ringBitmap)
                } else {
                    views.setViewVisibility(R.id.widget_ring, View.GONE)
                }

                val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        widgetId,
                        launchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                }

                appWidgetManager.updateAppWidget(widgetId, views)
            } catch (e: Exception) {
                android.util.Log.e("EventWidget", "Update failed, falling back to safe render", e)
                renderFallback(context, appWidgetManager, widgetId, title, countdown)
            }
        }

        /** Minimal, can't-fail render used only if the rich bitmap render throws. */
        private fun renderFallback(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            title: String,
            countdown: String,
        ) {
            try {
                val views = RemoteViews(context.packageName, R.layout.event_widget_layout)
                views.setTextViewText(R.id.widget_title, title)
                views.setTextViewText(R.id.widget_countdown, countdown)
                views.setViewVisibility(R.id.widget_ring, View.GONE)
                views.setInt(R.id.widget_card_bg, "setBackgroundColor", Color.parseColor(DEFAULT_START))
                appWidgetManager.updateAppWidget(widgetId, views)
            } catch (_: Exception) {
                // Nothing further we can safely do.
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val title = prefs.getString(KEY_TITLE, "No upcoming events") ?: "No upcoming events"
        val countdown = prefs.getString(KEY_COUNTDOWN, "") ?: ""
        val bgColorStr = prefs.getString(KEY_BG_COLOR, null)
        val progressPercent = prefs.getInt(KEY_PROGRESS, 65)

        for (widgetId in appWidgetIds) {
            updateWidgetDirectly(
                context, appWidgetManager, widgetId, title, countdown,
                bgColorStr, null, null, progressPercent
            )
        }
    }
}

class PomodoroWidgetProvider : AppWidgetProvider() {
    companion object {
        private const val PREFS_NAME = "FlutterSharedPreferences"
        private const val KEY_SUBJECT = "pomodoro_subject"
        private const val KEY_TIMER = "pomodoro_timer_text"
        private const val KEY_STATUS = "pomodoro_status"
        private const val KEY_BG_COLOR = "pomodoro_bg_color"
        private const val KEY_PROGRESS = "pomodoro_progress_percent"
        private const val DEFAULT_START = "#00BFA5"
        private const val DEFAULT_END = "#00E5FF"

        fun updateWidgetDirectly(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            subject: String,
            timerText: String,
            status: String,
            bgColorStartStr: String?,
            bgColorEndStr: String?,
            progressPercent: Int = 45,
            showProgress: Boolean = true,
            pulseEnabled: Boolean = false,
            isUrgent: Boolean = false,
            isAmoled: Boolean = false,
            isHighContrast: Boolean = false,
        ) {
            try {
                val views = RemoteViews(context.packageName, R.layout.pomodoro_widget_layout)

                views.setTextViewText(R.id.pomodoro_widget_subject, subject)
                views.setTextViewText(R.id.pomodoro_widget_timer, timerText)
                views.setTextViewText(R.id.pomodoro_widget_status, status)

                val colorStart = WidgetArtRenderer.parseColor(bgColorStartStr, Color.parseColor(DEFAULT_START))
                val colorEnd = WidgetArtRenderer.parseColor(bgColorEndStr, Color.parseColor(DEFAULT_END))
                val clampedProgress = progressPercent.coerceIn(0, 100)

                val cardBitmap = WidgetArtRenderer.renderCard(
                    context, colorStart, colorEnd, isAmoled, isHighContrast, null
                )
                views.setImageViewBitmap(R.id.pomodoro_card_bg, cardBitmap)

                val textColor = if (isHighContrast) WidgetArtRenderer.bestTextColor(colorStart) else Color.WHITE
                views.setTextColor(R.id.pomodoro_widget_subject, textColor)
                views.setTextColor(R.id.pomodoro_widget_timer, textColor)
                views.setTextColor(R.id.pomodoro_widget_status, textColor)

                if (showProgress) {
                    views.setViewVisibility(R.id.pomodoro_ring, View.VISIBLE)
                    val ringBitmap = WidgetArtRenderer.renderRing(
                        colorStart, colorEnd, clampedProgress / 100f,
                        isAmoled, isHighContrast, pulseEnabled, isUrgent
                    )
                    views.setImageViewBitmap(R.id.pomodoro_ring, ringBitmap)
                } else {
                    views.setViewVisibility(R.id.pomodoro_ring, View.GONE)
                }

                val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        widgetId,
                        launchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.pomodoro_widget_root, pendingIntent)
                }

                appWidgetManager.updateAppWidget(widgetId, views)
            } catch (e: Exception) {
                android.util.Log.e("PomodoroWidget", "Update failed", e)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            val subject = prefs.getString(KEY_SUBJECT, "Ready to Focus") ?: "Ready to Focus"
            val timerText = prefs.getString(KEY_TIMER, "Tap to start") ?: "Tap to start"
            val status = prefs.getString(KEY_STATUS, "Focus") ?: "Focus"
            val bgColorStr = prefs.getString(KEY_BG_COLOR, null)
            val progressPercent = prefs.getInt(KEY_PROGRESS, 45)

            for (widgetId in appWidgetIds) {
                updateWidgetDirectly(
                    context, appWidgetManager, widgetId, subject, timerText, status,
                    bgColorStr, null, progressPercent
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("PomodoroWidget", "onUpdate failed", e)
        }
    }
}
