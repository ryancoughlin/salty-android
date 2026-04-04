package com.example.saltyoffshore

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.example.saltyoffshore.ui.screen.LaunchView
import com.example.saltyoffshore.ui.screen.SignUpScreen
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.saltyoffshore.viewmodel.AppViewModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus

private const val TAG = "MainActivity"

/**
 * MainActivity matching iOS SaltyOffshoreApp.swift auth state machine.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
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

    // Launch animation — matches iOS LaunchView overlay
    var showLaunch by remember { mutableStateOf(true) }

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
                        viewModel.signOut()
                    }
                    isAuthenticated = false
                }
                is SessionStatus.Initializing -> { }
                is SessionStatus.RefreshFailure -> {
                    Log.w(TAG, "Auth session refresh failed: ${status.cause}")
                }
            }
        }
    }

    // Network monitoring
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

        if (showSignUpSheet) {
            SignUpScreen(
                onSignUpSuccess = { showSignUpSheet = false },
                onNavigateBack = { showSignUpSheet = false }
            )
        }

        if (showResetPasswordSheet) {
            ResetPasswordScreen(
                onBack = { showResetPasswordSheet = false }
            )
        }
    }

    // Launch animation overlay
    if (showLaunch) {
        LaunchView(onFinished = { showLaunch = false })
    }
}

private enum class SettingsSheet {
    None, Settings, EditProfile, RegionSelection, Crews, SavedMaps
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedContent(viewModel: AppViewModel) {
    var hasAppeared by remember { mutableStateOf(false) }
    var activeSheet by remember { mutableStateOf<SettingsSheet>(SettingsSheet.None) }

    val accountSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val editProfileSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val regionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Collect store states
    val regionState by viewModel.regionStore.state.collectAsState()
    val crewState by viewModel.crewStore.state.collectAsState()
    val prefsState by viewModel.userPreferencesStore.state.collectAsState()
    val savedMapsState by viewModel.savedMapsStore.state.collectAsState()

    LaunchedEffect(Unit) {
        hasAppeared = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapScreen(
            viewModel = viewModel,
            onSettingsClick = { activeSheet = SettingsSheet.Settings }
        )
    }

    // AccountHub bottom sheet
    if (activeSheet == SettingsSheet.Settings) {
        AccountHubSheet(
            sheetState = accountSheetState,
            preferences = prefsState.userPreferences,
            onDepthUnitsChanged = viewModel.userPreferencesStore::updateDepthUnits,
            onDistanceUnitsChanged = viewModel.userPreferencesStore::updateDistanceUnits,
            onSpeedUnitsChanged = viewModel.userPreferencesStore::updateSpeedUnits,
            onTemperatureUnitsChanged = viewModel.userPreferencesStore::updateTemperatureUnits,
            onGpsFormatChanged = viewModel.userPreferencesStore::updateGpsFormat,
            onMapThemeChanged = viewModel.userPreferencesStore::updateMapTheme,
            onEditProfile = { activeSheet = SettingsSheet.EditProfile },
            onPreferredRegion = { activeSheet = SettingsSheet.RegionSelection },
            onManageCrews = { activeSheet = SettingsSheet.Crews },
            onManageSavedMaps = { activeSheet = SettingsSheet.SavedMaps },
            onSignOut = { viewModel.signOut() },
            onDeleteAccount = { viewModel.deleteAccount() },
            onDismiss = { activeSheet = SettingsSheet.None }
        )
    }

    // Edit Profile
    if (activeSheet == SettingsSheet.EditProfile) {
        EditProfileSheet(
            sheetState = editProfileSheetState,
            preferences = prefsState.userPreferences,
            isSaving = prefsState.isSavingProfile,
            onSave = { firstName, lastName, location ->
                viewModel.userPreferencesStore.updateProfile(firstName, lastName, location)
                activeSheet = SettingsSheet.Settings
            },
            onDismiss = { activeSheet = SettingsSheet.Settings }
        )
    }

    // Region Selection
    if (activeSheet == SettingsSheet.RegionSelection) {
        RegionSelectionSheet(
            sheetState = regionSheetState,
            regionGroups = regionState.regionGroups,
            selectedRegionId = regionState.preferredRegionId,
            onRegionSelected = viewModel.userPreferencesStore::updatePreferredRegion,
            onDismiss = { activeSheet = SettingsSheet.Settings }
        )
    }

    // Crews sheet
    if (activeSheet == SettingsSheet.Crews) {
        CrewListSheet(
            crews = crewState.crews,
            crewWaypoints = crewState.crewWaypoints,
            savedMaps = savedMapsState.savedMaps,
            selectedCrew = crewState.selectedCrew,
            selectedCrewMembers = crewState.selectedCrewMembers,
            isCreator = crewState.selectedCrew?.let { viewModel.crewStore.isCreator(it) } ?: false,
            hasDisplayName = prefsState.hasDisplayName,
            onSelectCrew = viewModel.crewStore::selectCrew,
            onCreateCrew = { name, onSuccess -> viewModel.crewStore.createCrew(name, onSuccess) },
            onJoinCrew = { code, onSuccess, onError -> viewModel.crewStore.joinCrew(code, onSuccess, onError) },
            onLeaveCrew = { crew, onComplete -> viewModel.crewStore.leaveCrew(crew, onComplete) },
            onDeleteCrew = { crew, onComplete -> viewModel.crewStore.deleteCrew(crew, onComplete) },
            onRemoveMember = { crewId, memberId -> viewModel.crewStore.removeMember(crewId, memberId) },
            onUpdateCrewName = { crewId, newName, onSuccess -> viewModel.crewStore.updateCrewName(crewId, newName, onSuccess) },
            onSaveName = { firstName, lastName -> viewModel.userPreferencesStore.saveName(firstName, lastName) },
            onWaypointTap = { /* TODO: navigate to waypoint on map */ },
            onLoadMap = { /* TODO: load saved map configuration */ },
            onDismiss = { activeSheet = SettingsSheet.Settings },
        )
    }

    // Saved Maps sheet
    if (activeSheet == SettingsSheet.SavedMaps) {
        SavedMapsListSheet(
            savedMaps = savedMapsState.savedMaps,
            crews = crewState.crews,
            currentUserId = AuthManager.currentUserId,
            isLoading = savedMapsState.isLoadingSavedMaps,
            onLoadMap = { /* TODO: load saved map configuration */ },
            onDeleteMap = { viewModel.savedMapsStore.deleteSavedMap(it) },
            onShareToCrew = { mapId, crewId, name -> viewModel.savedMapsStore.shareMapWithCrew(mapId, crewId, name) },
            onUnshare = { viewModel.savedMapsStore.unshareMap(it) },
            onDismiss = { activeSheet = SettingsSheet.Settings },
        )
    }

    // FTUX: blocking full-screen dialog when no preferred region
    if (regionState.hasCompletedInitialLoad && regionState.preferredRegionId == null) {
        FTUXRegionSelectionScreen(
            regionGroups = regionState.regionGroups,
            loadingRegionId = regionState.ftuxLoadingRegionId,
            onRegionSelected = viewModel.regionStore::onFTUXRegionSelected
        )
    }
}

/**
 * Refresh data when app returns to foreground.
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
                if (isAuthenticated) {
                    val regionId = viewModel.regionStore.state.value.selectedRegion?.id
                    if (regionId != null) {
                        Log.d(TAG, "Foreground resume: refreshing region")
                        viewModel.regionStore.onRegionSelected(regionId)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
