package com.iliyadev.minichat.core.security

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Collections

@Service
class GoogleAuthService(
    @Value("\${google.client.id}") private val googleClientId: String
) {
    private val transport = NetHttpTransport()
    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val verifier = GoogleIdTokenVerifier.Builder(transport, jsonFactory)
        .setAudience(Collections.singletonList(googleClientId))
        .build()

    fun verifyToken(idTokenString: String): GoogleIdToken? {
        return try {
            val token = verifier.verify(idTokenString)
            if (token == null) {
                println("Google token verification failed: Token is null for audience: $googleClientId")
            }
            token
        } catch (e: Exception) {
            println("Google token verification error: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}
