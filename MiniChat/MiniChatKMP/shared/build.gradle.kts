plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    // ── Web Target (Wasm) ────────────────────────────────────────────
    wasmJs {
        browser()
        binaries.executable()
    }

    // ── iOS Targets ──────────────────────────────────────────────────
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        // ── Common ───────────────────────────────────────────────────
        commonMain.dependencies {
            // Compose Multiplatform
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)

            // Navigation
            implementation(libs.navigation.compose)

            // Lifecycle
            implementation(libs.lifecycle.viewmodel.compose)

            // Networking (Ktor)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.websockets)

            // Serialization
            implementation(libs.kotlinx.serialization.json)

            // Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // DI (Koin)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        // ── Wasm/Web ─────────────────────────────────────────────────
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }

        // ── iOS ──────────────────────────────────────────────────────
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
