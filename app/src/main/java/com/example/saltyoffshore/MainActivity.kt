package com.example.saltyoffshore

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
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
import com.example.saltyoffshore.ui.crew.CrewListSheet
import com.example.saltyoffshore.ui.savedmaps.SavedMapsListSheet
import com.example.saltyoffshore.ui.screen.AccountHubSheet
import com.example.saltyoffshore.ui.screen.EditProfileSheet
import com.example.saltyoffshore.ui.screen.FTUXRegionSelectionScreen
import com.example.saltyoffshore.ui.screen.RegionSelectionSheet
import com.example.saltyoffshore.ui.screen.LoginScreen
import com.example.saltyoffshore.ui.screen.MapScreen
import com.example.saltyoffshore.ui.screen.ResetPasswordScreen
import com.example.saltyoffshore.ui.screen.SignUpScreen
import androidx.compose.material3.ExperimentalMaterial3Api
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
                    viewModel.handleAuthReady()
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

    // Network monitoring: sync pending changes when connectivity restores
    LaunchedEffect(Unit) {
        viewModel.observeNetworkState()
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

        // Reset Password sheet
        if (showResetPasswordSheet) {
            ResetPasswordScreen(
                onBack = { showResetPasswordSheet = false }
            )
        }
    }
}

/**
 * Tracks which sheet is currently visible.
 * Only one sheet at a time — Material Design "dismiss then navigate" pattern.
 */
private enum class SettingsSheet {
    None, Settings, EditProfile, RegionSelection, Crews, SavedMaps
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedContent(viewModel: AppViewModel) {
    var hasAppeared by remember { mutableStateOf(false) }

    // Sheet navigation state — only one sheet visible at a time (Material Design pattern).
    // Sub-pages dismiss the settings sheet first, then open their own sheet.
    // On sub-page dismiss, settings re-opens.
    var activeSheet by remember { mutableStateOf<SettingsSheet>(SettingsSheet.None) }

    val accountSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val editProfileSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val regionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Trigger appear animation
    LaunchedEffect(Unit) {
        hasAppeared = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map screen (full-screen)
        MapScreen(
            viewModel = viewModel,
            onSettingsClick = { activeSheet = SettingsSheet.Settings }
        )
    }

    // AccountHub bottom sheet
    if (activeSheet == SettingsSheet.Settings) {
        AccountHubSheet(
            sheetState = accountSheetState,
            preferences = viewModel.userPreferences,
            onDepthUnitsChanged = viewModel::updateDepthUnits,
            onDistanceUnitsChanged = viewModel::updateDistanceUnits,
            onSpeedUnitsChanged = viewModel::updateSpeedUnits,
            onTemperatureUnitsChanged = viewModel::updateTemperatureUnits,
            onGpsFormatChanged = viewModel::updateGpsFormat,
            onMapThemeChanged = viewModel::updateMapTheme,
            onEditProfile = { activeSheet = SettingsSheet.EditProfile },
            onPreferredRegion = { activeSheet = SettingsSheet.RegionSelection },
            onManageCrews = { activeSheet = SettingsSheet.Crews },
            onManageSavedMaps = { activeSheet = SettingsSheet.SavedMaps },
            onSignOut = { viewModel.signOut() },
            onDeleteAccount = { viewModel.deleteAccount() },
            onDismiss = { activeSheet = SettingsSheet.None }
        )
    }

    // Edit Profile — opens after settings dismisses, returns to settings on close
    if (activeSheet == SettingsSheet.EditProfile) {
        EditProfileSheet(
            sheetState = editProfileSheetState,
            preferences = viewModel.userPreferences,
            isSaving = viewModel.isSavingProfile,
            onSave = { firstName, lastName, location ->
                viewModel.updateProfile(firstName, lastName, location)
                activeSheet = SettingsSheet.Settings
            },
            onDismiss = { activeSheet = SettingsSheet.Settings }
        )
    }

    // Region Selection — opens after settings dismisses, returns to settings on close
    if (activeSheet == SettingsSheet.RegionSelection) {
        RegionSelectionSheet(
            sheetState = regionSheetState,
            regionGroups = viewModel.regionGroups,
            selectedRegionId = viewModel.preferredRegionId,
            onRegionSelected = viewModel::updatePreferredRegion,
            onDismiss = { activeSheet = SettingsSheet.Settings }
        )
    }

    // Crews sheet
    if (activeSheet == SettingsSheet.Crews) {
        CrewListSheet(
            crews = viewModel.crews,
            crewWaypoints = viewModel.crewWaypoints,
            savedMaps = viewModel.savedMaps,
            selectedCrew = viewModel.selectedCrew,
            selectedCrewMembers = viewModel.selectedCrewMembers,
            isCreator = viewModel.selectedCrew?.let { viewModel.isCreator(it) } ?: false,
            hasDisplayName = viewModel.hasDisplayName,
            onSelectCrew = viewModel::selectCrew,
            onCreateCrew = { name, onSuccess -> viewModel.createCrew(name, onSuccess) },
            onJoinCrew = { code, onSuccess, onError -> viewModel.joinCrew(code, onSuccess, onError) },
            onLeaveCrew = { crew, onComplete -> viewModel.leaveCrew(crew, onComplete) },
            onDeleteCrew = { crew, onComplete -> viewModel.deleteCrew(crew, onComplete) },
            onRemoveMember = { crewId, memberId -> viewModel.removeMember(crewId, memberId) },
            onUpdateCrewName = { crewId, newName, onSuccess -> viewModel.updateCrewName(crewId, newName, onSuccess) },
            onSaveName = { firstName, lastName -> viewModel.saveName(firstName, lastName) },
            onWaypointTap = { /* TODO: navigate to waypoint on map */ },
            onLoadMap = { /* TODO: load saved map configuration */ },
            onDismiss = { activeSheet = SettingsSheet.Settings },
        )
    }

    // Saved Maps sheet
    if (activeSheet == SettingsSheet.SavedMaps) {
        SavedMapsListSheet(
            savedMaps = viewModel.savedMaps,
            crews = viewModel.crews,
            currentUserId = AuthManager.currentUserId,
            isLoading = viewModel.isLoadingSavedMaps,
            onLoadMap = { /* TODO: load saved map configuration */ },
            onDeleteMap = { viewModel.deleteSavedMap(it) },
            onShareToCrew = { mapId, crewId, name -> viewModel.shareMapWithCrew(mapId, crewId, name) },
            onUnshare = { viewModel.unshareMap(it) },
            onDismiss = { activeSheet = SettingsSheet.Settings },
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
