plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.chatora.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.chatora.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            buildConfigField("String", "API_BASE_URL", "\"http://172.86.95.177/\"")
            buildConfigField("String", "WS_URL", "\"ws://172.86.95.177/ws-native\"")
            buildConfigField("Boolean", "ENABLE_LOGGING", "true")
            // TURN server config — debug can use free servers for local testing
            buildConfigField("String", "TURN_URL", "\"turn:openrelay.metered.ca:443\"")
            buildConfigField("String", "TURN_USERNAME", "\"openrelayproject\"")
            buildConfigField("String", "TURN_PASSWORD", "\"openrelayproject\"")
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
            // CRITICAL FIX: Production MUST use HTTPS and WSS
            buildConfigField("String", "API_BASE_URL", "\"https://172.86.95.177/\"")
            buildConfigField("String", "WS_URL", "\"wss://172.86.95.177/ws-native\"")
            buildConfigField("Boolean", "ENABLE_LOGGING", "false")
            // TURN server config — replace with your own TURN credentials for production
            buildConfigField("String", "TURN_URL", "\"turn:openrelay.metered.ca:443\"")
            buildConfigField("String", "TURN_USERNAME", "\"openrelayproject\"")
            buildConfigField("String", "TURN_PASSWORD", "\"openrelayproject\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.0")

    ksp(libs.hilt.compiler)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)
    implementation(libs.work.runtime.ktx)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Polish & Perf
    implementation(libs.lottie.compose)
    debugImplementation(libs.leakcanary.android)

    // Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // WebRTC
    implementation(libs.webrtc)

    // Coil (Images)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Lottie (Animations)
    implementation("com.airbnb.android:lottie-compose:6.4.0")

    // Security — Encrypted Storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // CameraX
    val cameraxVersion = "1.3.4"
    implementation("androidx.camera:camera-core:${cameraxVersion}")
    implementation("androidx.camera:camera-camera2:${cameraxVersion}")
    implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
    implementation("androidx.camera:camera-view:${cameraxVersion}")

    // Extended Icons
    implementation("androidx.compose.material:material-icons-extended:1.6.8")

    // Credential Manager (Google Sign-In)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Media3 (Video Player)
    val media3Version = "1.3.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
}