package com.sportspulse.india.features.alerts.data

import com.sportspulse.india.core.data.MockDataSource
import com.sportspulse.india.core.data.db.dao.AlertDao
import com.sportspulse.india.core.data.db.entity.MatchAlertEntity
import com.sportspulse.india.core.domain.entity.MatchAlert
import com.sportspulse.india.core.domain.entity.SportType
import com.sportspulse.india.core.domain.repository.AlertRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Concrete implementation of [AlertRepository].
 *
 * WorkManager scheduling is wired in Batch 6 (worker implementation).
 * Currently stores and reads from Room via [AlertDao].
 */
class AlertRepositoryImpl @Inject constructor(
    private val alertDao: AlertDao
) : AlertRepository {

    override fun getAllAlerts(): Flow<List<MatchAlert>> =
        alertDao.observeAllAlerts().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getAlertForEvent(eventId: String): MatchAlert? =
        alertDao.getAlertForEvent(eventId)?.toDomain()

    override suspend fun upsertAlert(alert: MatchAlert): MatchAlert {
        alertDao.upsert(alert.toEntity())
        // WorkManager scheduling added in Batch 6
        return alert
    }

    override suspend fun deleteAlert(eventId: String) {
        alertDao.deleteByEventId(eventId)
        // WorkManager cancellation added in Batch 6
    }

    override suspend fun toggleAlert(eventId: String, enabled: Boolean) {
        alertDao.setEnabled(eventId, enabled)
        // WorkManager re-schedule / cancel in Batch 6
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mappers (inline for simplicity; moved to dedicated mapper in Batch 3)
    // ─────────────────────────────────────────────────────────────────────────

    private fun MatchAlertEntity.toDomain() = MatchAlert(
        id                    = id,
        eventId               = eventId,
        eventTitle            = eventTitle,
        sport                 = SportType.fromString(sport),
        eventStartTimeIst     = eventStartTimeIst,
        reminderMinutesBefore = reminderMinutesBefore,
        isEnabled             = isEnabled,
        workRequestId         = workRequestId
    )

    private fun MatchAlert.toEntity() = MatchAlertEntity(
        id                    = id,
        eventId               = eventId,
        eventTitle            = eventTitle,
        sport                 = sport.name,
        eventStartTimeIst     = eventStartTimeIst,
        reminderMinutesBefore = reminderMinutesBefore,
        isEnabled             = isEnabled,
        workRequestId         = workRequestId
    )
}
