package com.example.alris

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@SuppressLint("MissingPermission")
@Composable
fun MapScreen() {
    val context = LocalContext.current

    AndroidView(
        factory = {
            Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))
            Configuration.getInstance().userAgentValue = context.packageName

            val mapView = MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                controller.setZoom(16.0)
                controller.setCenter(GeoPoint(8.5126, 76.9419))

                // Report Points
                val reportPoints = listOf(
                    Triple(8.5123, 76.9416, "Drainage Issue"),
                    Triple(8.5130, 76.9420, "Broken Streetlight"),
                    Triple(8.5141, 76.9435, "Pothole")
                )

                for ((lat, lon, title) in reportPoints) {
                    val marker = Marker(this).apply {
                        position = GeoPoint(lat, lon)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        this.title = title
                    }
                    overlays.add(marker)
                }

                invalidate()
            }

            mapView
        },
        update = {}
    )
}
