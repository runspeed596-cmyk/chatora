package com.chatora.shared

actual class Platform actual constructor() {
    actual val name: String = "Web (Wasm)"
    actual val isWeb: Boolean = true
    actual val isIOS: Boolean = false
}
