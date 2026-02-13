package com.chatora.app.data.repositories

import com.chatora.app.data.local.MatchDao
import com.chatora.app.data.local.MatchEntity
import com.chatora.app.data.remote.WebSocketEvent
import com.chatora.app.data.remote.WebSocketManager
import kotlinx.coroutines.flow.Flow
import com.chatora.app.domain.repositories.MatchRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatchRepositoryImpl @Inject constructor(
    private val socketManager: WebSocketManager,
    private val matchDao: MatchDao
) : MatchRepository {
    override val matchEvents: Flow<WebSocketEvent> = socketManager.events
    override val matchHistory: Flow<List<MatchEntity>> = matchDao.getAllMatches()

    override suspend fun saveMatch(eventId: String, partnerId: String, username: String, country: String) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val match = MatchEntity(
                id = eventId,
                partnerId = partnerId,
                partnerUsername = username,
                partnerCountry = country
            )
            matchDao.insertMatch(match)
        }
    }

    override fun connectAndSubscribe(token: String) {
        socketManager.connect(token)
    }

    override fun findMatch(myCountry: String, targetCountry: String, targetGender: String) {
        socketManager.sendMatchRequest(myCountry, targetCountry, targetGender)
    }
    
    override fun stopMatching(myCountry: String, targetGender: String) {
        socketManager.sendLeaveRequest(myCountry, targetGender)
        //socketManager.disconnect() // DO NOT DISCONNECT! Keep alive for next Play
    }

    override fun disconnect() {
        // Just explicit disconnect without STOMP leave frame (fallback)
        socketManager.disconnect()
    }

    override fun sendMessage(matchId: String, message: String, mediaUrl: String?, mediaType: String?) {
        socketManager.sendMessage(matchId, message, mediaUrl, mediaType)
    }

    override fun subscribe(topic: String): String {
        return socketManager.subscribe(topic)
    }

    override fun unsubscribe(id: String) {
        socketManager.unsubscribe(id)
    }
}
