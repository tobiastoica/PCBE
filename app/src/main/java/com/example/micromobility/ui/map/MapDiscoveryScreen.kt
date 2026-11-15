package com.example.micromobility.ui.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.ElectricScooter
import androidx.compose.material.icons.rounded.LockClock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.micromobility.domain.model.Scooter
import com.example.micromobility.ui.components.ConnectionBanner
import com.example.micromobility.ui.components.OsmMapCanvas
import com.example.micromobility.ui.components.UrbanMapCanvas
import com.example.micromobility.ui.theme.LocalPalette

@Composable
fun MapDiscoveryScreen(
    uiState: MapDiscoveryUiState,
    onRefreshLocation: () -> Unit,
    onSelectRadius: (Int) -> Unit,
    onSelectScooter: (String) -> Unit,
    onReserveClick: () -> Unit,
    onStartRideClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("map_discovery_screen")
    ) {
        OsmMapCanvas(
            userLat = uiState.userLocation.lat,
            userLon = uiState.userLocation.lon,
            searchRadiusMeters = uiState.searchRadiusMeters,
            stations = uiState.stations,
            scooters = uiState.allScooters,
            selectedScooterId = uiState.selectedScooterId,
            onSelectScooter = onSelectScooter,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ConnectionBanner(
                activeConnection = uiState.activeConnection,
                locationLabel = uiState.userLocation.label,
                isRealGpsFix = uiState.userLocation.isRealGpsFix,
                onRefreshLocation = onRefreshLocation
            )

            RadiusSelectorRow(
                selectedRadius = uiState.searchRadiusMeters,
                scooterCount = uiState.nearbyScooters.size,
                onSelectRadius = onSelectRadius
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            DiscoveryDeck(
                nearbyScooters = uiState.nearbyScooters,
                selectedScooter = uiState.selectedScooter,
                onSelectScooter = onSelectScooter,
                onReserveClick = onReserveClick,
                onStartRideClick = onStartRideClick
            )
        }
    }
}

@Composable
private fun RadiusSelectorRow(
    selectedRadius: Int,
    scooterCount: Int,
    onSelectRadius: (Int) -> Unit
) {
    val radii = listOf(400, 750, 1200, 2500)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        radii.forEach { radius ->
            val selected = radius == selectedRadius
            FilterChip(
                selected = selected,
                onClick = { onSelectRadius(radius) },
                label = {
                    Text(
                        text = if (radius >= 1000) "${radius / 1000.0}km" else "${radius}m",
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.testTag("radius_chip_$radius")
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
        ) {
            Text(
                text = "$scooterCount free",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun DiscoveryDeck(
    nearbyScooters: List<Scooter>,
    selectedScooter: Scooter?,
    onSelectScooter: (String) -> Unit,
    onReserveClick: () -> Unit,
    onStartRideClick: () -> Unit
) {
    val palette = LocalPalette.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (nearbyScooters.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(nearbyScooters, key = { it.scooterId }) { scooter ->
                    val isSelected = scooter.scooterId == selectedScooter?.scooterId
                    Surface(
                        modifier = Modifier
                            .clickable { onSelectScooter(scooter.scooterId) }
                            .testTag("scooter_chip_${scooter.scooterId}"),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                        },
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) palette.emerald else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ElectricScooter,
                                contentDescription = null,
                                tint = palette.emerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = scooter.scooterId,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${scooter.batteryLevel}% • ${scooter.distanceFromUserMeters}m",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            if (selectedScooter == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (nearbyScooters.isEmpty()) {
                            "No scooters available within the selected radius"
                        } else {
                            "Tap a scooter on the map for details"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Expand the search radius to discover more vehicles.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(palette.emeraldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ElectricScooter,
                                    contentDescription = "Electric scooter",
                                    tint = palette.emerald,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "${selectedScooter.model} (${selectedScooter.scooterId})",
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Edge Node: ${selectedScooter.stationName} (:${selectedScooter.stationPort})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.edgeCyan,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = palette.emeraldContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.BatteryChargingFull,
                                    contentDescription = "Battery level",
                                    tint = palette.emerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${selectedScooter.batteryLevel}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onReserveClick,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("reserve_scooter_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LockClock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reserve 60m", maxLines = 1)
                        }

                        Button(
                            onClick = onStartRideClick,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.emerald),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("start_direct_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Ride", fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}