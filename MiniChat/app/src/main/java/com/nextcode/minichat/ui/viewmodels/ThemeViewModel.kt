package com.nextcode.minichat.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.nextcode.minichat.data.ThemeMode
import com.nextcode.minichat.data.local.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _themeMode = MutableStateFlow(tokenManager.getThemeMode())
    val themeMode = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        tokenManager.saveThemeMode(mode)
        _themeMode.value = mode
    }
}
