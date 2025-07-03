package com.example.alris

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class DashboardActivity : AppCompatActivity() {

    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // OSMDroid setup
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_dashboard)

        mapView = findViewById(R.id.mapView)
        setupMap()

        // 🔽 Drop your fixed report points here
        dropReportPoint(8.5123, 76.9416, "Drainage Issue", "Overflowing during rain")
        dropReportPoint(8.5130, 76.9420, "Broken Streetlight", "Reported on 2 July")
        dropReportPoint(8.5141, 76.9435, "Pothole", "Severe road damage")
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.setBuiltInZoomControls(true)

        val mapController = mapView.controller
        mapController.setZoom(16.0)
        mapController.setCenter(GeoPoint(8.5126, 76.9419))
    }

    private fun dropReportPoint(latitude: Double, longitude: Double, title: String, snippet: String) {
        val marker = Marker(mapView).apply {
            position = GeoPoint(latitude, longitude)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            this.title = title
            this.snippet = snippet
        }
        mapView.overlays.add(marker)
        mapView.invalidate()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDetach()
    }
}
