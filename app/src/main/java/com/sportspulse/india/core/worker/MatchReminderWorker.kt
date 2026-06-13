package com.sportspulse.india.core.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sportspulse.india.MainActivity
import com.sportspulse.india.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Worker responsible for firing a local notification when a match alert is triggered.
 */
@HiltWorker
class MatchReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_EVENT_ID = "EVENT_ID"
        const val KEY_EVENT_TITLE = "EVENT_TITLE"
        const val KEY_SPORT_NAME = "SPORT_NAME"
        
        const val CHANNEL_ID = "match_reminders_channel"
    }

    override suspend fun doWork(): Result {
        val eventId = inputData.getString(KEY_EVENT_ID) ?: return Result.failure()
        val eventTitle = inputData.getString(KEY_EVENT_TITLE) ?: "Match is starting soon!"
        val sportName = inputData.getString(KEY_SPORT_NAME) ?: "SportsPulse"

        Timber.d("MatchReminderWorker: Firing notification for $eventId")
        showNotification(eventId, eventTitle, sportName)
        return Result.success()
    }

    private fun showNotification(eventId: String, title: String, sport: String) {
        val notificationManager = ContextCompat.getSystemService(
            context,
            NotificationManager::class.java
        ) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Match Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for upcoming matches"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Deep link into the detail screen
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // In a real app we'd construct the Jetpack Compose deep link URI
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            eventId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use default for now
            .setContentTitle("$sport: $title")
            .setContentText("Your match is starting soon. Tap to view details and broadcasts.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(eventId.hashCode(), notification)
    }
}
