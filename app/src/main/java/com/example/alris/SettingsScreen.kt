package com.example.alris

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val colorScheme = MaterialTheme.colorScheme

    // Settings state
    var enableNotifications by remember { mutableStateOf(true) }
    var enableLocationSharing by remember { mutableStateOf(true) }
    var autoUpload by remember { mutableStateOf(false) }
    var highQualityImages by remember { mutableStateOf(true) }
    var darkMode by remember { mutableStateOf(false) }
    var saveToGallery by remember { mutableStateOf(true) }

    // Animation states
    val headerOffset by animateFloatAsState(
        targetValue = 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colorScheme.primary.copy(alpha = 0.1f),
                        colorScheme.background
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Header Section
            item {
                SettingsHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = headerOffset.dp)
                )
            }

            // Camera Settings Section
            item {
                SettingsSection(
                    title = "Camera Settings",
                    icon = Icons.Default.CameraAlt,
                    settings = listOf(
                        ToggleSetting(
                            title = "High Quality Images",
                            description = "Capture images in maximum resolution",
                            icon = Icons.Default.HighQuality,
                            isChecked = highQualityImages,
                            onCheckedChange = { highQualityImages = it }
                        ),
                        ToggleSetting(
                            title = "Auto Upload",
                            description = "Automatically upload captured images",
                            icon = Icons.Default.CloudUpload,
                            isChecked = autoUpload,
                            onCheckedChange = { autoUpload = it }
                        ),
                        ToggleSetting(
                            title = "Save to Gallery",
                            description = "Save captured images to device gallery",
                            icon = Icons.Default.PhotoLibrary,
                            isChecked = saveToGallery,
                            onCheckedChange = { saveToGallery = it }
                        )
                    )
                )
            }

            // Privacy Settings Section
            item {
                SettingsSection(
                    title = "Privacy & Location",
                    icon = Icons.Default.Security,
                    settings = listOf(
                        ToggleSetting(
                            title = "Location Sharing",
                            description = "Include GPS coordinates with uploads",
                            icon = Icons.Default.LocationOn,
                            isChecked = enableLocationSharing,
                            onCheckedChange = { enableLocationSharing = it }
                        ),
                        NavigationSetting(
                            title = "Data Usage",
                            description = "Manage upload preferences",
                            icon = Icons.Default.DataUsage,
                            onClick = { /* Navigate to data usage settings */ }
                        )
                    )
                )
            }

            // Notification Settings Section
            item {
                SettingsSection(
                    title = "Notifications",
                    icon = Icons.Default.Notifications,
                    settings = listOf(
                        ToggleSetting(
                            title = "Push Notifications",
                            description = "Receive upload status notifications",
                            icon = Icons.Default.NotificationsActive,
                            isChecked = enableNotifications,
                            onCheckedChange = { enableNotifications = it }
                        )
                    )
                )
            }

            // Appearance Settings Section
            item {
                SettingsSection(
                    title = "Appearance",
                    icon = Icons.Default.Palette,
                    settings = listOf(
                        ToggleSetting(
                            title = "Dark Mode",
                            description = "Use dark theme",
                            icon = Icons.Default.DarkMode,
                            isChecked = darkMode,
                            onCheckedChange = { darkMode = it }
                        )
                    )
                )
            }

            // About Settings Section
            item {
                SettingsSection(
                    title = "About",
                    icon = Icons.Default.Info,
                    settings = listOf(
                        NavigationSetting(
                            title = "Version",
                            description = "1.0.0",
                            icon = Icons.Default.AppSettingsAlt,
                            onClick = { /* Show version info */ }
                        ),
                        NavigationSetting(
                            title = "Privacy Policy",
                            description = "View our privacy policy",
                            icon = Icons.Default.Policy,
                            onClick = { /* Open privacy policy */ }
                        ),
                        NavigationSetting(
                            title = "Terms of Service",
                            description = "View terms and conditions",
                            icon = Icons.Default.Gavel,
                            onClick = { /* Open terms */ }
                        ),
                        NavigationSetting(
                            title = "Contact Support",
                            description = "Get help and support",
                            icon = Icons.Default.ContactSupport,
                            onClick = { /* Open support */ }
                        )
                    )
                )
            }

            // Spacer at bottom
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SettingsHeader(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Customize your experience",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    settings: List<SettingItem>,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Settings Items
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy
                    )
                ) + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    settings.forEachIndexed { index, setting ->
                        if (index > 0) {
                            Divider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        }
                        SettingItemRow(setting = setting)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingItemRow(setting: SettingItem) {
    when (setting) {
        is ToggleSetting -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        setting.icon,
                        contentDescription = setting.title,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = setting.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (setting.description.isNotEmpty()) {
                            Text(
                                text = setting.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Switch(
                    checked = setting.isChecked,
                    onCheckedChange = setting.onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
        is NavigationSetting -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { setting.onClick() }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        setting.icon,
                        contentDescription = setting.title,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = setting.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (setting.description.isNotEmpty()) {
                            Text(
                                text = setting.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Navigate",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Data classes for settings
sealed class SettingItem

data class ToggleSetting(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isChecked: Boolean,
    val onCheckedChange: (Boolean) -> Unit
) : SettingItem()

data class NavigationSetting(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val onClick: () -> Unit
) : SettingItem()