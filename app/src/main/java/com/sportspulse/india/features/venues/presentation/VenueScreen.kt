package com.sportspulse.india.features.venues.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sportspulse.india.core.domain.entity.Venue
import com.sportspulse.india.core.domain.entity.VenueType
import com.sportspulse.india.core.ui.theme.IndianGreen
import com.sportspulse.india.core.ui.theme.IndianWhite
import com.sportspulse.india.core.ui.theme.NavyBlue
import com.sportspulse.india.core.ui.theme.Saffron

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VenueScreen(
    viewModel: VenueViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || 
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.handleIntent(VenueIntent.FetchUserLocation)
        }
    }

    LaunchedEffect(Unit) {
        val fineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        
        if (fineLocation != PackageManager.PERMISSION_GRANTED && coarseLocation != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            viewModel.handleIntent(VenueIntent.FetchUserLocation)
        }
    }

    // Filter states
    var selectedRating by remember { mutableFloatStateOf(0f) }
    var selectedDistance by remember { mutableFloatStateOf(50f) } // default 50km
    var selectedSport by remember { mutableStateOf<VenueType?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Nearby Venues", 
                        fontWeight = FontWeight.Bold,
                        color = NavyBlue
                    ) 
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Location Status
            if (uiState.isLocationLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
            } else if (uiState.locationError != null) {
                Column {
                    Text(
                        text = uiState.locationError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Saffron)
                    ) {
                        Text("Grant Location Permission", color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = Saffron,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Near ${uiState.userLocation.areaName.ifBlank { "You" }}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Saffron
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                // Filters
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedRating == 4.0f,
                            onClick = { selectedRating = if (selectedRating == 4.0f) 0f else 4.0f },
                            label = { Text("4.0+ Rating") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Saffron.copy(alpha = 0.2f))
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedDistance == 5f,
                            onClick = { selectedDistance = if (selectedDistance == 5f) 50f else 5f },
                            label = { Text("< 5 km") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Saffron.copy(alpha = 0.2f))
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedDistance == 10f,
                            onClick = { selectedDistance = if (selectedDistance == 10f) 50f else 10f },
                            label = { Text("< 10 km") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Saffron.copy(alpha = 0.2f))
                        )
                    }
                    items(VenueType.values()) { type ->
                        FilterChip(
                            selected = selectedSport == type,
                            onClick = { selectedSport = if (selectedSport == type) null else type },
                            label = { Text(type.name) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Saffron.copy(alpha = 0.2f))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Venues List
            val filteredVenues = uiState.venues.filter { venue ->
                venue.rating >= selectedRating &&
                venue.distanceKm <= selectedDistance &&
                (selectedSport == null || venue.type == selectedSport)
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading && uiState.venues.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (filteredVenues.isEmpty() && !uiState.isLoading) {
                    Text(
                        text = "No venues found nearby.",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filteredVenues, key = { it.placeId }) { venue ->
                            VenueCard(
                                venue = venue,
                                onClick = {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("geo:0,0?q=${venue.lat},${venue.lng}(${venue.name})")
                                    )
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VenueCard(venue: Venue, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = venue.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = venue.address,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${venue.type.name} • ★ ${venue.rating}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Saffron
                )
                
                val travelText = if (venue.travelTimeMinutes > 0) {
                    "${venue.travelTimeMinutes} mins away"
                } else {
                    String.format("%.1f km", venue.distanceKm)
                }
                Text(
                    text = travelText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
