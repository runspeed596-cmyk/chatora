# ===========================================
# MiniChat ProGuard / R8 Rules
# ===========================================
# Aggressive optimization passes
-optimizationpasses 5
-dontusemixedcaseclassnames
-verbose

# ===========================================
# Retrofit
# ===========================================
-keepattributes Signature
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation class retrofit2.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# ===========================================
# Gson / Serialization
# ===========================================
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.nextcode.minichat.data.** { *; }
-keep class com.nextcode.minichat.domain.** { *; }
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ===========================================
# WebRTC (Native)
# ===========================================
-keep class org.webrtc.** { *; }
-keep class com.nextcode.minichat.webrtc.** { *; }

# ===========================================
# Kotlin Coroutines
# ===========================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.android.AndroidExceptionPreHandler {
    <init>();
}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}

# ===========================================
# Room
# ===========================================
-keep class androidx.room.RoomDatabase
-keep class androidx.room.RoomDatabase$Builder
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}

# ===========================================
# Hilt / Dagger
# ===========================================
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# ===========================================
# Coil (Image Loading)
# ===========================================
-keep class coil.** { *; }
-dontwarn coil.**

# ===========================================
# Lottie
# ===========================================
-dontwarn com.airbnb.lottie.**
-keep class com.airbnb.lottie.** { *; }

# ===========================================
# Media3 / ExoPlayer
# ===========================================
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ===========================================
# CameraX
# ===========================================
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ===========================================
# EncryptedSharedPreferences / Security
# ===========================================
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# ===========================================
# OkHttp
# ===========================================
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ===========================================
# Google Sign-In / Credentials
# ===========================================
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.** { *; }
-dontwarn com.google.android.libraries.identity.**

# ===========================================
# Compose
# ===========================================
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# ===========================================
# General Security
# ===========================================
# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}