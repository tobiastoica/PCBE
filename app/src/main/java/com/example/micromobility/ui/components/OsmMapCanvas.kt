package com.example.micromobility.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.micromobility.domain.model.ChargingStation
import com.example.micromobility.domain.model.Scooter
import com.example.micromobility.domain.model.ScooterStatus
import com.example.micromobility.ui.theme.LocalPalette
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.io.File

@Composable
fun OsmMapCanvas(
    userLat: Double,
    userLon: Double,
    searchRadiusMeters: Int,
    stations: List<ChargingStation>,
    scooters: List<Scooter>,
    selectedScooterId: String?,
    onSelectScooter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val palette = LocalPalette.current

    // Configure osmdroid once using app-private cache (no storage permission needed)
    remember(context) {
        Configuration.getInstance().apply {
            userAgentValue = "MicromobilityApp/1.0 (${context.packageName})"
            osmdroidBasePath = File(context.cacheDir, "osmdroid")
            osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
        }
        true
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = 12.0
            maxZoomLevel = 19.5
            controller.setZoom(15.5)
            controller.setCenter(GeoPoint(userLat, userLon))
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    var lastCenteredLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    val emeraldArgb = palette.emerald.toArgb()
    val emeraldFillArgb = palette.emerald.copy(alpha = 0.08f).toArgb()
    val emeraldStrokeArgb = palette.emerald.copy(alpha = 0.65f).toArgb()
    val edgeCyanArgb = palette.edgeCyan.toArgb()
    val pauseAmberArgb = palette.pauseAmber.toArgb()
    val violationCoralArgb = palette.violationCoral.toArgb()
    val whiteArgb = Color.White.toArgb()
    val darkTextArgb = Color(0xFF0B1F2A).toArgb()

    AndroidView(
        factory = { mapView },
        modifier = modifier
            .fillMaxSize()
            .testTag("osm_map_canvas"),
        update = { map ->
            val currentLoc = userLat to userLon
            if (lastCenteredLocation != currentLoc) {
                map.controller.animateTo(GeoPoint(userLat, userLon))
                lastCenteredLocation = currentLoc
            }

            map.overlays.clear()

            // 1. Geo-search radius circle
            val userPoint = GeoPoint(userLat, userLon)
            val radiusCircle = Polygon(map).apply {
                points = Polygon.pointsAsCircle(userPoint, searchRadiusMeters.toDouble())
                fillColor = emeraldFillArgb
                strokeColor = emeraldStrokeArgb
                strokeWidth = 4f
                setInfoWindow(null)
            }
            map.overlays.add(radiusCircle)

            // 2. Charging Stations (Edge Nodes)
            stations.forEach { station ->
                val shortCode = station.stationId.replace("station_", "S").uppercase()
                val stationMarker = Marker(map).apply {
                    position = GeoPoint(station.lat, station.lon)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = createPillMarkerDrawable(
                        context = context,
                        text = "$shortCode (${station.dockedCount})",
                        backgroundColor = whiteArgb,
                        borderColor = edgeCyanArgb,
                        textColor = edgeCyanArgb
                    )
                    title = station.name
                    snippet = "Edge Node :${station.port} • ${station.dockedCount}/${station.capacity} docked"
                }
                map.overlays.add(stationMarker)
            }

            // 3. Scooters
            scooters.forEach { scooter ->
                val isSelected = scooter.scooterId == selectedScooterId
                val markerColor = when (scooter.status) {
                    ScooterStatus.MOVING -> edgeCyanArgb
                    ScooterStatus.PAUSED, ScooterStatus.RESERVED -> pauseAmberArgb
                    ScooterStatus.LOW_BATTERY -> violationCoralArgb
                    else -> emeraldArgb
                }

                val label = if (isSelected) {
                    "${scooter.scooterId} • ${scooter.batteryLevel}%"
                } else {
                    "${scooter.batteryLevel}%"
                }

                val scooterMarker = Marker(map).apply {
                    position = GeoPoint(scooter.lat, scooter.lon)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = createPillMarkerDrawable(
                        context = context,
                        text = label,
                        backgroundColor = if (isSelected) markerColor else whiteArgb,
                        borderColor = if (isSelected) whiteArgb else markerColor,
                        textColor = if (isSelected) whiteArgb else darkTextArgb
                    )
                    setInfoWindow(null)
                    setOnMarkerClickListener { _, _ ->
                        onSelectScooter(scooter.scooterId)
                        true
                    }
                }
                map.overlays.add(scooterMarker)
            }

            // 4. User location dot
            val userMarker = Marker(map).apply {
                position = userPoint
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = createDotDrawable(
                    context = context,
                    fillColor = edgeCyanArgb,
                    borderColor = whiteArgb
                )
                setInfoWindow(null)
            }
            map.overlays.add(userMarker)

            map.invalidate()
        }
    )
}

private fun createPillMarkerDrawable(
    context: Context,
    text: String,
    backgroundColor: Int,
    borderColor: Int,
    textColor: Int
): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = 11.5f * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val horizontalPad = 9f * density
    val verticalPad = 5f * density
    val textWidth = textPaint.measureText(text)
    val fontMetrics = textPaint.fontMetrics
    val textHeight = fontMetrics.descent - fontMetrics.ascent

    val width = (textWidth + horizontalPad * 2).toInt().coerceAtLeast(1)
    val height = (textHeight + verticalPad * 2).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val strokeWidth = 2f * density
    val rect = RectF(
        strokeWidth / 2f,
        strokeWidth / 2f,
        width - strokeWidth / 2f,
        height - strokeWidth / 2f
    )
    val cornerRadius = height / 2f

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = backgroundColor
        style = Paint.Style.FILL
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = borderColor
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth
    }

    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

    val textY = verticalPad - fontMetrics.ascent
    canvas.drawText(text, horizontalPad, textY, textPaint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun createDotDrawable(
    context: Context,
    fillColor: Int,
    borderColor: Int
): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val size = (22f * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f

    val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = borderColor
        style = Paint.Style.FILL
    }
    val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }

    canvas.drawCircle(center, center, 9f * density, outerPaint)
    canvas.drawCircle(center, center, 6.5f * density, innerPaint)

    return BitmapDrawable(context.resources, bitmap)
}