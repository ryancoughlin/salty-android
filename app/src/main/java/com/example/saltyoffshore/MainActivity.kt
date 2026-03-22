package com.example.saltyoffshore

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.saltyoffshore.auth.AuthManager
import com.example.saltyoffshore.auth.SupabaseClientProvider
import com.example.saltyoffshore.ui.screen.AccountHubSheet
import com.example.saltyoffshore.ui.screen.FTUXRegionSelectionScreen
import com.example.saltyoffshore.ui.screen.LoginScreen
import com.example.saltyoffshore.ui.screen.MapScreen
import com.example.saltyoffshore.ui.screen.SignUpScreen
import com.example.saltyoffshore.viewmodel.AppViewModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus

private const val TAG = "MainActivity"

/**
 * MainActivity matching iOS SaltyOffshoreApp.swift auth state machine.
 *
 * Collects Supabase sessionStatus flow:
 * - Authenticated -> show authenticated content (MapScreen + TopBar + AccountHub)
 * - NotAuthenticated -> show LoginScreen
 *
 * Auth state changes (sign out, session expiry) automatically route UI.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SaltyApp()
        }
    }
}

@Composable
private fun SaltyApp() {
    val viewModel: AppViewModel = viewModel()

    // Auth state machine
    var isAuthenticated by remember { mutableStateOf(AuthManager.hasStoredSession) }
    var showSignUpSheet by remember { mutableStateOf(false) }
    var showResetPasswordSheet by remember { mutableStateOf(false) }

    // Listen for Supabase auth state changes (matching iOS listenForAuthChanges)
    LaunchedEffect(Unit) {
        SupabaseClientProvider.client.auth.sessionStatus.collect { status ->
            Log.d(TAG, "Auth status: $status")
            when (status) {
                is SessionStatus.Authenticated -> {
                    isAuthenticated = true
                }
                is SessionStatus.NotAuthenticated -> {
                    if (isAuthenticated) {
                        // Was authenticated, now signed out -> clear state
                        viewModel.signOut()
                    }
                    isAuthenticated = false
                }
                is SessionStatus.Initializing -> {
                    // Loading from storage, keep current state
                }
                is SessionStatus.RefreshFailure -> {
                    Log.w(TAG, "Auth session refresh failed: ${status.cause}")
                    // Keep current state -- may recover on next attempt
                }
            }
        }
    }

    // Scene lifecycle: refresh data when returning to foreground
    ForegroundRefreshEffect(viewModel = viewModel, isAuthenticated = isAuthenticated)

    if (isAuthenticated) {
        AuthenticatedContent(viewModel = viewModel)
    } else {
        LoginScreen(
            onNavigateToSignUp = { showSignUpSheet = true },
            onNavigateToResetPassword = { showResetPasswordSheet = true }
        )

        // Sign Up sheet
        if (showSignUpSheet) {
            SignUpScreen(
                onSignUpSuccess = { showSignUpSheet = false },
                onNavigateBack = { showSignUpSheet = false }
            )
        }

        // TODO: Reset Password sheet, Update Password sheet
    }
}

/**
 * Authenticated content: MapScreen + TopBar overlay + AccountHub sheet + FTUX dialog.
 * Matches iOS ContentView structure.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedContent(viewModel: AppViewModel) {
    var showingAccountSheet by remember { mutableStateOf(false) }
    var hasAppeared by remember { mutableStateOf(false) }

    val accountSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // Trigger appear animation
    LaunchedEffect(Unit) {
        hasAppeared = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map screen (full-screen)
        MapScreen(
            viewModel = viewModel,
            onSettingsClick = { showingAccountSheet = true }
        )
    }

    // AccountHub bottom sheet
    if (showingAccountSheet) {
        AccountHubSheet(
            sheetState = accountSheetState,
            preferences = viewModel.userPreferences,
            onDepthUnitsChanged = viewModel::updateDepthUnits,
            onDistanceUnitsChanged = viewModel::updateDistanceUnits,
            onSpeedUnitsChanged = viewModel::updateSpeedUnits,
            onTemperatureUnitsChanged = viewModel::updateTemperatureUnits,
            onSignOut = { viewModel.signOut() },
            onDismiss = { showingAccountSheet = false }
        )
    }

    // FTUX: blocking full-screen dialog when no preferred region
    if (viewModel.hasCompletedInitialLoad && viewModel.preferredRegionId == null) {
        FTUXRegionSelectionScreen(
            regionGroups = viewModel.regionGroups,
            loadingRegionId = viewModel.ftuxLoadingRegionId,
            onRegionSelected = viewModel::onFTUXRegionSelected
        )
    }
}

/**
 * Refresh data when app returns to foreground.
 * Matches iOS: onChange(of: scenePhase) where newPhase == .active.
 */
@Composable
private fun ForegroundRefreshEffect(viewModel: AppViewModel, isAuthenticated: Boolean) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var isFirstResume by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (isFirstResume) {
                    isFirstResume = false
                    return@LifecycleEventObserver
                }
                if (isAuthenticated && viewModel.selectedRegion != null) {
                    Log.d(TAG, "Foreground resume: refreshing region")
                    viewModel.selectedRegion?.id?.let { viewModel.onRegionSelected(it) }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
