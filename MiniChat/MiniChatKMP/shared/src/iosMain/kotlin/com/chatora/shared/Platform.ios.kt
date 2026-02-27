package com.chatora.shared

import platform.UIKit.UIDevice

actual class Platform actual constructor() {
    actual val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    actual val isWeb: Boolean = false
    actual val isIOS: Boolean = true
}
