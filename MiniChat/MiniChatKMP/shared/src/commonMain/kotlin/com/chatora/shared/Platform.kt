package com.chatora.shared

/**
 * Platform-specific information.
 * Each target (js, ios) provides its own implementation.
 */
expect class Platform() {
    val name: String
    val isWeb: Boolean
    val isIOS: Boolean
}
