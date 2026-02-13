package com.chatora.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chatora.app.R
import com.chatora.app.data.models.SubscriptionPlanDto
import com.chatora.app.ui.viewmodels.SubscriptionUiState
import com.chatora.app.ui.viewmodels.SubscriptionViewModel
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PremiumScreen(
    navController: NavController,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val paymentUrl by viewModel.paymentUrl.collectAsState()
    val activePlan by viewModel.activePlan.collectAsState()
    val showSuccess by viewModel.showSuccess.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(paymentUrl) {
        paymentUrl?.let {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
            context.startActivity(intent)
            viewModel.clearPaymentUrl()
            navController.popBackStack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Dynamic Background
        PremiumAnimatedBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                    
                }
            }
        ) { padding ->
            when (val state = uiState) {
                is SubscriptionUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFFFD700))
                    }
                }
                is SubscriptionUiState.Success -> {
                    if (showSuccess) {
                        PremiumSuccessView(onClose = { viewModel.dismissSuccess() })
                    } else {
                        PremiumContent(
                            padding = padding,
                            plans = state.plans,
                            activePlan = activePlan,
                            onPlanSelect = { viewModel.startPayment(it) }
                        )
                    }
                }
                is SubscriptionUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = Color.Red)
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun PremiumAnimatedBackground() {
    val infiniteTransition = rememberInfiniteTransition()
    val animateOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(Modifier.fillMaxSize()) {
        // Deep gradient base
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF140024), Color(0xFF0F0518), Color(0xFF000000))
                    )
                )
        )
        // Moving aurora effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = animateOffset * 0.2f }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF4A00E0).copy(alpha = 0.15f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(0f, 0f),
                        radius = 800f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = animateOffset * 0.1f }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFD700).copy(alpha = 0.1f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(1000f, 1000f),
                        radius = 800f
                    )
                )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PremiumContent(
    padding: PaddingValues,
    plans: List<SubscriptionPlanDto>,
    activePlan: String?,
    onPlanSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Icon
        PremiumHeaderIcon()

        Spacer(Modifier.height(24.dp))

        // Title text
        Text(
            text = stringResource(R.string.premium_unlock_title, stringResource(R.string.app_name)),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                brush = Brush.horizontalGradient(listOf(Color.White, Color(0xFFFFD700), Color.White))
            ),
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.premium_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(Modifier.weight(1f))

        // Features Carousel or List
        PremiumFeatureRow()

        Spacer(Modifier.weight(1f))

        // Plans Pager
        val pagerState = rememberPagerState(initialPage = 1, pageCount = { plans.size })
        val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
        var autoScrollActive by remember { mutableStateOf(true) }
        
        // Stop auto-scroll permanently if user drags
        LaunchedEffect(isDragged) {
            if (isDragged) autoScrollActive = false
        }
        
        // Auto-scroll loop
        LaunchedEffect(autoScrollActive) {
            if (autoScrollActive) {
                while (true) {
                    kotlinx.coroutines.delay(2000)
                    val nextPage = (pagerState.currentPage + 1) % plans.size
                    pagerState.animateScrollToPage(nextPage)
                }
            }
        }
        
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 48.dp),
            pageSpacing = 16.dp
        ) { page ->
            val plan = plans[page]
            val isSelected = pagerState.currentPage == page
            
            // Calculate scale transition
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val scale = 1f - (pageOffset.absoluteValue * 0.15f)
            val alpha = 1f - (pageOffset.absoluteValue * 0.5f)

            PremiumPlanCard(
                plan = plan,
                isSelected = isSelected,
                isActive = plan.name == activePlan,
                scale = scale,
                onSelect = { if (plan.name != activePlan) onPlanSelect(plan.name) }
            )
        }
        
        Spacer(Modifier.height(32.dp))

        // Selected Plan CTA
        val currentPlan = plans.getOrNull(pagerState.currentPage)
        Button(
            onClick = { currentPlan?.let { onPlanSelect(it.name) } },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(56.dp)
                .graphicsLayer {
                     shadowElevation = 16.dp.toPx()
                     shape = RoundedCornerShape(28.dp)
                     clip = true
                },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (currentPlan?.name == activePlan) 0.5f else 1f)
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFFFFD700), Color(0xFFFFA000))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (currentPlan?.name == activePlan) stringResource(R.string.currently_active).uppercase() else stringResource(R.string.continue_button),
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    letterSpacing = 1.sp
                )
            }
        }
        
        Text(
            text = stringResource(R.string.cancel_anytime),
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
fun PremiumHeaderIcon() {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = Modifier
            .size(100.dp)
            .scale(scale)
            .background(
                brush = Brush.linearGradient(
                    listOf(Color(0xFFFFD700).copy(alpha = 0.2f), Color.Transparent)
                ),
                shape = CircleShape
            )
            .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Diamond,
            contentDescription = null,
            tint = Color(0xFFFFD700),
            modifier = Modifier.size(50.dp)
        )
    }
}

@Composable
fun PremiumFeatureRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        PremiumFeatureItem(Icons.Default.Language, stringResource(R.string.feature_global))
        PremiumFeatureItem(Icons.Default.Face, stringResource(R.string.feature_gender))
        PremiumFeatureItem(Icons.Default.NoPhotography, stringResource(R.string.feature_no_ads))
        PremiumFeatureItem(Icons.Default.Hd, stringResource(R.string.feature_hd_video))
    }
}

@Composable
fun PremiumFeatureItem(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFF2A2A3E), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(text = label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PremiumPlanCard(
    plan: SubscriptionPlanDto,
    isSelected: Boolean,
    isActive: Boolean = false,
    scale: Float,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (isSelected) 1f else 0.6f
            }
            .fillMaxWidth()
            .height(280.dp)
            .clickable { onSelect() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                brush = if (isSelected) Brush.verticalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA000))) 
                        else Brush.verticalGradient(listOf(Color.Gray.copy(0.3f), Color.Gray.copy(0.1f))),
                shape = RoundedCornerShape(24.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1E1E2C) else Color(0xFF161622)
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 12.dp else 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isSelected) {
                // Glow effect
                Box(modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFFFFD700).copy(alpha = 0.15f), Color.Transparent)
                        )
                    )
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                if (isActive) {
                     ContainerBadge(stringResource(R.string.active_check), Color(0xFF00E676))
                } else if (plan.months in 1..3) {
                     ContainerBadge(stringResource(R.string.badge_most_popular), Color(0xFFFFD700))
                } else if (plan.months >= 12) {
                     ContainerBadge(stringResource(R.string.badge_best_value), Color(0xFF00E676))
                } else {
                    Spacer(Modifier.height(20.dp))
                }
                
                val localizedPlanName = when (plan.months) {
                    1 -> stringResource(R.string.plan_monthly)
                    3 -> stringResource(R.string.plan_quarterly)
                    12 -> stringResource(R.string.plan_yearly)
                    else -> plan.name
                }

                Text(
                    text = localizedPlanName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$${plan.priceUsd}",
                        color = if (isSelected) Color(0xFFFFD700) else Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 36.sp
                    )
                    Text(
                        text = stringResource(R.string.per_period),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                     // Removed "Save %" text as requested
                }
                
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun ContainerBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(50),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PremiumSuccessView(onClose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF00E676),
            modifier = Modifier.size(100.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.premium_success_title),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.premium_success_desc),
            color = Color.White.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text(stringResource(R.string.start_chatting), color = Color.Black)
        }
    }
}
