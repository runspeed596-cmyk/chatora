package com.nextcode.minichat.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nextcode.minichat.navigation.Screen
import androidx.compose.ui.res.stringResource
import com.nextcode.minichat.R
import com.nextcode.minichat.ui.components.PrimaryButton
import com.nextcode.minichat.ui.viewmodels.OnboardingViewModel
import kotlinx.coroutines.launch

data class OnboardingPage(val title: Int, val desc: Int, val color: Color)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    navController: NavController,
    viewModel: OnboardingViewModel = hiltViewModel<OnboardingViewModel>()
) {
    val pages = listOf(
        OnboardingPage(R.string.onboarding_title_1, R.string.onboarding_desc_1, MaterialTheme.colorScheme.primary),
        OnboardingPage(R.string.onboarding_title_2, R.string.onboarding_desc_2, MaterialTheme.colorScheme.secondary),
        OnboardingPage(R.string.onboarding_title_3, R.string.onboarding_desc_3, MaterialTheme.colorScheme.tertiary)
    )
    
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page: Int ->
            OnboardingPageContent(pages[page])
        }

        Row(
            Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else Color.LightGray
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .size(10.dp)
                        .background(color, CircleShape)
                )
            }
        }

        Box(modifier = Modifier.padding(24.dp)) {
            PrimaryButton(
                text = if (pagerState.currentPage == 2) stringResource(R.string.get_started) else stringResource(R.string.next),
                onClick = {
                    if (pagerState.currentPage < 2) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        viewModel.completeOnboarding()
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // More polished placeholder for Illustration
        Surface(
            modifier = Modifier
                .size(240.dp)
                .padding(bottom = 32.dp),
            shape = CircleShape,
            color = page.color.copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(2.dp, page.color)
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Here you would put a Lottie animation or a nice Image
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(page.color, CircleShape)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = stringResource(page.title),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.animateContentSize()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(page.desc),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
