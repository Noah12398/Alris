package com.example.alris

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.alris.Constants.logoutAndGoToLogin
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.alris.ui.theme.AlrisTheme


@SuppressLint("MissingPermission")
@Composable
fun MapScreen() {
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var selectedReport by remember { mutableStateOf<ReportPoint?>(null) }
    var isReportListVisible by remember { mutableStateOf(false) }
    var currentMapType by remember { mutableStateOf(MapType.STANDARD) }
    var showMyLocation by remember { mutableStateOf(true) }

    // Sample report data
    val reportPoints = remember {
        listOf(
            ReportPoint(
                id = 1,
                lat = 8.5123,
                lon = 76.9416,
                title = "Drainage Issue",
                description = "Water logging during rainy season",
                category = ReportCategory.DRAINAGE,
                timestamp = "2 hours ago",
                status = ReportStatus.PENDING
            ),
            ReportPoint(
                id = 2,
                lat = 8.5130,
                lon = 76.9420,
                title = "Broken Streetlight",
                description = "Street light not working for 3 days",
                category = ReportCategory.LIGHTING,
                timestamp = "1 day ago",
                status = ReportStatus.IN_PROGRESS
            ),

            ReportPoint(
                id = 4,
                lat = 8.5115,
                lon = 76.9400,
                title = "Garbage Overflow",
                description = "Garbage bin overflowing",
                category = ReportCategory.WASTE,
                timestamp = "5 hours ago",
                status = ReportStatus.PENDING
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map View
        AndroidView(
            factory = { ctx ->
                Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", 0))
                Configuration.getInstance().userAgentValue = ctx.packageName

                MapView(ctx).apply {
                    setTileSource(currentMapType.tileSource)
                    setMultiTouchControls(true)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    controller.setZoom(16.0)
                    controller.setCenter(GeoPoint(8.5126, 76.9419))

                    // Add location overlay
                    if (showMyLocation) {
                        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                        locationOverlay.enableMyLocation()
                        overlays.add(locationOverlay)
                    }

                    // Add report markers
                    reportPoints.forEach { report ->
                        val marker = Marker(this).apply {
                            position = GeoPoint(report.lat, report.lon)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = report.title
                            snippet = report.description
                            icon = getMarkerIcon(ctx, report.category, report.status)

                            setOnMarkerClickListener { marker, _ ->
                                selectedReport = report
                                true
                            }
                        }
                        overlays.add(marker)
                    }

                    invalidate()
                    mapView = this
                }
            },
            update = { view ->
                // Update map type if changed
                if (view.tileProvider.tileSource != currentMapType.tileSource) {
                    view.setTileSource(currentMapType.tileSource)
                }
            }
        )

        // Top Controls
        MapTopControls(
            currentMapType = currentMapType,
            onMapTypeChange = { currentMapType = it },
            showMyLocation = showMyLocation,
            onLocationToggle = { showMyLocation = it },
            onReportListToggle = { isReportListVisible = !isReportListVisible },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp)
        )

        // 🚀 Logout Button at top right
        FloatingActionButton(
            onClick = { logoutAndGoToLogin(context) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp),
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
        }

        // Statistics Card
        MapStatsCard(
            reportPoints = reportPoints,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 16.dp)
        )


        // Report Details Bottom Sheet
        selectedReport?.let { report ->
            ReportDetailsCard(
                report = report,
                onDismiss = { selectedReport = null },
                onNavigate = { /* Navigate to report */ },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }

        // Reports List Side Panel
        AnimatedVisibility(
            visible = isReportListVisible,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy
                )
            ) + fadeIn(),
            exit = slideOutHorizontally(
                targetOffsetX = { -it }
            ) + fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(320.dp)
        ) {
            ReportsListPanel(
                reports = reportPoints,
                onReportClick = { report ->
                    selectedReport = report
                    mapView?.controller?.animateTo(GeoPoint(report.lat, report.lon))
                },
                onClose = { isReportListVisible = false },
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(16.dp)
            )
        }

        // FAB for adding new report
        FloatingActionButton(
            onClick = { /* Add new report */ },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add Report",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun MapTopControls(
    currentMapType: MapType,
    onMapTypeChange: (MapType) -> Unit,
    showMyLocation: Boolean,
    onLocationToggle: (Boolean) -> Unit,
    onReportListToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reports List Button
        FloatingActionButton(
            onClick = onReportListToggle,
            modifier = Modifier.size(48.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Icon(
                Icons.Default.List,
                contentDescription = "Reports List",
                modifier = Modifier.size(20.dp)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Map Type Selector
            Card(
                modifier = Modifier.shadow(4.dp, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(4.dp)
                ) {
                    MapType.values().forEach { type ->
                        val isSelected = currentMapType == type
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable { onMapTypeChange(type) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type.displayName,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // My Location Button
            FloatingActionButton(
                onClick = { onLocationToggle(!showMyLocation) },
                modifier = Modifier.size(48.dp),
                containerColor = if (showMyLocation) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface,
                contentColor = if (showMyLocation) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = "My Location",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun MapStatsCard(
    reportPoints: List<ReportPoint>,
    modifier: Modifier = Modifier
) {
    val pendingCount = reportPoints.count { it.status == ReportStatus.PENDING }
    val inProgressCount = reportPoints.count { it.status == ReportStatus.IN_PROGRESS }
    val resolvedCount = reportPoints.count { it.status == ReportStatus.RESOLVED }

    Card(
        modifier = modifier.shadow(4.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Reports",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            StatItem(
                count = pendingCount,
                label = "Pending",
                color = MaterialTheme.colorScheme.error
            )
            StatItem(
                count = inProgressCount,
                label = "In Progress",
                color = MaterialTheme.colorScheme.tertiary
            )
            StatItem(
                count = resolvedCount,
                label = "Resolved",
                color = Color.Green
            )
        }
    }
}

@Composable
fun StatItem(count: Int, label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$count $label",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 10.sp
        )
    }
}

@Composable
fun ReportDetailsCard(
    report: ReportPoint,
    onDismiss: () -> Unit,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        report.category.icon,
                        contentDescription = null,
                        tint = report.category.color,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = report.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status Badge
            StatusBadge(status = report.status)

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = report.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Timestamp
            Text(
                text = "Reported ${report.timestamp}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button
            Button(
                onClick = onNavigate,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Navigate to Location")
            }
        }
    }
}

@Composable
fun ReportsListPanel(
    reports: List<ReportPoint>,
    onReportClick: (ReportPoint) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.shadow(8.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reports",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // Reports List
            LazyColumn(
                modifier = Modifier.fillMaxHeight(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reports) { report ->
                    ReportListItem(
                        report = report,
                        onClick = { onReportClick(report) }
                    )
                }
            }
        }
    }
}

@Composable
fun ReportListItem(
    report: ReportPoint,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                report.category.icon,
                contentDescription = null,
                tint = report.category.color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = report.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = report.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusBadge(status = report.status, compact = true)
        }
    }
}

@Composable
fun StatusBadge(
    status: ReportStatus,
    compact: Boolean = false
) {
    val (color, text) = when (status) {
        ReportStatus.PENDING -> MaterialTheme.colorScheme.error to "Pending"
        ReportStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiary to "In Progress"
        ReportStatus.RESOLVED -> Color.Green to "Resolved"
    }

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = if (compact) 6.dp else 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.bodySmall,
            fontSize = if (compact) 10.sp else 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// Helper function to get marker icon based on category and status
private fun getMarkerIcon(context: Context, category: ReportCategory, status: ReportStatus): Drawable? {
    // You can customize this to use different icons for different categories and statuses
    return ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation)
}

// Data classes
data class ReportPoint(
    val id: Int,
    val lat: Double,
    val lon: Double,
    val title: String,
    val description: String,
    val category: ReportCategory,
    val timestamp: String,
    val status: ReportStatus
)

enum class ReportCategory(val icon: ImageVector, val color: Color) {
    DRAINAGE(Icons.Default.Water, Color.Blue),
    LIGHTING(Icons.Default.LightMode, Color.Yellow),
    WASTE(Icons.Default.Delete, Color.Green)
}

enum class ReportStatus {
    PENDING,
    IN_PROGRESS,
    RESOLVED
}

enum class MapType(val displayName: String, val tileSource: org.osmdroid.tileprovider.tilesource.ITileSource) {
    STANDARD("Map", TileSourceFactory.MAPNIK),
    SATELLITE("Satellite", TileSourceFactory.WIKIMEDIA)
}