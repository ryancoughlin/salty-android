package com.example.saltyoffshore.ui.crew.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.saltyoffshore.ui.theme.SaltyColors
import com.example.saltyoffshore.ui.theme.SaltyType
import com.example.saltyoffshore.ui.theme.Spacing

@Composable
fun DisplayNameFormView(
    firstName: String,
    lastName: String,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    isLoading: Boolean,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = SaltyColors.accent,
        unfocusedBorderColor = SaltyColors.borderSubtle,
        focusedLabelColor = SaltyColors.accent,
        unfocusedLabelColor = SaltyColors.textSecondary,
        cursorColor = SaltyColors.accent,
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(SaltyColors.sunken),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = SaltyColors.accent,
                modifier = Modifier.size(28.dp),
            )
        }

        Spacer(Modifier.height(Spacing.large))

        Text(
            text = "What's Your Name?",
            style = SaltyType.heading,
            color = SaltyColors.textPrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(Spacing.small))

        Text(
            text = "Your name is visible to crew members",
            style = SaltyType.bodySmall,
            color = SaltyColors.textSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(Spacing.extraLarge))

        OutlinedTextField(
            value = firstName,
            onValueChange = onFirstNameChange,
            label = { Text("First Name") },
            singleLine = true,
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(Spacing.medium))

        OutlinedTextField(
            value = lastName,
            onValueChange = onLastNameChange,
            label = { Text("Last Name") },
            singleLine = true,
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(Spacing.extraLarge))

        Button(
            onClick = onContinue,
            enabled = firstName.isNotBlank() && !isLoading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = SaltyColors.accent,
                contentColor = SaltyColors.buttonText,
            ),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = SaltyColors.buttonText,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Continue", style = SaltyType.bodySmall)
            }
        }
    }
}
