package ye.alkamel.billing

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject

class ReadingReminderWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result = try {
        val ctx = applicationContext
        val prefs = ctx.getSharedPreferences("alkamel_db", Context.MODE_PRIVATE)
        val config = JSONObject(prefs.getString("settings", "{}") ?: "{}")
        if (!config.optBoolean("readingReminderEnabled", false)) return Result.success()
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "alkamel_reading_reminders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "تذكير قراءة العدادات", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        val pending = PendingIntent.getActivity(ctx, 501, intent, flags)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(ctx, channelId)
        } else {
            Notification.Builder(ctx)
        }
        val notification = builder
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("الكامل للفواتير والتحصيل")
            .setContentText("حان وقت مراجعة قراءات العدادات وإصدار الفواتير")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        manager.notify(501, notification)
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }
}
