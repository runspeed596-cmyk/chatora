package com.chatora.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chatora.app.R
import com.chatora.app.ui.theme.PrimaryNeon
import com.chatora.app.ui.theme.SecondaryNeon
import com.chatora.app.ui.viewmodels.AuthEvent
import com.chatora.app.ui.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onNavigateToVerify: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated && !state.needsVerification) {
            onAuthSuccess()
        }
    }

    LaunchedEffect(state.needsVerification) {
        if (state.needsVerification) {
            onNavigateToVerify()
            viewModel.onEvent(AuthEvent.ResetVerificationState)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF0F0F0F) else Color(0xFFF5F7FA))
    ) {
        // Decorative Animated-like Blobs (Glassmorphism backdrop)
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-50).dp, y = (-50).dp)
                .background(PrimaryNeon.copy(alpha = 0.15f), CircleShape)
                .blur(80.dp)
        )
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 50.dp, y = 50.dp)
                .background(SecondaryNeon.copy(alpha = 0.15f), CircleShape)
                .blur(80.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .navigationBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Header Section
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.app_name),
                    color = PrimaryNeon,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = if (state.isLoginMode) stringResource(R.string.welcome_back) else stringResource(R.string.create_account),
                    color = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.6f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // Main Glass Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(32.dp),
                color = if (isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.7f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    if (isDark) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.3f)
                ),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!state.isLoginMode) {
                        CustomTextField(
                            value = state.username,
                            onValueChange = { viewModel.onEvent(AuthEvent.UsernameChanged(it)) },
                            label = stringResource(R.string.username),
                            icon = Icons.Default.Person,
                            isDark = isDark
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    CustomTextField(
                        value = state.email,
                        onValueChange = { viewModel.onEvent(AuthEvent.EmailChanged(it)) },
                        label = stringResource(R.string.email),
                        icon = Icons.Default.AlternateEmail,
                        keyboardType = KeyboardType.Email,
                        isDark = isDark
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    CustomTextField(
                        value = state.password,
                        onValueChange = { viewModel.onEvent(AuthEvent.PasswordChanged(it)) },
                        label = stringResource(R.string.password),
                        icon = Icons.Default.LockOpen,
                        isPassword = true,
                        isDark = isDark
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { 
                            if (state.isLoginMode) viewModel.onEvent(AuthEvent.EmailSignIn)
                            else viewModel.onEvent(AuthEvent.EmailSignUp)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryNeon,
                            contentColor = Color.White
                        ),
                        enabled = !state.isLoading
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = if (state.isLoginMode) stringResource(R.string.login) else stringResource(R.string.sign_up),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Social Login Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Divider(modifier = Modifier.weight(1f), color = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f))
                        Text(
                            text = stringResource(R.string.or),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.4f),
                            fontSize = 12.sp
                        )
                        Divider(modifier = Modifier.weight(1f), color = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Google Login Button (Premium Style)
                    OutlinedButton(
                        onClick = { viewModel.onEvent(AuthEvent.GoogleSignIn(context)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, 
                            if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isDark) Color.White else Color.Black
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle, // Placeholder for Google Icon
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = if (isDark) Color.White else PrimaryNeon
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.google_sign_in),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Bottom Footer
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TextButton(onClick = { viewModel.onEvent(AuthEvent.ToggleMode) }) {
                    Text(
                        text = if (state.isLoginMode) stringResource(R.string.no_account) 
                               else stringResource(R.string.have_account),
                        color = SecondaryNeon,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (state.error != null) {
        val errorMsg = state.error ?: ""
        // Map common backend errors to localized strings if possible
        val localizedError = when {
            errorMsg.contains("invalid", ignoreCase = true) -> stringResource(R.string.error_invalid_credentials)
            errorMsg.contains("network", ignoreCase = true) -> stringResource(R.string.error_network)
            errorMsg.contains("verify", ignoreCase = true) -> stringResource(R.string.error_email_not_verified)
            else -> errorMsg
        }

        AlertDialog(
            onDismissRequest = { viewModel.onEvent(AuthEvent.DismissError) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(AuthEvent.DismissError) }) {
                    Text(stringResource(R.string.ok))
                }
            },
            title = { Text(stringResource(R.string.error)) },
            text = { Text(localizedError) },
            shape = RoundedCornerShape(24.dp),
            containerColor = if (isDark) Color(0xFF1E1E1E) else Color.White
        )
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    isDark: Boolean
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        leadingIcon = { Icon(icon, contentDescription = null, tint = PrimaryNeon) },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryNeon,
            unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f),
            focusedLabelColor = PrimaryNeon,
            unfocusedLabelColor = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.4f),
            focusedTextColor = if (isDark) Color.White else Color.Black,
            unfocusedTextColor = if (isDark) Color.White else Color.Black,
            cursorColor = PrimaryNeon
        ),
        singleLine = true
    )
}
