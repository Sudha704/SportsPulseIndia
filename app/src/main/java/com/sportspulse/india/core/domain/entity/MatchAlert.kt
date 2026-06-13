package com.sportspulse.india.core.domain.entity

/**
 * User-configured match reminder stored locally in Room.
 *
 * A [WorkManager] one-time job keyed by [id] fires a notification
 * [reminderMinutesBefore] minutes before [eventStartTimeIst].
 *
 * @param id                    Unique reminder ID (mirrors the [SportEvent.id]).
 * @param eventId               Associated sport event ID.
 * @param eventTitle            Copied from [SportEvent.title] for offline display.
 * @param sport                 Sport type for icon selection.
 * @param eventStartTimeIst     Event start epoch-ms (IST).
 * @param reminderMinutesBefore How many minutes before kick-off to fire the alert.
 * @param isEnabled             Whether this reminder is currently active.
 * @param workRequestId         UUID string of the scheduled [WorkRequest] (for cancellation).
 */
data class MatchAlert(
    val id: String,
    val eventId: String,
    val eventTitle: String,
    val sport: SportType,
    val eventStartTimeIst: Long,
    val reminderMinutesBefore: Int = 30,
    val isEnabled: Boolean = true,
    val workRequestId: String? = null
) {
    /** Epoch-ms when the notification should fire. */
    val triggerAtMillis: Long
        get() = eventStartTimeIst - (reminderMinutesBefore * 60_000L)
}
