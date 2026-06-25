package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MonitorWidgetProvider : AppWidgetProvider() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        scope.launch {
            updateAllWidgets(context, appWidgetManager, appWidgetIds)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MonitorWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            scope.launch {
                updateAllWidgets(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    private suspend fun updateAllWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val db = AppDatabase.getDatabase(context)
        val logs = db.usageDao().getAllUsageLogs().firstOrNull() ?: emptyList()
        val config = db.usageDao().getApiConfig() ?: com.example.data.ApiConfig()

        val totalCost = logs.sumOf { it.cost }
        val budget = config.monthlyBudget.coerceAtLeast(0.01)
        val progress = ((totalCost / budget) * 100).toInt().coerceIn(0, 100)

        val ratio = totalCost / budget
        val isExceeded = ratio >= 1.0
        val isWarning = ratio >= 0.8 && ratio < 1.0 // Warning at 80% or higher

        val timeString = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

        withContext(Dispatchers.Main) {
            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.monitor_widget_layout)

                // Populate widget text and metrics
                views.setTextViewText(R.id.widget_cost_text, String.format("$%.2f", totalCost))
                views.setTextViewText(R.id.widget_budget_text, String.format("Limit: $%.2f", budget))
                views.setTextViewText(R.id.widget_updated_time, "Sync: $timeString")

                // Update progress bar
                views.setProgressBar(R.id.widget_progress_bar, 100, progress, false)

                // Toggle visibility states of status badges
                if (isExceeded) {
                    views.setViewVisibility(R.id.widget_status_ok, View.GONE)
                    views.setViewVisibility(R.id.widget_status_warning, View.GONE)
                    views.setViewVisibility(R.id.widget_status_alert, View.VISIBLE)
                } else if (isWarning) {
                    views.setViewVisibility(R.id.widget_status_ok, View.GONE)
                    views.setViewVisibility(R.id.widget_status_warning, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_status_alert, View.GONE)
                } else {
                    views.setViewVisibility(R.id.widget_status_ok, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_status_warning, View.GONE)
                    views.setViewVisibility(R.id.widget_status_alert, View.GONE)
                }

                // Setup deep link to App on clicking spending amount
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val mainPendingIntent = PendingIntent.getActivity(
                    context, 0, mainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_cost_text, mainPendingIntent)
                views.setOnClickPendingIntent(R.id.widget_cost_label, mainPendingIntent)

                // Setup broadcast intent for Sync action
                val refreshIntent = Intent(context, MonitorWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH_WIDGET
                }
                val refreshPendingIntent = PendingIntent.getBroadcast(
                    context, appWidgetId, refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        job.cancel()
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.example.widget.ACTION_REFRESH_WIDGET"
    }
}
