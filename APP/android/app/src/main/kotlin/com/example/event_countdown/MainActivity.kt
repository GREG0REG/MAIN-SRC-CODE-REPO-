package com.example.event_countdown

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.example.event_countdown/widget"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "updateWidget" -> {
                    val appWidgetManager = AppWidgetManager.getInstance(this)
                    val componentName = ComponentName(this, EventCountdownWidgetProvider::class.java)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

                    val title = call.argument<String>("title") ?: "No upcoming events"
                    val countdown = call.argument<String>("countdown") ?: ""
                    val bgColorStart = call.argument<String>("bgColorStart")
                        ?: call.argument<String>("bgColor")
                    val bgColorEnd = call.argument<String>("bgColorEnd")
                    val textColorStr = call.argument<String>("textColor")
                    val progressPercent = call.argument<Int>("progressPercent") ?: 65
                    val showProgress = call.argument<Boolean>("showProgress") ?: true
                    val pulseEnabled = call.argument<Boolean>("pulseEnabled") ?: false
                    val isUrgent = call.argument<Boolean>("isUrgent") ?: false
                    val isAmoled = call.argument<Boolean>("isAmoled") ?: false
                    val isHighContrast = call.argument<Boolean>("isHighContrast") ?: false
                    val bgImagePath = call.argument<String>("bgImagePath")

                    for (widgetId in appWidgetIds) {
                        EventCountdownWidgetProvider.updateWidgetDirectly(
                            this,
                            appWidgetManager,
                            widgetId,
                            title,
                            countdown,
                            bgColorStart,
                            bgColorEnd,
                            textColorStr,
                            progressPercent,
                            showProgress,
                            pulseEnabled,
                            isUrgent,
                            isAmoled,
                            isHighContrast,
                            bgImagePath
                        )
                    }

                    result.success(null)
                }
                "updatePomodoroWidget" -> {
                    val appWidgetManager = AppWidgetManager.getInstance(this)
                    val componentName = ComponentName(this, PomodoroWidgetProvider::class.java)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

                    val subject = call.argument<String>("subject") ?: "Ready to Focus"
                    val timerText = call.argument<String>("timerText") ?: "Tap to start"
                    val status = call.argument<String>("status") ?: "Focus"
                    val bgColorStart = call.argument<String>("bgColorStart")
                        ?: call.argument<String>("bgColor")
                    val bgColorEnd = call.argument<String>("bgColorEnd")
                    val progressPercent = call.argument<Int>("progressPercent") ?: 45
                    val showProgress = call.argument<Boolean>("showProgress") ?: true
                    val pulseEnabled = call.argument<Boolean>("pulseEnabled") ?: false
                    val isUrgent = call.argument<Boolean>("isUrgent") ?: false
                    val isAmoled = call.argument<Boolean>("isAmoled") ?: false
                    val isHighContrast = call.argument<Boolean>("isHighContrast") ?: false

                    for (widgetId in appWidgetIds) {
                        PomodoroWidgetProvider.updateWidgetDirectly(
                            this,
                            appWidgetManager,
                            widgetId,
                            subject,
                            timerText,
                            status,
                            bgColorStart,
                            bgColorEnd,
                            progressPercent,
                            showProgress,
                            pulseEnabled,
                            isUrgent,
                            isAmoled,
                            isHighContrast
                        )
                    }

                    result.success(null)
                }
                else -> result.notImplemented()
            }
        }
    }
}
