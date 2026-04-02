package com.example.saltyoffshore.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.ui.components.NotificationSettingsView
import com.example.saltyoffshore.data.DepthUnits
import com.example.saltyoffshore.data.DistanceUnits
import com.example.saltyoffshore.data.GpsFormat
import com.example.saltyoffshore.data.MapTheme
import com.example.saltyoffshore.data.SpeedUnits
import com.example.saltyoffshore.data.TemperatureUnits
import com.example.saltyoffshore.data.UserPreferences
import com.example.saltyoffshore.ui.components.DatasetGuideView
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.coroutines.launch

/**
 * AccountHub bottom sheet matching iOS AccountHub.swift.
 *
 * Sections in order:
 * 1. Welcome
 * 2. Account Settings (4 navigation rows)
 * 3. Notifications
 * 4. Units (migrated from SettingsScreen)
 * 5. Map Theme
 * 6. Dataset Information
 * 7. About Salty (4 rows)
 * 8. Sign Out
 * 9. Delete Account (2-step confirm)
 * 10. Diagnostics
 * 11. Version footer
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountHubSheet(
    sheetState: SheetState,
    preferences: UserPreferences?,
    onDepthUnitsChanged: (DepthUnits) -> Unit,
    onDistanceUnitsChanged: (DistanceUnits) -> Unit,
    onSpeedUnitsChanged: (SpeedUnits) -> Unit,
    onTemperatureUnitsChanged: (TemperatureUnits) -> Unit,
    onGpsFormatChanged: (GpsFormat) -> Unit,
    onMapThemeChanged: (MapTheme) -> Unit,
    onEditProfile: () -> Unit,
    onPreferredRegion: () -> Unit,
    onManageWaypoints: () -> Unit = {},
    onImportGPX: () -> Unit = {},
    onManageCrews: () -> Unit = {},
    onManageSavedMaps: () -> Unit = {},
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            )
        }
    ) {
        AccountHubContent(
            preferences = preferences,
            onDepthUnitsChanged = onDepthUnitsChanged,
            onDistanceUnitsChanged = onDistanceUnitsChanged,
            onSpeedUnitsChanged = onSpeedUnitsChanged,
            onTemperatureUnitsChanged = onTemperatureUnitsChanged,
            onGpsFormatChanged = onGpsFormatChanged,
            onMapThemeChanged = onMapThemeChanged,
            onEditProfile = onEditProfile,
            onPreferredRegion = onPreferredRegion,
            onManageWaypoints = onManageWaypoints,
            onImportGPX = onImportGPX,
            onManageCrews = onManageCrews,
            onManageSavedMaps = onManageSavedMaps,
            onDeleteAccount = onDeleteAccount,
            onSignOut = {
                scope.launch {
                    onSignOut()
                    onDismiss()
                }
            },
            onDone = {
                scope.launch {
                    sheetState.hide()
                    onDismiss()
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountHubContent(
    preferences: UserPreferences?,
    onDepthUnitsChanged: (DepthUnits) -> Unit,
    onDistanceUnitsChanged: (DistanceUnits) -> Unit,
    onSpeedUnitsChanged: (SpeedUnits) -> Unit,
    onTemperatureUnitsChanged: (TemperatureUnits) -> Unit,
    onGpsFormatChanged: (GpsFormat) -> Unit,
    onMapThemeChanged: (MapTheme) -> Unit,
    onEditProfile: () -> Unit,
    onPreferredRegion: () -> Unit,
    onManageWaypoints: () -> Unit,
    onImportGPX: () -> Unit,
    onManageCrews: () -> Unit,
    onManageSavedMaps: () -> Unit,
    onDeleteAccount: () -> Unit,
    onSignOut: () -> Unit,
    onDone: () -> Unit
) {
    val scrollState = rememberScrollState()

    var showingDeleteConfirmation by remember { mutableStateOf(false) }
    var showingFinalConfirmation by remember { mutableStateOf(false) }
    var confirmationText by remember { mutableStateOf("") }
    var showingDatasetGuide by remember { mutableStateOf(false) }
    var showingNotificationSettings by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
    ) {
        // Title bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Settings",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = onDone) {
                Text("Done", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
            }
        }

        WelcomeSection()
        Spacer(modifier = Modifier.height(16.dp))

        AccountSettingsSection(
            onEditProfile = onEditProfile,
            onPreferredRegion = onPreferredRegion
        )
        Spacer(modifier = Modifier.height(16.dp))

        NotificationsSection(
            expanded = showingNotificationSettings,
            onToggleExpanded = { showingNotificationSettings = !showingNotificationSettings }
        )
        Spacer(modifier = Modifier.height(16.dp))

        UnitsSection(
            preferences = preferences,
            onDepthUnitsChanged = onDepthUnitsChanged,
            onDistanceUnitsChanged = onDistanceUnitsChanged,
            onSpeedUnitsChanged = onSpeedUnitsChanged,
            onTemperatureUnitsChanged = onTemperatureUnitsChanged,
            onGpsFormatChanged = onGpsFormatChanged
        )
        Spacer(modifier = Modifier.height(16.dp))

        MapThemeSection(
            preferences = preferences,
            onMapThemeChanged = onMapThemeChanged
        )
        Spacer(modifier = Modifier.height(16.dp))

        MyWaypointsSection(
            onManageWaypoints = onManageWaypoints,
            onImportGPX = onImportGPX
        )
        Spacer(modifier = Modifier.height(16.dp))

        // My Crews section
        SectionHeader(title = "MY CREWS")
        Spacer(modifier = Modifier.height(8.dp))
        SunkenCard {
            NavigationRow(
                icon = Icons.Default.Group,
                title = "Manage Crews",
                onClick = onManageCrews
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Saved Maps section
        SectionHeader(title = "MY MAPS")
        Spacer(modifier = Modifier.height(8.dp))
        SunkenCard {
            NavigationRow(
                icon = Icons.Default.Map,
                title = "Saved Maps",
                onClick = onManageSavedMaps
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        DatasetInfoSection(onDatasetGuide = { showingDatasetGuide = true })
        Spacer(modifier = Modifier.height(16.dp))

        AboutSection()
        Spacer(modifier = Modifier.height(24.dp))

        // Sign Out Button
        Button(
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Delete Account Button
        Button(
            onClick = { showingDeleteConfirmation = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Delete Account", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(16.dp))
        DiagnosticsSection()
        Spacer(modifier = Modifier.height(16.dp))
        VersionFooter()
        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showingDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showingDeleteConfirmation = false },
            title = { Text("Delete Account") },
            text = {
                Text("Are you sure you want to delete your account? This action cannot be undone and will permanently delete all your data.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showingDeleteConfirmation = false
                        showingFinalConfirmation = true
                    }
                ) {
                    Text("Continue", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showingDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showingFinalConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showingFinalConfirmation = false
                confirmationText = ""
            },
            title = { Text("Final Confirmation") },
            text = {
                Column {
                    Text("Type \"DELETE\" to permanently delete your account and all associated data.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirmationText,
                        onValueChange = { confirmationText = it },
                        placeholder = { Text("Type DELETE to confirm") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showingFinalConfirmation = false
                        confirmationText = ""
                        onDeleteAccount()
                    },
                    enabled = confirmationText == "DELETE"
                ) {
                    Text("Delete Account", color = if (confirmationText == "DELETE") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showingFinalConfirmation = false
                    confirmationText = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dataset Guide full-screen bottom sheet
    if (showingDatasetGuide) {
        ModalBottomSheet(
            onDismissRequest = { showingDatasetGuide = false },
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dataset Guide",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = { showingDatasetGuide = false }) {
                        Text("Done", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                // Guide content
                DatasetGuideView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(600.dp)
                )
            }
        }
    }
}

// =============================================================================
// MARK: - Sections
// =============================================================================

@Composable
private fun WelcomeSection() {
    val context = LocalContext.current
    SunkenCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Welcome to Salty",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Thanks for being part of the Salty community. We built this app because we love fishing and wanted better tools for the water.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:support@saltyoffshore.com")
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text("Share Feedback", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun AccountSettingsSection(
    onEditProfile: () -> Unit,
    onPreferredRegion: () -> Unit
) {
    SectionHeader(title = "ACCOUNT SETTINGS")
    Spacer(modifier = Modifier.height(8.dp))
    SunkenCard {
        NavigationRow(
            icon = Icons.Default.Map,
            title = "Preferred Region",
            onClick = onPreferredRegion
        )
        RowDivider()
        NavigationRow(
            icon = Icons.Default.Person,
            title = "Edit Profile",
            onClick = onEditProfile
        )
        RowDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Subscription",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Coming Soon",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun NotificationsSection(
    expanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    SectionHeader(title = "NOTIFICATIONS")
    Spacer(modifier = Modifier.height(8.dp))
    SunkenCard {
        NavigationRow(
            icon = Icons.Default.Notifications,
            title = "Notification Settings",
            onClick = onToggleExpanded
        )
        if (expanded) {
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            NotificationSettingsView()
        }
    }
}

@Composable
private fun UnitsSection(
    preferences: UserPreferences?,
    onDepthUnitsChanged: (DepthUnits) -> Unit,
    onDistanceUnitsChanged: (DistanceUnits) -> Unit,
    onSpeedUnitsChanged: (SpeedUnits) -> Unit,
    onTemperatureUnitsChanged: (TemperatureUnits) -> Unit,
    onGpsFormatChanged: (GpsFormat) -> Unit
) {
    SectionHeader(title = "UNITS")
    Spacer(modifier = Modifier.height(8.dp))
    SunkenCard {
        UnitPickerRow(
            label = "Temperature",
            currentValue = TemperatureUnits.fromRawValue(preferences?.temperatureUnits)?.displayName ?: "Fahrenheit",
            options = TemperatureUnits.entries.map { it.displayName },
            onSelected = { displayName ->
                TemperatureUnits.entries.find { it.displayName == displayName }?.let(onTemperatureUnitsChanged)
            }
        )
        RowDivider()
        UnitPickerRow(
            label = "Depth",
            currentValue = DepthUnits.fromRawValue(preferences?.depthUnits)?.displayName ?: "Feet",
            options = DepthUnits.entries.map { it.displayName },
            onSelected = { displayName ->
                DepthUnits.entries.find { it.displayName == displayName }?.let(onDepthUnitsChanged)
            }
        )
        RowDivider()
        UnitPickerRow(
            label = "Distance",
            currentValue = DistanceUnits.fromRawValue(preferences?.distanceUnits)?.displayName ?: "Nautical Miles",
            options = DistanceUnits.entries.map { it.displayName },
            onSelected = { displayName ->
                DistanceUnits.entries.find { it.displayName == displayName }?.let(onDistanceUnitsChanged)
            }
        )
        RowDivider()
        UnitPickerRow(
            label = "Speed",
            currentValue = SpeedUnits.fromRawValue(preferences?.speedUnits)?.displayName ?: "Knots",
            options = SpeedUnits.entries.map { it.displayName },
            onSelected = { displayName ->
                SpeedUnits.entries.find { it.displayName == displayName }?.let(onSpeedUnitsChanged)
            }
        )
        RowDivider()
        val currentGpsFormat = GpsFormat.fromRawValue(preferences?.gpsFormat) ?: GpsFormat.DMM
        UnitPickerRow(
            label = "GPS Format",
            currentValue = currentGpsFormat.displayName,
            options = GpsFormat.entries.map { it.displayName },
            onSelected = { displayName ->
                GpsFormat.entries.find { it.displayName == displayName }?.let(onGpsFormatChanged)
            }
        )
        Text(
            text = currentGpsFormat.example,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 16.dp, bottom = 12.dp, end = 16.dp)
        )
    }
}

@Composable
private fun MapThemeSection(
    preferences: UserPreferences?,
    onMapThemeChanged: (MapTheme) -> Unit
) {
    val selectedTheme = MapTheme.fromRawValue(preferences?.mapTheme) ?: MapTheme.LIGHT

    SectionHeader(title = "MAP THEME")
    Spacer(modifier = Modifier.height(8.dp))
    SunkenCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MapTheme.userSelectable.forEach { theme ->
                    val isSelected = theme == selectedTheme
                    Button(
                        onClick = { onMapThemeChanged(theme) },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )
                    ) {
                        Text(
                            text = theme.displayName,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = selectedTheme.description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun MyWaypointsSection(
    onManageWaypoints: () -> Unit,
    onImportGPX: () -> Unit
) {
    SectionHeader(title = "MY WAYPOINTS")
    Spacer(modifier = Modifier.height(8.dp))
    SunkenCard {
        NavigationRow(
            icon = Icons.Default.Place,
            title = "Manage Waypoints",
            onClick = onManageWaypoints
        )
        RowDivider()
        NavigationRow(
            icon = Icons.Default.Info,
            title = "Import GPX File",
            onClick = onImportGPX
        )
    }
}

@Composable
private fun DatasetInfoSection(onDatasetGuide: () -> Unit) {
    SectionHeader(title = "DATASET INFORMATION")
    Spacer(modifier = Modifier.height(8.dp))
    SunkenCard {
        NavigationRow(
            icon = Icons.Default.Info,
            title = "Dataset Guide",
            onClick = onDatasetGuide
        )
    }
}

@Composable
private fun AboutSection() {
    val context = LocalContext.current

    SectionHeader(title = "ABOUT SALTY")
    Spacer(modifier = Modifier.height(8.dp))
    SunkenCard {
        NavigationRow(
            icon = Icons.Outlined.AutoAwesome,
            title = "Take the Tour",
            onClick = { /* Future: tour */ }
        )
        RowDivider()
        NavigationRow(
            icon = Icons.Default.PrivacyTip,
            title = "Privacy Policy",
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://saltyoffshore.com/#/privacy"))
                context.startActivity(intent)
            }
        )
        RowDivider()
        NavigationRow(
            icon = Icons.Outlined.Description,
            title = "Terms of Use",
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://saltyoffshore.com/#/terms-of-use"))
                context.startActivity(intent)
            }
        )
        RowDivider()
        NavigationRow(
            icon = Icons.Default.QuestionMark,
            title = "Get Help",
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:support@saltyoffshore.com?subject=Help%20Request")
                }
                context.startActivity(intent)
            }
        )
    }
}

@Composable
private fun DiagnosticsSection() {
    SectionHeader(title = "DIAGNOSTICS")
    Spacer(modifier = Modifier.height(8.dp))
    SunkenCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tile Server", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.weight(1f))
            Text("OK", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
        RowDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Map Cache", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.weight(1f))
            Text("OK", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun VersionFooter() {
    val context = LocalContext.current
    val packageInfo = remember {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    val versionName = packageInfo.versionName ?: "Unknown"
    val versionCode = packageInfo.longVersionCode

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Version $versionName",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = "Build $versionCode",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

// =============================================================================
// MARK: - Shared Components
// =============================================================================

@Composable
private fun SunkenCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        content()
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun NavigationRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "\u203A",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun UnitPickerRow(
    label: String,
    currentValue: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = currentValue,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }

        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 52.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    )
}
