package com.chatora.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chatora.app.R
import com.chatora.app.navigation.Screen
import com.chatora.app.ui.theme.AccentPink
import com.chatora.app.ui.theme.PrimaryNeon
import com.chatora.app.ui.theme.SecondaryNeon
import com.chatora.app.ui.viewmodels.SplashViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModel = hiltViewModel()
) {
    // Animation States
    val transition = rememberInfiniteTransition(label = "SplashLoop")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)), label = "Phase"
    )
    
    // Logo Animations
    val logoScale = remember { Animatable(0f) }
    val logoAlpha = remember { Animatable(0f) }
    val logoShimmer = remember { Animatable(-1f) } // -1 to 1 for shimmer position
    
    // Particle System
    val particles = remember { List(50) { Particle.random() } }
    
    LaunchedEffect(Unit) {
        // Sequence: 
        // 1. Logo Pop (0-600ms)
        launch {
            logoScale.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow))
        }
        launch {
            logoAlpha.animateTo(1f, tween(500))
        }
        
        // 2. Shimmer Sweep (600-1400ms)
        delay(600)
        logoShimmer.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
        
        // 3. Navigation (at 2000ms)
        delay(800)
        if (viewModel.isLoggedIn()) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        } else {
            navController.navigate(Screen.Auth.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050508)) // Deep Void Black
    ) {
        // 1. Dynamic Nebula Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = center
            
            // Rotating Nebula Clouds
            withTransform({
                rotate(phase)
                scale(1.5f, 1.5f)
            }) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF4A00E0).copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(center.x + 200, center.y - 200),
                        radius = 900f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFF00CC).copy(alpha = 0.1f), Color.Transparent),
                        center = Offset(center.x - 200, center.y + 200),
                        radius = 800f
                    )
                )
            }
            
            // Floating Particles
            particles.forEach { particle ->
                val currentX = center.x + particle.radius * cos(Math.toRadians(phase.toDouble() * particle.speed + particle.angle).toFloat())
                val currentY = center.y + particle.radius * sin(Math.toRadians(phase.toDouble() * particle.speed + particle.angle).toFloat())
                
                drawCircle(
                    color = Color.White.copy(alpha = particle.alpha * (0.5f + 0.5f * sin(Math.toRadians(phase * 5.0)).toFloat())),
                    radius = particle.size,
                    center = Offset(currentX, currentY)
                )
            }
        }

        // 2. Centered Logo with Effects
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Glow behind logo
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .graphicsLayer {
                            scaleX = logoScale.value * 1.5f
                            scaleY = logoScale.value * 1.5f
                        }
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(com.chatora.app.ui.theme.PrimaryNeon.copy(alpha = 0.3f), Color.Transparent)
                            ),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )

                // Main Logo Text
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = logoScale.value
                            scaleY = logoScale.value
                            alpha = logoAlpha.value
                        }
                        .drawWithCache {
                            val shimmerBrush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.0f),
                                    Color.White.copy(alpha = 0.8f),
                                    Color.White.copy(alpha = 0.0f)
                                ),
                                start = Offset(size.width * (logoShimmer.value - 0.5f), 0f),
                                end = Offset(size.width * (logoShimmer.value + 0.5f), size.height)
                            )
                            onDrawWithContent {
                                drawContent()
                                drawRect(shimmerBrush, blendMode = BlendMode.SrcAtop)
                            }
                        },
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Subtitle / Slogan
            Text(
                text = stringResource(R.string.onboarding_desc_1),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f * logoAlpha.value),
                letterSpacing = 1.sp, // Reduced letter spacing for longer text
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer {
                    alpha = logoAlpha.value
                    translationY = (1f - logoAlpha.value) * 50f
                }
            )
        }
    }
}

// Particle Data Class
data class Particle(
    val angle: Double,
    val radius: Float,
    val speed: Float,
    val size: Float,
    val alpha: Float
) {
    companion object {
        fun random(): Particle {
            return Particle(
                angle = Math.random() * 360,
                radius = (200 + Math.random() * 400).toFloat(), // 200..600
                speed = (0.5 + Math.random() * 1.5).toFloat(),   // 0.5..2.0
                size = (2 + Math.random() * 3).toFloat(),        // 2..5
                alpha = (0.3 + Math.random() * 0.5).toFloat()    // 0.3..0.8
            )
        }
    }
}
