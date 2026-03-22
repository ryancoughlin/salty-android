package com.example.saltyoffshore.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.data.UserPreferences

// Salty design tokens (matching AccountHubSheet)
private val SaltyBase = Color(0xFF0A0A0F)
private val SaltySunken = Color(0xFF141419)
private val SaltyTextPrimary = Color(0xFFFFFFFF)
private val SaltyTextSecondary = Color(0xFF8A8A9A)
private val SaltyAccent = Color(0xFF00D4AA)
private val SaltyBorderSubtle = Color(0x1AFAFEFF)

/**
 * Edit Profile bottom sheet matching iOS EditProfile.swift.
 * Three text fields (first name, last name, location) + save button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileSheet(
    sheetState: SheetState,
    preferences: UserPreferences?,
    isSaving: Boolean,
    onSave: (firstName: String?, lastName: String?, location: String?) -> Unit,
    onDismiss: () -> Unit
) {
    // Local form state, pre-filled from current preferences
    var firstName by remember(preferences) {
        mutableStateOf(preferences?.firstName ?: "")
    }
    var lastName by remember(preferences) {
        mutableStateOf(preferences?.lastName ?: "")
    }
    var location by remember(preferences) {
        mutableStateOf(preferences?.location ?: "")
    }

    val hasChanges = remember(firstName, lastName, location, preferences) {
        firstName != (preferences?.firstName ?: "") ||
                lastName != (preferences?.lastName ?: "") ||
                location != (preferences?.location ?: "")
    }

    val lastNameFocus = remember { FocusRequester() }
    val locationFocus = remember { FocusRequester() }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = SaltyTextPrimary,
        unfocusedTextColor = SaltyTextPrimary,
        focusedBorderColor = SaltyAccent,
        unfocusedBorderColor = SaltyBorderSubtle,
        focusedLabelColor = SaltyAccent,
        unfocusedLabelColor = SaltyTextSecondary,
        cursorColor = SaltyAccent,
        focusedContainerColor = SaltySunken,
        unfocusedContainerColor = SaltySunken,
        focusedPlaceholderColor = SaltyTextSecondary,
        unfocusedPlaceholderColor = SaltyTextSecondary
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SaltyBase
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // Header
            Text(
                text = "Personal Information",
                color = SaltyTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // First Name
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name") },
                placeholder = { Text("First Name") },
                singleLine = true,
                colors = textFieldColors,
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { lastNameFocus.requestFocus() }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Last Name
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name") },
                placeholder = { Text("Last Name") },
                singleLine = true,
                colors = textFieldColors,
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { locationFocus.requestFocus() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(lastNameFocus)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Location
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                placeholder = { Text("City, State") },
                singleLine = true,
                colors = textFieldColors,
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (hasChanges && !isSaving) {
                            onSave(
                                firstName.ifBlank { null },
                                lastName.ifBlank { null },
                                location.ifBlank { null }
                            )
                        }
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(locationFocus)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save button
            Button(
                onClick = {
                    onSave(
                        firstName.ifBlank { null },
                        lastName.ifBlank { null },
                        location.ifBlank { null }
                    )
                },
                enabled = hasChanges && !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SaltyAccent,
                    disabledContainerColor = SaltyAccent.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = if (isSaving) "Saving..." else "Save Changes",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
