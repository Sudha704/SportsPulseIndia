package com.sportspulse.india.features.alerts.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sportspulse.india.core.domain.entity.MatchAlert
import com.sportspulse.india.core.domain.entity.SportType
import com.sportspulse.india.core.ui.theme.IndianGreen
import com.sportspulse.india.core.ui.theme.IndianWhite
import com.sportspulse.india.core.ui.theme.NavyBlue
import com.sportspulse.india.core.ui.theme.Saffron
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    viewModel: AlertsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = NavyBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            "Match Alerts",
                            fontWeight = FontWeight.Bold,
                            color = NavyBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = NavyBlue
                ),
                modifier = Modifier.background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Saffron, IndianWhite, IndianGreen)
                    )
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showBottomSheet = true },
                icon = {
                    Icon(Icons.Default.Add, contentDescription = "Add Alert")
                },
                text = { Text("Add Alert") },
                containerColor = Saffron,
                contentColor = Color.White
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (uiState.isLoading && uiState.alerts.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.alerts.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "No alerts",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No match reminders yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap + Add Alert to get notified\nbefore upcoming matches.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp, horizontal = 0.dp)
                ) {
                    items(uiState.alerts, key = { it.id }) { alert ->
                        AlertCard(
                            alert = alert,
                            onToggle = { isEnabled ->
                                viewModel.handleIntent(AlertsIntent.ToggleAlert(alert.eventId, isEnabled))
                            },
                            onDelete = {
                                viewModel.handleIntent(AlertsIntent.RemoveAlert(alert.eventId))
                            }
                        )
                    }
                }
            }

            // Error snackbar
            uiState.error?.let { errorMsg ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp),
                    action = {
                        TextButton(onClick = { viewModel.handleIntent(AlertsIntent.DismissError) }) {
                            Text("Dismiss", color = Saffron)
                        }
                    }
                ) {
                    Text(errorMsg)
                }
            }
        }
    }

    // ─── Add Alert Bottom Sheet ─────────────────────────────────────────────
    if (showBottomSheet) {
        AddAlertBottomSheet(
            sheetState = sheetState,
            onDismiss = { showBottomSheet = false },
            onCreate = { sport, minutesBefore ->
                viewModel.handleIntent(AlertsIntent.CreateCustomAlert(sport, minutesBefore))
                showBottomSheet = false
            }
        )
    }
}

// ─── Add Alert Bottom Sheet ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAlertBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onCreate: (SportType, Int) -> Unit
) {
    var selectedSport by remember { mutableStateOf<SportType?>(null) }
    var selectedMinutes by remember { mutableIntStateOf(30) }
    val reminderOptions = listOf(15, 30, 60, 120)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Text(
                text = "🔔 Add Match Alert",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Get notified before matches of your favourite sport.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            // Sport type picker
            Text(
                text = "Select Sport",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // Grid of sport chips (2 per row)
            val sportList = SportType.entries.toList()
            sportList.chunked(2).forEach { rowSports ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowSports.forEach { sport ->
                        val isSelected = selectedSport == sport
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedSport = sport },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Saffron else MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = if (isSelected) 4.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = sport.emoji,
                                    fontSize = 20.sp
                                )
                                Text(
                                    text = sport.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    // Fill last row if odd count
                    if (rowSports.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }

            HorizontalDivider()

            // Reminder lead time
            Text(
                text = "Remind Me Before",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                reminderOptions.forEach { minutes ->
                    val isSelected = selectedMinutes == minutes
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedMinutes = minutes },
                        label = {
                            Text(
                                text = if (minutes < 60) "${minutes}m" else "${minutes / 60}h",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Saffron,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Create button
            Button(
                onClick = {
                    selectedSport?.let { sport ->
                        onCreate(sport, selectedMinutes)
                    }
                },
                enabled = selectedSport != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Saffron,
                    contentColor = Color.White,
                    disabledContainerColor = Saffron.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (selectedSport != null)
                        "Set ${selectedSport!!.emoji} ${selectedSport!!.displayName} Alert"
                    else "Select a sport first",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

// ─── Alert Card ──────────────────────────────────────────────────────────────

@Composable
fun AlertCard(
    alert: MatchAlert,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val formatter = SimpleDateFormat("d MMM, h:mm a", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sport emoji badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Saffron.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = alert.sport.emoji, fontSize = 22.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.eventTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${alert.sport.displayName} • ${formatter.format(alert.eventStartTimeIst)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Saffron
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Reminding ${alert.reminderMinutesBefore} mins before",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(
                    checked = alert.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Saffron,
                        checkedTrackColor = Saffron.copy(alpha = 0.3f)
                    )
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Alert",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
