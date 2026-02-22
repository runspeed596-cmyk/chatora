package com.iliyadev.springboot.utils

import com.iliyadev.springboot.config.JwtTokenUtils.JwtTokenUtils
import com.iliyadev.springboot.utils.exceptions.JwtTokenException
import jakarta.servlet.http.HttpServletRequest
import java.util.Locale
import kotlin.text.substring
import kotlin.text.trim

class UserUtil {
    companion object {
        fun getCurrentUsername(jwtUtil: JwtTokenUtils,request: HttpServletRequest): String {
            val header = request.getHeader("Authorization")
            if (header == null || !header.trim().lowercase(Locale.getDefault()).startsWith("bearer ")) {
                throw JwtTokenException("please set bearer token")
            }
            val token = header.substring(7).trim()
            return jwtUtil.getUsernameFromToken(token)
        }
    }
}