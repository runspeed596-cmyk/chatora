package com.chatora.shared.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatora.shared.ui.theme.*
import com.chatora.shared.viewmodel.MatchState
import com.chatora.shared.viewmodel.MatchViewModel

@Composable
fun HomeScreen(
    username: String,
    matchViewModel: MatchViewModel,
    onLogout: () -> Unit
) {
    val matchState by matchViewModel.matchState.collectAsState()

    var selectedGender by remember { mutableStateOf("ALL") }
    var selectedCountry by remember { mutableStateOf("*") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header ───────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Chatora",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ChatoraPrimary
                    )
                    Text(
                        text = "Welcome, $username",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                OutlinedButton(
                    onClick = onLogout,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Logout")
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // ── Main Action Area ─────────────────────────────────
            Card(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Status indicator
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                when (matchState) {
                                    is MatchState.Idle -> Color(0xFF2D2D3F)
                                    is MatchState.Searching -> ChatoraPrimary.copy(alpha = 0.2f)
                                    is MatchState.Matched -> ChatoraGreen.copy(alpha = 0.2f)
                                    is MatchState.PartnerLeft -> ChatoraRed.copy(alpha = 0.2f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (matchState) {
                                is MatchState.Idle -> "🎥"
                                is MatchState.Searching -> "🔍"
                                is MatchState.Matched -> "✅"
                                is MatchState.PartnerLeft -> "👋"
                            },
                            fontSize = 40.sp
                        )
                    }

                    Text(
                        text = when (matchState) {
                            is MatchState.Idle -> "Ready to Chat?"
                            is MatchState.Searching -> "Finding a partner..."
                            is MatchState.Matched -> "Connected with ${(matchState as MatchState.Matched).partnerUsername}"
                            is MatchState.PartnerLeft -> "Partner disconnected"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    // ── Gender Filter ─────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("ALL" to "All", "MALE" to "Male", "FEMALE" to "Female").forEach { (value, label) ->
                            FilterChip(
                                selected = selectedGender == value,
                                onClick = { selectedGender = value },
                                label = { Text(label, fontSize = 13.sp) },
                                modifier = Modifier.padding(horizontal = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // ── Action Buttons ─────────────────────────────
                    when (matchState) {
                        is MatchState.Idle, is MatchState.PartnerLeft -> {
                            Button(
                                onClick = { matchViewModel.startSearching() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ChatoraPrimary
                                )
                            ) {
                                Text(
                                    text = "Start Video Chat",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                        is MatchState.Searching -> {
                            CircularProgressIndicator(
                                color = ChatoraPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                            OutlinedButton(
                                onClick = { matchViewModel.stop() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Cancel", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        is MatchState.Matched -> {
                            // Chat area placeholder — WebRTC streams go here
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF1A1A2E)
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "📹 Video Stream Area\n(WebRTC — platform-specific)",
                                        color = Color.White.copy(alpha = 0.5f),
                                        textAlign = TextAlign.Center,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { matchViewModel.findNext() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ChatoraPrimary
                                    )
                                ) {
                                    Text("Next", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { matchViewModel.stop() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Stop", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Footer ───────────────────────────────────────────
            Text(
                text = "Chatora v1.0 • Powered by Kotlin Multiplatform",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}
