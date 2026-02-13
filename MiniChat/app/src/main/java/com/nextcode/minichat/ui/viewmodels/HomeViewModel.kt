package com.nextcode.minichat.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcode.minichat.data.Country
import com.nextcode.minichat.data.StaticData
import com.nextcode.minichat.data.User
import com.nextcode.minichat.domain.repositories.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import android.content.Context
import com.nextcode.minichat.webrtc.WebRtcClient
import org.webrtc.SurfaceViewRenderer

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val webRtcClient: WebRtcClient
) : ViewModel() {
    
    fun initLocalPreview(context: Context, view: SurfaceViewRenderer) {
        webRtcClient.startLocalVideo(view)
    }

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _selectedCountry = MutableStateFlow(StaticData.getCountries().first())
    val selectedCountry = _selectedCountry.asStateFlow()

    val countries = StaticData.getCountries()

    init {
        // Trigger initial refresh
        viewModelScope.launch {
            userRepository.refreshUser()
        }

        // Collect Flow from Real Repo
        viewModelScope.launch {
            userRepository.currentUser.collect { userEntity ->
                if (userEntity != null) {
                    _currentUser.value = User(
                        id = userEntity.id,
                        username = userEntity.username,
                        email = userEntity.email,
                        karma = userEntity.karma,
                        diamonds = userEntity.diamonds,
                        country = Country(userEntity.countryCode, userEntity.countryNameRes, userEntity.countryFlag),
                        isPremium = userEntity.isPremium
                    )
                }
            }
        }
    }
    
    fun updateCountry(country: Country) {
        _selectedCountry.value = country
    }
}
