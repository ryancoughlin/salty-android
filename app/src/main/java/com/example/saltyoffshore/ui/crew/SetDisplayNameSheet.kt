package com.example.saltyoffshore.ui.crew

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.ui.crew.components.DisplayNameFormView
import com.example.saltyoffshore.ui.theme.SaltyColors
import com.example.saltyoffshore.ui.theme.Spacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetDisplayNameSheet(
    onComplete: () -> Unit,
    onDismiss: () -> Unit,
    onSaveName: suspend (firstName: String, lastName: String) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val scope = rememberCoroutineScope()
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
        DisplayNameFormView(
            firstName = firstName,
            lastName = lastName,
            onFirstNameChange = { firstName = it; errorMessage = null },
            onLastNameChange = { lastName = it; errorMessage = null },
            isLoading = isLoading,
            onContinue = {
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    try {
                        onSaveName(firstName.trim(), lastName.trim())
                        onComplete()
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "Failed to save name"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.padding(Spacing.large),
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = Spacing.large),
            )
        }
    }
}
