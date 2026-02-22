package com.iliyadev.minichat.core.exception

import org.springframework.http.HttpStatus

open class ApiException(
    val code: String,
    override val message: String,
    val status: HttpStatus = HttpStatus.BAD_REQUEST,
    val details: Any? = null
) : RuntimeException(message)

class EmailNotVerifiedException : ApiException(
    code = "EMAIL_NOT_VERIFIED",
    message = "Email is not verified. Please verify your email to continue.",
    status = HttpStatus.FORBIDDEN
)

class InvalidCredentialsException(message: String = "Invalid email or password") : ApiException(
    code = "INVALID_CREDENTIALS",
    message = message,
    status = HttpStatus.UNAUTHORIZED
)

class UserNotFoundException(message: String = "User not found") : ApiException(
    code = "USER_NOT_FOUND",
    message = message,
    status = HttpStatus.NOT_FOUND
)

class ConflictException(code: String, message: String) : ApiException(
    code = code,
    message = message,
    status = HttpStatus.CONFLICT
)

class BannedException(reason: String?) : ApiException(
    code = "USER_BANNED",
    message = "User is banned" + (if (reason != null) ": $reason" else ""),
    status = HttpStatus.FORBIDDEN
)
