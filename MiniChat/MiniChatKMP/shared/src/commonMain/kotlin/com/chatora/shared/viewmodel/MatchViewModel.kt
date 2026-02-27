package com.chatora.shared.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared ViewModel for match/queue state.
 * WebRTC signaling is platform-specific (JS WebRTC API vs iOS framework),
 * so this ViewModel only manages the queue/match state, not the media streams.
 */
class MatchViewModel : ViewModel() {

    private val _matchState = MutableStateFlow<MatchState>(MatchState.Idle)
    val matchState: StateFlow<MatchState> = _matchState.asStateFlow()

    fun startSearching() {
        _matchState.value = MatchState.Searching
    }

    fun onMatchFound(matchId: String, partnerId: String, partnerUsername: String, initiator: Boolean) {
        _matchState.value = MatchState.Matched(
            matchId = matchId,
            partnerId = partnerId,
            partnerUsername = partnerUsername,
            initiator = initiator
        )
    }

    fun onPartnerLeft() {
        _matchState.value = MatchState.PartnerLeft
    }

    fun onDisconnect() {
        _matchState.value = MatchState.Idle
    }

    fun findNext() {
        _matchState.value = MatchState.Searching
    }

    fun stop() {
        _matchState.value = MatchState.Idle
    }
}

sealed class MatchState {
    data object Idle : MatchState()
    data object Searching : MatchState()
    data class Matched(
        val matchId: String,
        val partnerId: String,
        val partnerUsername: String,
        val initiator: Boolean
    ) : MatchState()
    data object PartnerLeft : MatchState()
}
