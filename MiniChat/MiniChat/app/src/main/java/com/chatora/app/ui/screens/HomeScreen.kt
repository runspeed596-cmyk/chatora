package com.chatora.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chatora.app.R
import com.chatora.app.data.Country
import com.chatora.app.navigation.Screen
import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.chatora.app.ui.theme.PrimaryNeon
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import android.app.DownloadManager
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.chatora.app.data.remote.ApiConstants
import com.chatora.app.ui.viewmodels.ChatMessage
import com.chatora.app.ui.viewmodels.MatchUiState
import com.chatora.app.ui.viewmodels.MatchViewModel
import kotlinx.coroutines.launch
import org.webrtc.SurfaceViewRenderer
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: MatchViewModel = hiltViewModel()
) {
    val uiState by viewModel.matchState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val selectedCountry by viewModel.selectedCountry.collectAsState()
    val selectedGender by viewModel.selectedGender.collectAsState()
    val hasStarted by viewModel.hasStarted.collectAsState()
    
    var showCountrySheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    
    var messageText by remember { mutableStateOf("") }
    val isDark = isSystemInDarkTheme()
    
    val uiEvent by viewModel.uiEvent.collectAsState()
    
    LaunchedEffect(uiEvent) {
        uiEvent?.let { event ->
            when (event) {
                is com.chatora.app.ui.viewmodels.UiEvent.ShowPremiumRequired -> {
                    navController.navigate(Screen.Premium.route)
                }
            }
            viewModel.clearUiEvent()
        }
    }

    val primaryBlue = Color(0xFF4A76FD)
    val context = LocalContext.current
    
    // Gender Selection State — genderDialogShown persists across recomposition & config changes
    var genderDialogShown by rememberSaveable { mutableStateOf(false) }
    
    // Derive dialog visibility directly — no LaunchedEffect, no snapshotFlow, no race conditions
    val showGenderDialog = user != null && user?.gender == "UNSPECIFIED" && !genderDialogShown

    // Media State
    var selectedMediaUrl by remember { mutableStateOf<String?>(null) }
    var selectedMediaType by remember { mutableStateOf<String?>(null) }
    
    val mediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.uploadAndSendMedia(it) }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = if (isDark) {
                        listOf(Color(0xFF000000), Color(0xFF1A1A2E)) // Deep Black/Blue for Dark Mode
                    } else {
                        listOf(Color(0xFFF0F4FF), Color(0xFFE0E7FF))
                    }
                )
            ),
        containerColor = Color.Transparent,
        topBar = {
            PremiumHomeTopBar(
                user = user,
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onPremiumClick = { navController.navigate(Screen.Premium.route) }
            )
        }
    ) { padding ->
        if (showGenderDialog) {
            GenderSelectionDialog(
                onGenderSelected = { gender ->
                    viewModel.updateUserGender(gender)
                    genderDialogShown = true  // This automatically hides the dialog (derived val becomes false)
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val hasStarted by viewModel.hasStarted.collectAsState()
            val isVideoReady by viewModel.remoteVideoReady.collectAsState()
            // 1. Partner Video / Searching Area
            Box(
                modifier = Modifier
                    .weight(3f) // Optimized height (was 5f, too big)
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                PremiumVideoContainer(
                        uiState = uiState,
                        isVideoReady = isVideoReady,
                        onRetry = { viewModel.findMatch() },
                        initRemoteVideo = { viewModel.initRemoteVideo(it) },
                        releaseRemoteVideo = { viewModel.releaseRemoteVideo(it) }
                    )
                }

                // 2. Filter Bar
                PremiumFilterBar(
                    selectedGender = selectedGender,
                    selectedCountry = selectedCountry,
                    isPremium = user?.isPremium ?: false,
                    onGenderToggle = { 
                        val next = if (selectedGender == "All") "Male" else if (selectedGender == "Male") "Female" else "All"
                        viewModel.updateGender(next)
                    },
                    onCountryClick = { showCountrySheet = true }
                )

                // 3. Chat Area
                Box(modifier = Modifier.weight(1f)) {
                    val foundState = uiState as? MatchUiState.Found
                    PremiumChatArea(
                        messages = messages,
                        partnerIp = foundState?.partnerIp ?: "",
                        partnerCountryCode = foundState?.partnerCountryCode ?: "",
                        onMediaClick = { url, type ->
                            selectedMediaUrl = url
                            selectedMediaType = type
                        }
                    )
                }

                // 4. Control Section
                PremiumControlSection(
                    messageText = messageText,
                    onMessageChange = { messageText = it },
                    onSend = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(messageText)
                            messageText = ""
                        }
                    },
                    onAttachClick = { 
                        mediaLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageAndVideo
                            )
                        ) 
                    },
                    onStop = { viewModel.stopMatching() },
                    onNext = { viewModel.nextMatch() },
                    onSwitchCamera = { viewModel.switchCamera() },
                    initLocalVideo = { viewModel.initLocalVideo(it) },
                    hasStarted = hasStarted
                )
            }
        }

        if (showCountrySheet) {
            ModalBottomSheet(
                onDismissRequest = { showCountrySheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                CountryList(
                    countries = viewModel.countries,
                    isPremium = user?.isPremium ?: false,
                    onSelect = {
                        viewModel.updateCountry(it)
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showCountrySheet = false
                        }
                    }
                )
            }
        }

        selectedMediaUrl?.let { url ->
            val type = selectedMediaType ?: "image/*"
            if (type.startsWith("video")) {
                VideoPlayerDialog(url = url, onDismiss = { selectedMediaUrl = null })
            } else {
                PhotoViewerDialog(url = url, onDismiss = { selectedMediaUrl = null })
            }
        }
    }

