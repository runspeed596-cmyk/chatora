# ===========================================
# Chatora ProGuard / R8 Rules
# ===========================================
-optimizationpasses 5
-dontusemixedcaseclassnames
-verbose

# ===========================================
# GLOBAL: Keep generic type info everywhere
# ===========================================
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# ===========================================
# Retrofit (CRITICAL for R8 full mode)
# ===========================================
# Retrofit creates proxies for interfaces — R8 sees no implementations
# and strips method signatures. These rules prevent that.
-keep,allowobfuscation class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class retrofit2.Call

# R8 full mode: keep Retrofit interface method signatures
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation,allowshrinking interface <1>

# Keep the ApiService interface entirely (our Retrofit API)
-keep interface com.chatora.app.data.remote.ApiService { *; }

# ===========================================
# Gson / Serialization
# ===========================================
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep ALL data/model classes (needed for Gson deserialization)
-keep class com.chatora.app.data.** { *; }
-keep class com.chatora.app.domain.** { *; }

# ===========================================
# WebRTC (Native)
# ===========================================
-keep class org.webrtc.** { *; }
-keep class com.chatora.app.webrtc.** { *; }

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
# Google Sign-In / Credentials / Identity
# ===========================================
-keep class androidx.credentials.** { *; }
-keep class androidx.credentials.internal.** { *; }
-keep class androidx.credentials.provider.** { *; }
-keep class com.google.android.libraries.identity.** { *; }
-dontwarn com.google.android.libraries.identity.**
-keep class com.google.android.libraries.credential.** { *; }
-dontwarn com.google.android.libraries.credential.**
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.tasks.** { *; }

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