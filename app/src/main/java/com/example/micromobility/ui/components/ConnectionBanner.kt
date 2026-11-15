package com.example.micromobility.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.micromobility.domain.model.ActiveConnectionTarget
import com.example.micromobility.ui.theme.LocalPalette

@Composable
fun ConnectionBanner(
    activeConnection: ActiveConnectionTarget,
    locationLabel: String,
    isRealGpsFix: Boolean,
    onRefreshLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalPalette.current
    val isEdge = activeConnection is ActiveConnectionTarget.EdgeStationNode

    val containerColor by animateColorAsState(
        targetValue = if (isEdge) palette.edgeCyanContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        animationSpec = tween(300),
        label = "conn_bg"
    )
    val accentColor by animateColorAsState(
        targetValue = if (isEdge) palette.edgeCyan else palette.emerald,
        animationSpec = tween(300),
        label = "conn_accent"
    )

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("connection_banner"),
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        tonalElevation = 6.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isEdge) Icons.Rounded.Hub else Icons.Rounded.Dns,
                        contentDescription = if (isEdge) "Connected to Edge Node" else "Connected to Central Server",
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .alpha(pulseAlpha)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (activeConnection) {
                                is ActiveConnectionTarget.CentralServer ->
                                    "Phase 1 • Central Server :${activeConnection.port}"
                                is ActiveConnectionTarget.EdgeStationNode ->
                                    "Phase 2 Edge • ${activeConnection.stationId} :${activeConnection.port}"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = when (activeConnection) {
                            is ActiveConnectionTarget.CentralServer ->
                                "$locationLabel • Local Demo"
                            is ActiveConnectionTarget.EdgeStationNode ->
                                "Exclusive link with ${activeConnection.stationName}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onRefreshLocation,
                modifier = Modifier.testTag("refresh_location_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.MyLocation,
                    contentDescription = "Refresh GPS location",
                    tint = if (isRealGpsFix) palette.emerald else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}