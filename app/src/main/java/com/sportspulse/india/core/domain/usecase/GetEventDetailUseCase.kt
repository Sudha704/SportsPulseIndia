package com.sportspulse.india.core.domain.usecase

import com.sportspulse.india.core.domain.entity.SportEvent
import com.sportspulse.india.core.domain.repository.SportEventRepository
import javax.inject.Inject

/**
 * Use case for fetching a single [SportEvent] with an optional Gemini-powered AI summary.
 *
 * Called when the user taps a match card and lands on the Detail screen.
 *
 * @param repository  Source of truth for sport events.
 */
class GetEventDetailUseCase @Inject constructor(
    private val repository: SportEventRepository
) {
    /**
     * @param eventId       Unique ID of the event to fetch.
     * @param withAiSummary When true, triggers a Gemini API call to generate a match preview.
     *                      The summary is stored in [SportEvent.geminiSummary].
     */
    suspend operator fun invoke(
        eventId: String,
        withAiSummary: Boolean = true
    ): Result<SportEvent> = repository.getEventById(eventId, withAiSummary)
}
