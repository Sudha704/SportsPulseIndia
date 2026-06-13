package com.sportspulse.india.core.domain.repository

import com.sportspulse.india.core.domain.entity.MatchAlert
import kotlinx.coroutines.flow.Flow

/**
 * Contract for managing user-configured match reminder alerts.
 * Backed by Room + WorkManager one-time jobs.
 */
interface AlertRepository {

    /** Emits the full list of saved alerts, updating in real time. */
    fun getAllAlerts(): Flow<List<MatchAlert>>

    /** Returns a single alert by event ID, or null if not found. */
    suspend fun getAlertForEvent(eventId: String): MatchAlert?

    /**
     * Creates or updates a [MatchAlert], scheduling the corresponding WorkManager job.
     * Returns the updated [MatchAlert] with its [MatchAlert.workRequestId] filled in.
     */
    suspend fun upsertAlert(alert: MatchAlert): MatchAlert

    /**
     * Deletes the alert for [eventId] and cancels its WorkManager job.
     */
    suspend fun deleteAlert(eventId: String)

    /**
     * Toggles the [MatchAlert.isEnabled] flag for [eventId].
     * Re-schedules or cancels the WorkManager job accordingly.
     */
    suspend fun toggleAlert(eventId: String, enabled: Boolean)
}
