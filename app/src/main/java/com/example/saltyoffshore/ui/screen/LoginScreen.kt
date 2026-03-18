package com.example.saltyoffshore.ui.screen

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saltyoffshore.auth.AuthError
import com.example.saltyoffshore.auth.AuthManager
import com.example.saltyoffshore.auth.ValidationHelper
import kotlinx.coroutines.launch

// Salty brand colors
private val SaltyBase = Color(0xFF0A0A0F)
private val SaltyAccent = Color(0xFF00D4AA)
private val SaltyBorder = Color(0xFF1A1A24)
private val SaltyTextPrimary = Color(0xFFFFFFFF)
private val SaltyTextSecondary = Color(0xFF8A8A9A)

@Composable
fun LoginScreen(
    onSignInSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToResetPassword: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    val isFormValid = email.isNotEmpty() &&
            ValidationHelper.isValidEmail(email) &&
            password.isNotEmpty()

    val showEmailError = hasAttemptedSubmit &&
            (!ValidationHelper.isValidEmail(email) || email.isEmpty())

    val showPasswordError = hasAttemptedSubmit && password.isEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SaltyBase)
    ) {
        // Subtle grid background
        SubtleGridBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Header
            HeaderSection()

            Spacer(modifier = Modifier.weight(1f))

            // Auth section
            AuthSection(
                email = email,
                onEmailChange = { email = it },
                password = password,
                onPasswordChange = { password = it },
                showEmailError = showEmailError,
                showPasswordError = showPasswordError,
                isLoading = AuthManager.isLoading,
                isFormValid = isFormValid,
                onForgotPassword = onNavigateToResetPassword,
                onSignIn = {
                    hasAttemptedSubmit = true
                    if (isFormValid) {
                        scope.launch {
                            try {
                                AuthManager.signIn(email, password)
                                onSignInSuccess()
                            } catch (e: AuthError) {
                                errorMessage = e.message
                                hasAttemptedSubmit = false
                            }
                        }
                    }
                }
            )

            // Error message
            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = msg,
                    color = Color.Red,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Footer
            FooterSection(onSignUp = onNavigateToSignUp)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SubtleGridBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val dotSpacing = 24.dp.toPx()
        val dotRadius = 1.dp.toPx()

        var x = 0f
        while (x < size.width) {
            var y = 0f
            while (y < size.height) {
                drawCircle(
                    color = SaltyTextPrimary.copy(alpha = 0.06f),
                    radius = dotRadius,
                    center = Offset(x, y)
                )
                y += dotSpacing
            }
            x += dotSpacing
        }
    }
}

@Composable
private fun HeaderSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Salty logo placeholder (48x48)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(SaltyAccent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "S",
                color = SaltyBase,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Tech badge
        TechBadge(text = "OCEAN INTELLIGENCE")

        // Tagline
        Text(
            text = "BUILT FOR PROS.\nDESIGNED FOR EVERYONE.",
            color = SaltyTextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp
        )
    }
}

@Composable
private fun TechBadge(text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(SaltyAccent.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(SaltyAccent)
        )
        Text(
            text = text,
            color = SaltyTextSecondary,
            fontSize = 10.sp,
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
private fun AuthSection(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    showEmailError: Boolean,
    showPasswordError: Boolean,
    isLoading: Boolean,
    isFormValid: Boolean,
    onForgotPassword: () -> Unit,
    onSignIn: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Divider with "or" (skipping Apple Sign In for now)
        DividerWithText(text = "Sign in with email")

        // Email field
        Column {
            SaltyTextField(
                value = email,
                onValueChange = onEmailChange,
                placeholder = "Email",
                keyboardType = KeyboardType.Email,
                isError = showEmailError
            )
            if (showEmailError) {
                Text(
                    text = if (email.isEmpty()) "Email is required" else "Please enter a valid email",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Password field
        Column {
            SaltyTextField(
                value = password,
                onValueChange = onPasswordChange,
                placeholder = "Password",
                keyboardType = KeyboardType.Password,
                isPassword = true,
                isError = showPasswordError,
                imeAction = ImeAction.Done
            )
            if (showPasswordError) {
                Text(
                    text = "Password is required",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Forgot password
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Forgot Password?",
                    color = SaltyTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onForgotPassword() }
                )
            }
        }

        // Sign in button
        SaltyButton(
            text = "Sign In",
            isLoading = isLoading,
            isEnabled = !isLoading && isFormValid,
            onClick = onSignIn
        )
    }
}

@Composable
private fun DividerWithText(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = SaltyBorder
        )
        Text(
            text = text,
            color = SaltyTextSecondary,
            fontSize = 12.sp
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = SaltyBorder
        )
    }
}

@Composable
private fun SaltyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    isError: Boolean = false,
    imeAction: ImeAction = ImeAction.Next
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(placeholder, color = SaltyTextSecondary)
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = SaltyTextPrimary,
            unfocusedTextColor = SaltyTextPrimary,
            focusedBorderColor = if (isError) Color.Red else SaltyAccent,
            unfocusedBorderColor = if (isError) Color.Red else SaltyBorder,
            cursorColor = SaltyAccent,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
private fun SaltyButton(
    text: String,
    isLoading: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SaltyAccent,
            contentColor = SaltyBase,
            disabledContainerColor = SaltyAccent.copy(alpha = 0.5f),
            disabledContentColor = SaltyBase.copy(alpha = 0.5f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = SaltyBase,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun FooterSection(onSignUp: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .clickable { onSignUp() },
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Don't have an account? ",
            color = SaltyTextSecondary,
            fontSize = 14.sp
        )
        Text(
            text = "Sign Up",
            color = SaltyTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
