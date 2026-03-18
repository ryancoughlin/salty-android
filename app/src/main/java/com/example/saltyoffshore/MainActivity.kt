package com.example.saltyoffshore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.saltyoffshore.ui.screen.MapScreen
import com.example.saltyoffshore.ui.screen.SettingsScreen
import com.example.saltyoffshore.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: AppViewModel = viewModel()
            var showSettings by remember { mutableStateOf(false) }

            if (showSettings) {
                SettingsScreen(
                    preferences = viewModel.userPreferences,
                    onDepthUnitsChanged = viewModel::updateDepthUnits,
                    onDistanceUnitsChanged = viewModel::updateDistanceUnits,
                    onSpeedUnitsChanged = viewModel::updateSpeedUnits,
                    onTemperatureUnitsChanged = viewModel::updateTemperatureUnits,
                    onSignOut = {
                        viewModel.signOut()
                        showSettings = false
                    },
                    onBack = { showSettings = false }
                )
            } else {
                MapScreen(
                    viewModel = viewModel,
                    onSettingsClick = { showSettings = true }
                )
            }
        }
    }
}
