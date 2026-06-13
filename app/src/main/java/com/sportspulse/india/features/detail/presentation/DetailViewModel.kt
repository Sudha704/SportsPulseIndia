package com.sportspulse.india.features.detail.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportspulse.india.core.domain.entity.MatchAlert
import com.sportspulse.india.core.domain.repository.AlertRepository
import com.sportspulse.india.core.domain.usecase.GetEventDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val getEventDetailUseCase: GetEventDetailUseCase,
    private val alertRepository: AlertRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun handleIntent(intent: DetailIntent) {
        when (intent) {
            is DetailIntent.LoadEvent -> loadEventDetails(intent.eventId, withAiSummary = false)
            is DetailIntent.GenerateAiSummary -> loadEventDetails(intent.eventId, withAiSummary = true)
            is DetailIntent.SetMatchAlert -> setMatchAlert(intent.minutesBefore)
            is DetailIntent.RemoveMatchAlert -> removeMatchAlert()
            is DetailIntent.DismissError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun loadEventDetails(eventId: String, withAiSummary: Boolean) {
        viewModelScope.launch {
            if (withAiSummary) {
                _uiState.update { it.copy(isSummaryLoading = true, error = null) }
            } else {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            val result = getEventDetailUseCase(eventId, withAiSummary)
            result.onSuccess { event ->
                val currentAlert = alertRepository.getAlertForEvent(eventId)
                _uiState.update {
                    it.copy(
                        event = event,
                        currentAlert = currentAlert,
                        isLoading = false,
                        isSummaryLoading = false,
                        error = null
                    )
                }
            }.onFailure { exception ->
                Timber.e(exception, "Failed to load event details")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSummaryLoading = false,
                        error = exception.message ?: "Failed to load event details"
                    )
                }
            }
        }
    }

    private fun setMatchAlert(minutesBefore: Int) {
        val event = _uiState.value.event ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSettingAlert = true) }
            runCatching {
                val alert = MatchAlert(
                    id = UUID.randomUUID().toString(),
                    eventId = event.id,
                    eventTitle = event.title,
                    sport = event.sport,
                    eventStartTimeIst = event.startTimeIst,
                    reminderMinutesBefore = minutesBefore,
                    isEnabled = true,
                    workRequestId = "" // WorkManager sets this
                )
                val savedAlert = alertRepository.upsertAlert(alert)
                _uiState.update { 
                    it.copy(
                        currentAlert = savedAlert,
                        isSettingAlert = false
                    ) 
                }
            }.onFailure { exception ->
                Timber.e(exception, "Failed to set match alert")
                _uiState.update { 
                    it.copy(
                        isSettingAlert = false,
                        error = "Failed to set reminder"
                    ) 
                }
            }
        }
    }

    private fun removeMatchAlert() {
        val event = _uiState.value.event ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSettingAlert = true) }
            runCatching {
                alertRepository.deleteAlert(event.id)
                _uiState.update { 
                    it.copy(
                        currentAlert = null,
                        isSettingAlert = false
                    ) 
                }
            }.onFailure { exception ->
                Timber.e(exception, "Failed to remove match alert")
                _uiState.update { 
                    it.copy(
                        isSettingAlert = false,
                        error = "Failed to remove reminder"
                    ) 
                }
            }
        }
    }
}
