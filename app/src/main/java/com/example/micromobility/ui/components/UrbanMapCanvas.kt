package com.example.micromobility.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.micromobility.core.location.LocationTracker
import com.example.micromobility.domain.model.ChargingStation
import com.example.micromobility.domain.model.Scooter
import com.example.micromobility.domain.model.ScooterStatus
import com.example.micromobility.ui.theme.LocalPalette
import kotlin.math.cos
import kotlin.math.hypot

@Composable
fun UrbanMapCanvas(
    userLat: Double,
    userLon: Double,
    searchRadiusMeters: Int,
    stations: List<ChargingStation>,
    scooters: List<Scooter>,
    selectedScooterId: String?,
    onSelectScooter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalPalette.current
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textMeasurer = rememberTextMeasurer()

    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var zoomScale by remember { mutableFloatStateOf(1f) }

    val pulseTransition = rememberInfiniteTransition(label = "map_pulse")
    val pulseRadius by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius"
    )
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    val centerLat = LocationTracker.DEFAULT_LAT
    val centerLon = LocationTracker.DEFAULT_LON

    var scooterScreenPoints by remember { mutableStateOf<List<Pair<String, Offset>>>(emptyList()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.mapBackground)
            .testTag("urban_map_canvas")
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    zoomScale = (zoomScale * zoom).coerceIn(0.75f, 2.8f)
                    panOffset = Offset(
                        x = (panOffset.x + pan.x).coerceIn(-600f, 600f),
                        y = (panOffset.y + pan.y).coerceIn(-600f, 600f)
                    )
                }
            }
            .pointerInput(scooters) {
                detectTapGestures { tapOffset ->
                    val hit = scooterScreenPoints.minByOrNull { (_, pos) ->
                        hypot(pos.x - tapOffset.x, pos.y - tapOffset.y)
                    }
                    if (hit != null && hypot(hit.second.x - tapOffset.x, hit.second.y - tapOffset.y) < 64f) {
                        onSelectScooter(hit.first)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val basePxPerDegLat = size.minDimension * 52f * zoomScale
            val cosLat = cos(Math.toRadians(centerLat)).toFloat()
            val basePxPerDegLon = basePxPerDegLat * cosLat

            fun toScreen(lat: Double, lon: Double): Offset {
                val dx = ((lon - centerLon) * basePxPerDegLon).toFloat()
                val dy = ((centerLat - lat) * basePxPerDegLat).toFloat()
                return Offset(
                    x = size.width * 0.5f + dx + panOffset.x,
                    y = size.height * 0.44f + dy + panOffset.y
                )
            }

            // 1. Parks
            val parkTopLeft = toScreen(45.7524, 21.2185)
            val parkBottomRight = toScreen(45.7492, 21.2232)
            drawRoundRect(
                color = palette.mapPark,
                topLeft = parkTopLeft,
                size = Size(
                    width = (parkBottomRight.x - parkTopLeft.x).coerceAtLeast(40f),
                    height = (parkBottomRight.y - parkTopLeft.y).coerceAtLeast(40f)
                ),
                cornerRadius = CornerRadius(28f, 28f)
            )

            val uniriiParkTL = toScreen(45.7590, 21.2275)
            val uniriiParkBR = toScreen(45.7570, 21.2305)
            drawRoundRect(
                color = palette.mapPark.copy(alpha = 0.7f),
                topLeft = uniriiParkTL,
                size = Size(
                    width = (uniriiParkBR.x - uniriiParkTL.x).coerceAtLeast(30f),
                    height = (uniriiParkBR.y - uniriiParkTL.y).coerceAtLeast(30f)
                ),
                cornerRadius = CornerRadius(20f, 20f)
            )

            // 2. Bega Canal
            val riverPath = Path().apply {
                val p0 = toScreen(45.7478, 21.2140)
                val p1 = toScreen(45.7498, 21.2230)
                val p2 = toScreen(45.7482, 21.2330)
                val p3 = toScreen(45.7502, 21.2420)
                moveTo(p0.x, p0.y)
                cubicTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
            }
            drawPath(
                path = riverPath,
                color = palette.mapRiver,
                style = Stroke(
                    width = 24.dp.toPx() * zoomScale.coerceAtMost(1.5f),
                    cap = StrokeCap.Round
                )
            )

            // 3. Urban street grid
            for (i in -6..6) {
                val latLine = centerLat + i * 0.0022
                drawLine(
                    color = if (i % 2 == 0) palette.mapRoadPrimary else palette.mapRoadSecondary,
                    start = toScreen(latLine, centerLon - 0.025),
                    end = toScreen(latLine, centerLon + 0.025),
                    strokeWidth = if (i % 2 == 0) 7.dp.toPx() else 3.dp.toPx()
                )

                val lonLine = centerLon + i * 0.0030
                drawLine(
                    color = if (i % 2 == 0) palette.mapRoadPrimary else palette.mapRoadSecondary,
                    start = toScreen(centerLat - 0.018, lonLine),
                    end = toScreen(centerLat + 0.018, lonLine),
                    strokeWidth = if (i % 2 == 0) 7.dp.toPx() else 3.dp.toPx()
                )
            }

            // 4. Geo-search radius circle
            val userPos = toScreen(userLat, userLon)
            val metersPerDegLat = 111_111f
            val radiusPx = (searchRadiusMeters / metersPerDegLat) * basePxPerDegLat
            drawCircle(
                color = palette.emerald.copy(alpha = 0.07f),
                radius = radiusPx,
                center = userPos
            )
            drawCircle(
                color = palette.emerald.copy(alpha = 0.45f),
                radius = radiusPx,
                center = userPos,
                style = Stroke(
                    width = 1.8.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f))
                )
            )

            // 5. Charging Stations (Edge Nodes)
            stations.forEach { st ->
                val stPos = toScreen(st.lat, st.lon)
                drawCircle(
                    color = palette.edgeCyan.copy(alpha = 0.14f),
                    radius = 22.dp.toPx(),
                    center = stPos
                )
                drawCircle(
                    color = surfaceColor,
                    radius = 13.dp.toPx(),
                    center = stPos
                )
                drawCircle(
                    color = palette.edgeCyan,
                    radius = 13.dp.toPx(),
                    center = stPos,
                    style = Stroke(width = 2.5.dp.toPx())
                )

                val shortLabel = st.stationId.replace("station_", "S").uppercase() + " (${st.dockedCount})"
                val measured = textMeasurer.measure(
                    text = shortLabel,
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = onSurfaceColor
                    )
                )
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        stPos.x - measured.size.width / 2f,
                        stPos.y + 15.dp.toPx()
                    )
                )
            }

            // 6. Scooters
            val computedScreenPoints = mutableListOf<Pair<String, Offset>>()
            scooters.forEach { sc ->
                val scPos = toScreen(sc.lat, sc.lon)
                computedScreenPoints += sc.scooterId to scPos

                val isSelected = sc.scooterId == selectedScooterId
                val markerColor = when (sc.status) {
                    ScooterStatus.MOVING -> palette.edgeCyan
                    ScooterStatus.PAUSED, ScooterStatus.RESERVED -> palette.pauseAmber
                    ScooterStatus.LOW_BATTERY -> palette.violationCoral
                    else -> palette.emerald
                }

                if (isSelected) {
                    drawCircle(
                        color = markerColor.copy(alpha = 0.25f),
                        radius = 20.dp.toPx(),
                        center = scPos
                    )
                }

                drawCircle(
                    color = Color.White,
                    radius = if (isSelected) 12.dp.toPx() else 9.5.dp.toPx(),
                    center = scPos
                )
                drawCircle(
                    color = markerColor,
                    radius = if (isSelected) 9.dp.toPx() else 7.dp.toPx(),
                    center = scPos
                )

                if (isSelected) {
                    val labelLayout = textMeasurer.measure(
                        text = "${sc.scooterId} • ${sc.batteryLevel}%",
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = onSurfaceColor
                        )
                    )
                    drawRoundRect(
                        color = surfaceColor.copy(alpha = 0.92f),
                        topLeft = Offset(
                            scPos.x - labelLayout.size.width / 2f - 6.dp.toPx(),
                            scPos.y - 32.dp.toPx()
                        ),
                        size = Size(
                            labelLayout.size.width + 12.dp.toPx(),
                            labelLayout.size.height + 6.dp.toPx()
                        ),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                    drawText(
                        textLayoutResult = labelLayout,
                        topLeft = Offset(
                            scPos.x - labelLayout.size.width / 2f,
                            scPos.y - 29.dp.toPx()
                        )
                    )
                }
            }
            scooterScreenPoints = computedScreenPoints

            // 7. User location marker
            drawCircle(
                color = palette.edgeCyan.copy(alpha = pulseAlpha * 0.7f),
                radius = 14.dp.toPx() * pulseRadius,
                center = userPos
            )
            drawCircle(
                color = Color.White,
                radius = 8.dp.toPx(),
                center = userPos
            )
            drawCircle(
                color = palette.edgeCyan,
                radius = 5.5.dp.toPx(),
                center = userPos
            )
        }
    }
}