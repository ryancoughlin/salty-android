package com.example.saltyoffshore.ui.crew

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.data.waypoint.Crew
import com.example.saltyoffshore.data.waypoint.CrewService
import com.example.saltyoffshore.ui.theme.SaltyColors
import com.example.saltyoffshore.ui.theme.SaltyType
import com.example.saltyoffshore.ui.theme.Spacing
import com.example.saltyoffshore.ui.theme.SplineSansMono
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCrewSheet(
    onDismiss: () -> Unit,
    onCrewCreated: (Crew) -> Unit,
    onSaveName: suspend (firstName: String, lastName: String) -> Unit,
    hasDisplayName: Boolean,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val scope = rememberCoroutineScope()
    var crewName by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }
    var createdCrew by remember { mutableStateOf<Crew?>(null) }
    var showDisplayNameSheet by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun doCreate() {
        scope.launch {
            isCreating = true
            errorMessage = null
            try {
                val crew = CrewService.createCrew(crewName.trim())
                createdCrew = crew
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to create crew"
            } finally {
                isCreating = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SaltyColors.overlay,
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            )
        },
    ) {
        AnimatedContent(
            targetState = createdCrew,
            transitionSpec = {
                fadeIn() + scaleIn(initialScale = 0.92f) togetherWith fadeOut()
            },
            label = "createCrewContent",
        ) { crew ->
            if (crew == null) {
                CreateFormContent(
                    crewName = crewName,
                    onCrewNameChange = { crewName = it; errorMessage = null },
                    isCreating = isCreating,
                    errorMessage = errorMessage,
                    onCreateClick = {
                        if (!hasDisplayName) {
                            showDisplayNameSheet = true
                        } else {
                            doCreate()
                        }
                    },
                )
            } else {
                SuccessContent(
                    crew = crew,
                    onDone = {
                        onCrewCreated(crew)
                        onDismiss()
                    },
                )
            }
        }
    }

    if (showDisplayNameSheet) {
        SetDisplayNameSheet(
            onComplete = { showDisplayNameSheet = false; doCreate() },
            onDismiss = { showDisplayNameSheet = false },
            onSaveName = onSaveName,
        )
    }
}

@Composable
private fun CreateFormContent(
    crewName: String,
    onCrewNameChange: (String) -> Unit,
    isCreating: Boolean,
    errorMessage: String?,
    onCreateClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.large),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(SaltyColors.sunken),
        ) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                tint = SaltyColors.accent,
                modifier = Modifier.size(32.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Name Your Crew",
            style = SaltyType.heading,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Create a crew to share waypoints with your team",
            style = SaltyType.bodySmall,
            color = SaltyColors.textSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = crewName,
            onValueChange = onCrewNameChange,
            label = { Text("Crew Name") },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        )

        if (errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = errorMessage,
                style = SaltyType.caption,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onCreateClick,
            enabled = crewName.isNotBlank() && !isCreating,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isCreating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Create Crew")
            }
        }
    }
}

@Composable
private fun SuccessContent(
    crew: Crew,
    onDone: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }
    val formattedCode = crew.inviteCode.take(3) + " \u2013 " + crew.inviteCode.drop(3)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.large),
    ) {
        val successGreen = Color(0xFF4CAF50)

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(successGreen.copy(alpha = 0.15f)),
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = successGreen,
                modifier = Modifier.size(32.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Crew Created!",
            style = SaltyType.heading,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Share this code to invite members",
            style = SaltyType.bodySmall,
            color = SaltyColors.textSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .background(SaltyColors.sunken)
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraLarge)
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = formattedCode,
                fontFamily = SplineSansMono,
                fontSize = 28.sp,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(24.dp))

        FilledTonalButton(
            onClick = {
                clipboardManager.setText(AnnotatedString(crew.inviteCode))
                copied = true
                scope.launch {
                    delay(2000)
                    copied = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(if (copied) "Copied!" else "Copy Code")
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Done")
        }
    }
}
