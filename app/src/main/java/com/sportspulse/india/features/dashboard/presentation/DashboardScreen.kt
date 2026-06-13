package com.sportspulse.india.features.dashboard.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sportspulse.india.core.domain.entity.SportType
import com.sportspulse.india.core.ui.components.EventCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("SportsPulse India", fontWeight = FontWeight.Bold) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Filters Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedSportFilter == null,
                        onClick = { viewModel.handleIntent(DashboardIntent.SelectSportFilter(null)) },
                        label = { Text("All") }
                    )
                }
                items(SportType.values()) { sport ->
                    FilterChip(
                        selected = uiState.selectedSportFilter == sport,
                        onClick = { viewModel.handleIntent(DashboardIntent.SelectSportFilter(sport)) },
                        label = { Text(sport.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // LIVE Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Live Matches Only",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = uiState.isLiveFilterActive,
                    onCheckedChange = { viewModel.handleIntent(DashboardIntent.ToggleLiveFilter(it)) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Error Message
            if (uiState.error != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Events List
            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading && uiState.events.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (uiState.events.isEmpty()) {
                    Text(
                        text = "No matches found.",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(uiState.events, key = { it.id }) { event ->
                            EventCard(
                                event = event,
                                onClick = { onNavigateToDetail(event.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
