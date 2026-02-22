package com.chatora.app.data.remote

import android.content.Context
import com.chatora.app.R

object ErrorMapper {
    fun mapCodeToMessage(context: Context, code: String?): String {
        val resId = when (code) {
            "INVALID_CREDENTIALS" -> R.string.error_invalid_credentials
            "EMAIL_NOT_VERIFIED" -> R.string.error_email_not_verified
            "USER_NOT_FOUND" -> R.string.error_user_not_found
            "EMAIL_ALREADY_EXISTS" -> R.string.error_email_already_exists
            "USERNAME_ALREADY_EXISTS" -> R.string.error_username_already_exists
            "USER_BANNED" -> R.string.error_user_banned
            "INVALID_CODE" -> R.string.error_invalid_code
            "ALREADY_VERIFIED" -> R.string.error_already_verified
            "BAD_REQUEST" -> R.string.error_bad_request
            "INTERNAL_ERROR" -> R.string.error_internal
            else -> null
        }
        
        return if (resId != null) {
            context.getString(resId)
        } else {
            context.getString(R.string.error_network)
        }
    }
}