@Composable
fun PremiumHomeTopBar(user: com.chatora.app.data.User?, onSettingsClick: () -> Unit, onPremiumClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = (user?.username ?: "G").take(1).uppercase(),
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = user?.username ?: stringResource(R.string.guest),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
                // Sleeker Premium Badge: Elegant Gradient with Glow
                val infiniteTransition = rememberInfiniteTransition(label = "badgeGlow")
                val glowAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.6f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "glow"
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (user?.isPremium == true) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .height(30.dp)
                            .clickable { onPremiumClick() }
                            .then(
                                if (user?.isPremium == true) {
                                    Modifier.background(
                                        brush = Brush.linearGradient(
                                            listOf(Color(0xFFFFD700).copy(alpha = 0.9f), Color(0xFFFFA000).copy(alpha = 0.9f))
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ).border(1.dp, Color(0xFFFFD700).copy(alpha = glowAlpha), RoundedCornerShape(16.dp))
                                } else Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Diamond, null, 
                                tint = if (user?.isPremium == true) Color.Black else Color(0xFFFFD700), 
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (user?.isPremium == true) stringResource(R.string.premium_badge).uppercase() else stringResource(R.string.get_premium).uppercase(),
                                fontWeight = FontWeight.ExtraBold, 
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp,
                                color = if (user?.isPremium == true) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
@Composable
fun PremiumVideoContainer(
    uiState: MatchUiState,
    isVideoReady: Boolean,
    onRetry: () -> Unit,
    initRemoteVideo: (SurfaceViewRenderer) -> Unit,
    releaseRemoteVideo: (SurfaceViewRenderer) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(32.dp),
        color = Color.Black,
        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        shadowElevation = 8.dp
    ) {
        Box(Modifier.fillMaxSize()) {
            // ===== REMOTE VIDEO RENDERER — ALWAYS ALIVE =====
            // Never destroyed/recreated between states. Eliminates EGL lifecycle corruption.
            AndroidView(
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).apply {
                        initRemoteVideo(this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // ===== STATE OVERLAYS — only the overlay changes, not the renderer =====
            when (uiState) {
                is MatchUiState.Found -> {
                    // No overlay — keep video clear as requested
                }
                is MatchUiState.Searching -> {
                    // Dark overlay covers the renderer during search
                    Box(Modifier.fillMaxSize().background(Color.Black)) {
                        SearchingAnimation()
                    }
                }
                is MatchUiState.Error -> {
                    Box(Modifier.fillMaxSize().background(Color.Black)) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFF06262), modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(uiState.message, color = Color.LightGray)
                            Spacer(Modifier.height(24.dp))
                            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                        }
                    }
                }
                else -> {
                    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.minichat_logo_text), color = Color.White.copy(alpha = 0.1f), fontWeight = FontWeight.Black, fontSize = 32.sp)
                    }
                }
            }
            
            // Removed partner watermark for clear view
        }
    }
}

