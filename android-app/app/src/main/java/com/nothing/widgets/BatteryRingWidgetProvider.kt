package com.nothing.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.widget.RemoteViews

class BatteryRingWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.applicationContext.registerReceiver(null, ifilter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 80
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val batteryPct = (level * 100 / scale.toFloat()).toInt()
        val isCharging = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING

        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_battery_ring)
            views.setTextViewText(R.id.txt_battery_pct, "$batteryPct%")
            views.setTextViewText(
                R.id.txt_battery_status,
                if (isCharging) "[⚡ CHARGING]" else "[PHONE(1) • DISCHARGING]"
            )
            views.setProgressBar(R.id.progress_battery, 100, batteryPct, false)

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_battery_container, pendingIntent)

            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
