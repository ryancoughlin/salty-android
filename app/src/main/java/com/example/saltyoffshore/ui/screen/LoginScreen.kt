package com.example.saltyoffshore.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.VisualTransformation
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
private val SaltySunken = Color(0xFF141419)
private val SaltyTextPrimary = Color(0xFFFFFFFF)
private val SaltyTextSecondary = Color(0xFF8A8A9A)

/**
 * LoginScreen matching iOS LoginView.swift.
 *
 * Three-section layout:
 * - HeaderSection: logo (72x72) + heading
 * - AuthSection: "Continue with email" collapsed -> expand animation -> email/password form
 * - FooterSection: "Don't have an account? Sign Up"
 *
 * Auth state machine in MainActivity handles routing -- no onSignInSuccess callback needed.
 * Supabase sessionStatus flow will automatically route to authenticated content.
 */
@Composable
fun LoginScreen(
    onNavigateToSignUp: () -> Unit,
    onNavigateToResetPassword: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showEmailForm by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

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
        SubtleGridBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Matches iOS: .padding(.top, 120)
            Spacer(modifier = Modifier.height(120.dp))

            // Header Section
            HeaderSection()

            // Matches iOS: Spacer().frame(height: 80)
            Spacer(modifier = Modifier.height(80.dp))

            // Auth Section
            AuthSection(
                email = email,
                onEmailChange = { email = it },
                password = password,
                onPasswordChange = { password = it },
                showEmailError = showEmailError,
                showPasswordError = showPasswordError,
                showEmailForm = showEmailForm,
                isLoading = AuthManager.isLoading,
                isFormValid = isFormValid,
                onContinueWithEmail = { showEmailForm = true },
                onForgotPassword = onNavigateToResetPassword,
                onSignIn = {
                    hasAttemptedSubmit = true
                    if (isFormValid) {
                        scope.launch {
                            try {
                                AuthManager.signIn(email, password)
                                // Auth state machine will handle navigation
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

            // Footer Section
            FooterSection(onSignUp = onNavigateToSignUp)

            Spacer(modifier = Modifier.height(48.dp))
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
        // Salty logo placeholder (72x72, matching iOS)
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SaltyAccent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "S",
                color = SaltyBase,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Heading matching iOS: "Built for pros.\nDesigned for everyone."
        Text(
            text = "Built for pros.\nDesigned for everyone.",
            color = SaltyTextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp
        )
    }
}

/**
 * Auth section matching iOS AuthSection.
 *
 * Initially shows "Continue with email" secondary button.
 * On tap, animates open (0.2s ease-in-out) the email/password form + Sign In button.
 */
@Composable
private fun AuthSection(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    showEmailError: Boolean,
    showPasswordError: Boolean,
    showEmailForm: Boolean,
    isLoading: Boolean,
    isFormValid: Boolean,
    onContinueWithEmail: () -> Unit,
    onForgotPassword: () -> Unit,
    onSignIn: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // "or" divider (Apple Sign-In would go above this on iOS)
        DividerWithText(text = "or")

        // Collapsed state: "Continue with email" button
        // Expanded state: email/password form + Sign In button
        AnimatedVisibility(
            visible = showEmailForm,
            enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

                // Sign In button (primary)
                SaltyPrimaryButton(
                    text = "Sign In",
                    isLoading = isLoading,
                    isEnabled = !isLoading && isFormValid,
                    onClick = onSignIn
                )
            }
        }

        // "Continue with email" button (shown when form is collapsed)
        if (!showEmailForm) {
            SaltySecondaryButton(
                text = "Continue with email",
                icon = Icons.Default.Email,
                onClick = onContinueWithEmail
            )
        }
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
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
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

/** Primary button -- accent background, dark text */
@Composable
private fun SaltyPrimaryButton(
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

/** Secondary button -- sunken background, primary text, optional leading icon */
@Composable
private fun SaltySecondaryButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SaltySunken,
            contentColor = SaltyTextPrimary
        )
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = SaltyTextPrimary
            )
            Spacer(modifier = Modifier.size(8.dp))
        }
        Text(
            text = text,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp
        )
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
