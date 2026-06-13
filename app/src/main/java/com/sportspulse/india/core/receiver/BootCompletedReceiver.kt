package com.sportspulse.india.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.sportspulse.india.core.domain.repository.AlertRepository
import com.sportspulse.india.core.worker.MatchReminderWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Re-schedules all active WorkManager match alerts when the device boots up.
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alertRepository: AlertRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Timber.d("BootCompletedReceiver: Device booted, re-scheduling active alerts")
            
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val activeAlerts = alertRepository.getAllAlerts().firstOrNull()?.filter { it.isEnabled }
                    activeAlerts?.forEach { alert ->
                        val delay = alert.triggerAtMillis - System.currentTimeMillis()
                        
                        if (delay > 0) {
                            val inputData = Data.Builder()
                                .putString(MatchReminderWorker.KEY_EVENT_ID, alert.eventId)
                                .putString(MatchReminderWorker.KEY_EVENT_TITLE, alert.eventTitle)
                                .putString(MatchReminderWorker.KEY_SPORT_NAME, alert.sport.displayName)
                                .build()
                            
                            val workRequest = OneTimeWorkRequestBuilder<MatchReminderWorker>()
                                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                                .setInputData(inputData)
                                .build()
                                
                            WorkManager.getInstance(context).enqueueUniqueWork(
                                "alert_${alert.eventId}",
                                ExistingWorkPolicy.REPLACE,
                                workRequest
                            )
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "BootCompletedReceiver: Failed to reschedule alerts")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
