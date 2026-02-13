package com.nextcode.minichat.domain.repositories

import com.nextcode.minichat.data.local.MatchEntity
import com.nextcode.minichat.data.remote.WebSocketEvent
import kotlinx.coroutines.flow.Flow

interface MatchRepository {
    val matchEvents: Flow<WebSocketEvent>
    val matchHistory: Flow<List<MatchEntity>>
    suspend fun saveMatch(eventId: String, partnerId: String, username: String, country: String)
    fun connectAndSubscribe(token: String)
    fun findMatch(myCountry: String, targetCountry: String, targetGender: String)
    fun stopMatching(myCountry: String, targetGender: String)
    fun disconnect()
    fun sendMessage(matchId: String, message: String, mediaUrl: String? = null, mediaType: String? = null)
    fun subscribe(topic: String): String
    fun unsubscribe(id: String)
}
