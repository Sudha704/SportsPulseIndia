package com.sportspulse.india

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Hilt-enabled Application class for SportsPulse India.
 *
 * Responsibilities:
 * - Bootstrap Hilt dependency injection
 * - Configure WorkManager with [HiltWorkerFactory]
 * - Plant Timber logging tree (debug builds only)
 * - Create notification channels (required on Android O+)
 */
@HiltAndroidApp
class SportsPulseApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // Injected WorkManager configuration (provided by WorkerModule)
    @Inject
    lateinit var _workManagerConfiguration: Configuration

    override val workManagerConfiguration: Configuration
        get() = _workManagerConfiguration

    override fun onCreate() {
        super.onCreate()

        // ─── Timber logging ───────────────────────────────────────────────────
        if (BuildConfig.ENABLE_LOGGING) {
            Timber.plant(Timber.DebugTree())
        }

        // ─── Notification channels ────────────────────────────────────────────
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        // Match alerts channel
        val alertChannel = NotificationChannel(
            CHANNEL_MATCH_ALERTS,
            "Match Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders before your favourite matches start"
            enableVibration(true)
            setShowBadge(true)
        }

        // Live score updates channel
        val liveChannel = NotificationChannel(
            CHANNEL_LIVE_SCORES,
            "Live Score Updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Score updates during live matches"
            enableVibration(false)
        }

        // General sports news channel
        val newsChannel = NotificationChannel(
            CHANNEL_SPORTS_NEWS,
            "Sports News",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Breaking sports news and headlines"
        }

        manager.createNotificationChannels(
            listOf(alertChannel, liveChannel, newsChannel)
        )
    }

    companion object {
        const val CHANNEL_MATCH_ALERTS = "match_alerts"
        const val CHANNEL_LIVE_SCORES  = "live_scores"
        const val CHANNEL_SPORTS_NEWS  = "sports_news"
    }
}