@Composable
fun VideoPlaceholderOverlay(partnerName: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .blur(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
           Surface(
               shape = CircleShape,
               modifier = Modifier.size(100.dp),
               color = Color.White.copy(alpha = 0.1f),
               border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.3f))
           ) {
               Box(contentAlignment = Alignment.Center) {
                   Text(
                       partnerName.take(1).uppercase(),
                       fontSize = 42.sp,
                       fontWeight = FontWeight.Black,
                       color = Color.White
                   )
               }
           }
           Spacer(Modifier.height(16.dp))
           Text(
                stringResource(R.string.connecting_to, partnerName),
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SearchingAnimation() {
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing)
        )
    )


    Box(Modifier.fillMaxSize().background(Color(0xFF0A0A0B)), contentAlignment = Alignment.Center) {
        // Radar circle
        androidx.compose.foundation.Canvas(modifier = Modifier.size(200.dp)) {
            drawCircle(
                color = Color(0xFF6C63FF).copy(alpha = pulse * 0.15f),
                radius = (size.minDimension / 2f) * pulse
            )
            drawCircle(
                color = Color(0xFF6C63FF),
                radius = size.minDimension / 2f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
        }
        
        // Rotating scanner
        androidx.compose.foundation.Canvas(modifier = Modifier
            .size(190.dp)
            .graphicsLayer { rotationZ = rotation }) {
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(Color.Transparent, Color(0xFF6C63FF).copy(alpha = 0.5f))
                ),
                startAngle = 0f,
                sweepAngle = 90f,
                useCenter = true
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.searching_title),
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            Text(
                stringResource(R.string.searching_subtitle),
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun PremiumFilterBar(
    selectedGender: String,
    selectedCountry: com.chatora.app.data.Country,
    isPremium: Boolean,
    onGenderToggle: () -> Unit,
    onCountryClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val genderLabel = when (selectedGender) {
            "Male" -> stringResource(R.string.gender_male)
            "Female" -> stringResource(R.string.gender_female)
            else -> stringResource(R.string.gender_all)
        }
        PremiumFilterChip(
            label = stringResource(R.string.gender_label, genderLabel),
            icon = Icons.Default.Face,
            onClick = onGenderToggle,
            isLocked = !isPremium && selectedGender == "All",
            isPremium = isPremium,
            modifier = Modifier.weight(1f)
        )
        PremiumFilterChip(
            label = "${selectedCountry.flag} ${stringResource(selectedCountry.nameRes)}",
            icon = Icons.Default.Language,
            onClick = onCountryClick,
            isLocked = !isPremium && selectedCountry.code != "AUTO" && selectedCountry.code != "US" && selectedCountry.code != "GB",
            isPremium = isPremium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun PremiumPromoBanner(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        color = Color(0xFF6C63FF),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Diamond, null, tint = Color.White)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(R.string.get_premium), color = Color.White, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.premium_desc), color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ArrowForwardIos, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun PremiumFilterChip(
    label: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    isLocked: Boolean = false, 
    isPremium: Boolean = false,
    onClick: () -> Unit, 
    modifier: Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f), // Glassmorphism
        border = androidx.compose.foundation.BorderStroke(
            width = 0.5.dp,
            brush = if (isPremium) Brush.linearGradient(listOf(Color(0xFFFFD700).copy(alpha = 0.6f), Color(0xFFFFA000).copy(alpha = 0.6f)))
                    else Brush.linearGradient(listOf(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)))
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon, null, 
                modifier = Modifier.size(14.dp), 
                tint = if (isPremium) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label, 
                fontWeight = if (isPremium) FontWeight.SemiBold else FontWeight.Medium, 
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (isPremium) {
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.Diamond, null, modifier = Modifier.size(10.dp), tint = Color(0xFFFFD700).copy(alpha = 0.8f))
            }
            if (isLocked) {
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.Lock, null, modifier = Modifier.size(10.dp), tint = Color.Gray.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun GenderSelectionDialog(onGenderSelected: (String) -> Unit) {
    var selectedGender by remember { mutableStateOf<String?>(null) }
    
    Dialog(onDismissRequest = { /* Mandatory */ }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Face, 
                    null, 
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.select_your_gender),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.gender_selection_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GenderOptionCard(
                        label = stringResource(R.string.male),
                        icon = Icons.Default.Male,
                        isSelected = selectedGender == "MALE",
                        onClick = { selectedGender = "MALE" },
                        modifier = Modifier.weight(1f)
                    )
                    GenderOptionCard(
                        label = stringResource(R.string.female),
                        icon = Icons.Default.Female,
                        isSelected = selectedGender == "FEMALE",
                        onClick = { selectedGender = "FEMALE" },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(Modifier.height(24.dp))
                
                Button(
                    onClick = { selectedGender?.let { onGenderSelected(it) } },
                    enabled = selectedGender != null,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.confirm_selection).uppercase(), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GenderOptionCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon, 
                null, 
                modifier = Modifier.size(32.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PremiumChatArea(
    messages: List<ChatMessage>,
    partnerIp: String = "",
    partnerCountryCode: String = "",
    onMediaClick: (String, String) -> Unit = { _, _ -> }
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    val showPartnerInfo = partnerIp.isNotEmpty() || partnerCountryCode.isNotEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(
                top = if (showPartnerInfo) 36.dp else 0.dp,
                bottom = 16.dp
            )
        ) {
            items(messages) { msg ->
                PremiumChatBubble(msg, onMediaClick = onMediaClick)
            }
        }

        // Fixed overlay at top — doesn't scroll with messages
        if (showPartnerInfo) {
            PartnerInfoBar(
                partnerIp = partnerIp,
                partnerCountryCode = partnerCountryCode,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
fun PartnerInfoBar(
    partnerIp: String,
    partnerCountryCode: String,
    modifier: Modifier = Modifier
) {
    // Lookup country flag from StaticData
    val country = if (partnerCountryCode.isNotEmpty()) {
        com.chatora.app.data.StaticData.getCountries().find { 
            it.code.equals(partnerCountryCode, ignoreCase = true) 
        }
    } else null
    
    // Fallback flag/name when country code is unknown or not found
    val flagText = country?.flag ?: "🌍"
    val countryName = country?.let { stringResource(it.nameRes) } ?: ""
    
    // Partially mask IP for privacy: show first and last octet, mask middle
    val maskedIp = if (partnerIp.isNotEmpty()) {
        val parts = partnerIp.split(".")
        if (parts.size == 4) {
            "${parts[0]}.***.***.${parts[3]}"
        } else partnerIp
    } else ""
    
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .graphicsLayer { alpha = 0.75f }
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = flagText,
            fontSize = 13.sp
        )
        if (countryName.isNotEmpty()) {
            Spacer(Modifier.width(5.dp))
            Text(
                text = countryName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
        if (maskedIp.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "•",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                fontSize = 9.sp
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = maskedIp,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontSize = 10.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}

@Composable
fun PremiumChatBubble(msg: ChatMessage, onMediaClick: (String, String) -> Unit = { _, _ -> }) {
    val isMe = msg.isFromMe
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
            shape = RoundedCornerShape(
                topStart = 20.dp, topEnd = 20.dp,
                bottomStart = if (isMe) 20.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 20.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column {
                if (msg.mediaUrl != null) {
                    MediaPreview(
                        url = msg.mediaUrl,
                        mediaType = msg.mediaType ?: "image/*",
                        onClick = { onMediaClick(msg.mediaUrl, msg.mediaType ?: "image/*") }
                    )
                }
                
                if (msg.text.isNotEmpty()) {
                    Text(
                        text = msg.text,
                        modifier = Modifier.padding(12.dp, 8.dp),
                        color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun MediaPreview(url: String, mediaType: String, onClick: () -> Unit) {
    val isVideo = mediaType.startsWith("video")
    
    Box(
        modifier = Modifier
            .padding(8.dp)
            .width(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.2f))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Image,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = if (isVideo) "Video Media" else "Photo Media",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "Tap to View",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

fun getFullUrl(url: String): String {
    if (url.startsWith("http")) return url
    val base = ApiConstants.BASE_URL.removeSuffix("/")
    val path = if (url.startsWith("/")) url else "/$url"
    return "$base$path"
}

fun downloadMedia(context: android.content.Context, url: String, isVideo: Boolean) {
    try {
        val fullUrl = getFullUrl(url)
        val request = DownloadManager.Request(Uri.parse(fullUrl))
            .setTitle("Chatora Download")
            .setDescription("Downloading media...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES,
                "Chatora_${System.currentTimeMillis()}${if (isVideo) ".mp4" else ".jpg"}"
            )
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        android.widget.Toast.makeText(context, "Download started...", android.widget.Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        android.util.Log.e("MINICHAT_DEBUG", "Download failed", e)
        android.widget.Toast.makeText(context, "Download failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun PhotoViewerDialog(url: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val fullUrl = getFullUrl(url)
    android.util.Log.d("MINICHAT_DEBUG", "PhotoViewerDialog: loading $fullUrl")
    
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.fillMaxSize().clickable { onDismiss() })
            var isLoading by remember { mutableStateOf(true) }
            var isError by remember { mutableStateOf(false) }

            AsyncImage(
                model = fullUrl,
                contentDescription = "Full Photo",
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit,
                onLoading = { isLoading = true; isError = false },
                onSuccess = { isLoading = false; isError = false },
                onError = { 
                    isLoading = false
                    isError = true
                    android.util.Log.e("MINICHAT_DEBUG", "PhotoViewerDialog: Error loading $fullUrl")
                }
            )

            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            if (isError) {
                Text("Failed to load image", color = Color.White)
            }

            // Download Button
            IconButton(
                onClick = { downloadMedia(context, url, false) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Download, "Download", tint = Color.White)
            }
        }
    }
}

@Composable
fun VideoPlayerDialog(url: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val fullUrl = getFullUrl(url)
    android.util.Log.d("MINICHAT_DEBUG", "VideoPlayerDialog: loading $fullUrl")
    
    val exoPlayer = remember(fullUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(fullUrl))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { 
            android.util.Log.d("MINICHAT_DEBUG", "Releasing ExoPlayer for $fullUrl")
            exoPlayer.release() 
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f/16f)
                .clip(RoundedCornerShape(24.dp)),
            color = Color.Black
        ) {
            Box(Modifier.fillMaxSize()) {
                AndroidView(
                    factory = {
                        PlayerView(context).apply {
                            player = exoPlayer
                            useController = true
                            setBackgroundColor(android.graphics.Color.BLACK)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Download Button
                IconButton(
                    onClick = { downloadMedia(context, url, true) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Download, "Download", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun PremiumControlSection(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit,
    onStop: () -> Unit,
    onNext: () -> Unit,
    onSwitchCamera: () -> Unit,
    initLocalVideo: (SurfaceViewRenderer) -> Unit,
    hasStarted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Local Camera Preview
        Box(
            modifier = Modifier
                .size(90.dp, 120.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black)
                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
        ) {
            AndroidView(
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).apply {
                        setMirror(true)
                        // Defer heavy camera init to next frame — prevents ANR
                        post { initLocalVideo(this) }
                    }
                },
                onRelease = { it.release() },
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = onSwitchCamera,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape).size(24.dp)
            ) {
                Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        
        Column(modifier = Modifier.weight(1f)) {
            // Message Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onAttachClick) {
                        Icon(Icons.Default.AttachFile, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    BasicTextField(
                        value = messageText,
                        onValueChange = onMessageChange,
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        decorationBox = { inner ->
                            if (messageText.isEmpty()) Text(stringResource(R.string.say_hello_hint), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), fontSize = 14.sp)
                            inner()
                        }
                    )
                    IconButton(onClick = onSend) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Large Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // STOP
                Surface(
                    onClick = onStop,
                    modifier = Modifier.weight(1f).height(60.dp),
                    color = Color(0xFFF06262),
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Stop, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
                
                // NEXT / START
                Surface(
                    onClick = onNext,
                    modifier = Modifier.weight(2f).height(60.dp),
                    color = if (hasStarted) Color(0xFF6C63FF) else Color(0xFF00C853), // Purple vs Green
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 8.dp
                ) {
                    AnimatedContent(
                        targetState = hasStarted,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.8f) togetherWith
                            fadeOut(animationSpec = tween(300))
                        },
                        label = "StartButtonAnim"
                    ) { started ->
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (started) {
                                Icon(Icons.AutoMirrored.Filled.NavigateNext, null, tint = Color.White, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.find_next_button), color = Color.White, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            } else {
                                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.start_button), color = Color.White, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun CountryList(
    countries: List<Country>, 
    isPremium: Boolean,
    onSelect: (Country) -> Unit
) {
    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            Text(
                text = stringResource(R.string.select_country),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        items(countries) { country ->
            val isAllowed = isPremium || country.code == "AUTO" || country.code == "US" || country.code == "GB"
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(country) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(country.flag, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(16.dp))
                Text(
                    text = stringResource(country.nameRes), 
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                if (!isAllowed) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            HorizontalDivider()
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
fun AdBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Box(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.ad_tag),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.unlock_no_ads),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
