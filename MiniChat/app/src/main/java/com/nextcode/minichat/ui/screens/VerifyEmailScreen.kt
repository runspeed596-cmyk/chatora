package com.nextcode.minichat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nextcode.minichat.R
import com.nextcode.minichat.ui.theme.PrimaryNeon
import com.nextcode.minichat.ui.theme.SecondaryNeon
import com.nextcode.minichat.ui.viewmodels.AuthEvent
import com.nextcode.minichat.ui.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyEmailScreen(
    onVerificationSuccess: () -> Unit,
    onBackToAuth: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var code by remember { mutableStateOf("") }

    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated && !state.needsVerification) {
            onVerificationSuccess()
        }
    }

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isDark) {
                     Brush.verticalGradient(colors = listOf(PrimaryNeon, Color(0xFF121212)))
                } else {
                     Brush.verticalGradient(colors = listOf(Color(0xFFF5F7FA), Color(0xFFE0E0E0)))
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBackToAuth) {
                    Icon(
                        Icons.Default.ArrowBack, 
                        contentDescription = null, 
                        tint = if (isDark) Color.White else Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                tint = SecondaryNeon,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.verify_email),
                color = if (isDark) Color.White else Color.Black,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${stringResource(R.string.verify_code_sent)}\n${state.email}",
                color = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.6f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = code,
                onValueChange = { newValue -> if (newValue.length <= 6) code = newValue },
                label = { Text(stringResource(R.string.verification_code)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SecondaryNeon,
                    unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.3f),
                    focusedLabelColor = SecondaryNeon,
                    unfocusedLabelColor = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f),
                    focusedTextColor = if (isDark) Color.White else Color.Black,
                    unfocusedTextColor = if (isDark) Color.White else Color.Black
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.onEvent(AuthEvent.VerifyEmail(code)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryNeon),
                enabled = !state.isLoading && code.length == 6
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(stringResource(R.string.verify), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(
                onClick = { viewModel.onEvent(AuthEvent.ResendCode) },
                enabled = state.resendTimer == 0
            ) {
                if (state.resendTimer > 0) {
                     val minutes = state.resendTimer / 60
                     val seconds = state.resendTimer % 60
                     Text(
                        text = "${stringResource(R.string.resend_code)} (${String.format("%02d:%02d", minutes, seconds)})", 
                        color = if (isDark) Color.Gray else Color.Gray
                     )
                } else {
                     Text(stringResource(R.string.resend_code), color = SecondaryNeon)
                }
            }
        }
    }

    if (state.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(AuthEvent.DismissError) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(AuthEvent.DismissError) }) {
                    Text(stringResource(R.string.ok))
                }
            },
            title = { Text(stringResource(R.string.error)) },
            text = { Text(state.error ?: "") }
        )
    }
}
