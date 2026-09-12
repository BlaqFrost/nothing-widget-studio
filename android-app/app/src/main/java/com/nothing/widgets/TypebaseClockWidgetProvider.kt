package com.nothing.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TypebaseClockWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val now = Date()
        val hourFormat = SimpleDateFormat("HH", Locale.US)
        val minFormat = SimpleDateFormat("mm", Locale.US)
        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.US)
        val ndotDate = SimpleDateFormat("MM.dd // 'W'w", Locale.US)

        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_typebase_clock)
            views.setTextViewText(R.id.txt_hour, hourFormat.format(now))
            views.setTextViewText(R.id.txt_minute, minFormat.format(now))
            views.setTextViewText(R.id.txt_date_readable, dateFormat.format(now).uppercase())
            views.setTextViewText(R.id.txt_ndot_tag, "[NOTHING.SYS // ${ndotDate.format(now)}]")

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_clock_container, pendingIntent)

            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
