package com.example.saltyoffshore.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.data.DepthUnits
import com.example.saltyoffshore.data.DistanceUnits
import com.example.saltyoffshore.data.SpeedUnits
import com.example.saltyoffshore.data.TemperatureUnits
import com.example.saltyoffshore.data.UserPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences?,
    onDepthUnitsChanged: (DepthUnits) -> Unit,
    onDistanceUnitsChanged: (DistanceUnits) -> Unit,
    onSpeedUnitsChanged: (SpeedUnits) -> Unit,
    onTemperatureUnitsChanged: (TemperatureUnits) -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            // Units Section
            SettingsSection(title = "Units") {
                // Depth Units
                SettingsDropdown(
                    label = "Depth",
                    currentValue = DepthUnits.fromRawValue(preferences?.depthUnits)?.displayName ?: "Feet",
                    options = DepthUnits.entries.map { it.displayName },
                    onOptionSelected = { displayName ->
                        DepthUnits.entries.find { it.displayName == displayName }?.let(onDepthUnitsChanged)
                    }
                )

                // Distance Units
                SettingsDropdown(
                    label = "Distance",
                    currentValue = DistanceUnits.fromRawValue(preferences?.distanceUnits)?.displayName ?: "Nautical Miles",
                    options = DistanceUnits.entries.map { it.displayName },
                    onOptionSelected = { displayName ->
                        DistanceUnits.entries.find { it.displayName == displayName }?.let(onDistanceUnitsChanged)
                    }
                )

                // Speed Units
                SettingsDropdown(
                    label = "Speed",
                    currentValue = SpeedUnits.fromRawValue(preferences?.speedUnits)?.displayName ?: "Knots",
                    options = SpeedUnits.entries.map { it.displayName },
                    onOptionSelected = { displayName ->
                        SpeedUnits.entries.find { it.displayName == displayName }?.let(onSpeedUnitsChanged)
                    }
                )

                // Temperature Units
                SettingsDropdown(
                    label = "Temperature",
                    currentValue = TemperatureUnits.fromRawValue(preferences?.temperatureUnits)?.displayName ?: "Fahrenheit",
                    options = TemperatureUnits.entries.map { it.displayName },
                    onOptionSelected = { displayName ->
                        TemperatureUnits.entries.find { it.displayName == displayName }?.let(onTemperatureUnitsChanged)
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Sign Out Button
            Button(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626)
                )
            ) {
                Text("Sign Out", color = Color.White)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        )

        content()

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SettingsDropdown(
    label: String,
    currentValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = currentValue,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }

    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
}
