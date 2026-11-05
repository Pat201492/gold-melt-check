package com.goldmeltcheck.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.goldmeltcheck.MainActivity
import com.goldmeltcheck.R
import com.goldmeltcheck.core.AlertHit
import com.goldmeltcheck.core.Alerts
import com.goldmeltcheck.core.Metal
import com.goldmeltcheck.core.PricePoint
import kotlinx.coroutines.flow.first

/** Records each fetched price in the history and posts a notification for any alert it trips. */
object PriceAlerts {
    private const val CHANNEL = "price_alerts"

    suspend fun recordAndCheck(context: Context, store: PriceStore, metal: Metal, point: PricePoint) {
        val history = store.appendHistory(metal, point)
        val fired = store.alertLastFired.first()
        store.alertRules.first()
            .filter { it.metal == metal }
            .mapNotNull { Alerts.evaluate(it, history, fired[it.id]) }
            .forEach { hit ->
                if (notify(context, hit)) store.markAlertFired(hit.rule.id, point.at)
            }
    }

    fun canNotify(context: Context): Boolean {
        val granted = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Returns true if the notification was shown. Without permission the alert stays unfired and can try again. */
    private fun notify(context: Context, hit: AlertHit): Boolean {
        if (!canNotify(context)) return false
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(hit.title)
            .setContentText(hit.body)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        return runCatching {
            NotificationManagerCompat.from(context).notify(hit.rule.id.hashCode(), n)
            true
        }.getOrDefault(false)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Price alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Gold and silver price alerts you set up on the Trends tab"
                },
            )
        }
    }
}
