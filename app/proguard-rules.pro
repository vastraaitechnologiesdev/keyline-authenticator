# =============================================================================
# Keyline Authenticator - Production ProGuard / R8 Optimization & Keep Rules
# =============================================================================

# Preserve line numbers and source file attributes for actionable production stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve runtime annotations, generic signatures, inner classes, enclosing methods, and exceptions
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions

# -----------------------------------------------------------------------------
# 1. Application Data Models & Enum Deserialization
# -----------------------------------------------------------------------------
# Preserve OtpAccount and related enums/models for JSON serialization/deserialization
-keep class com.example.model.** { *; }
-keepclassmembers class com.example.model.** { *; }
-keep enum com.example.model.** { *; }
-keepclassmembers enum com.example.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Preserve all enum values and valueOf methods across the app to prevent deserialization crashes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Preserve ViewModel and Helper data classes
-keep class com.example.viewmodel.AccountUiItem { *; }
-keepclassmembers class com.example.viewmodel.AccountUiItem { *; }
-keep class com.example.viewmodel.AuthenticatorViewModel$UndoDeleteState { *; }
-keepclassmembers class com.example.viewmodel.AuthenticatorViewModel$UndoDeleteState { *; }
-keep class com.example.crypto.SecurityCheckResult { *; }
-keepclassmembers class com.example.crypto.SecurityCheckResult { *; }

# Keep all annotated classes with @Keep
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep <fields>;
    @androidx.annotation.Keep <methods>;
}

# -----------------------------------------------------------------------------
# 2. JSON Serialization Frameworks (Kotlinx Serialization / Gson / Moshi / org.json)
# -----------------------------------------------------------------------------
# Standard org.json
-keep class org.json.** { *; }
-dontwarn org.json.**

# Kotlinx Serialization
-dontwarn kotlinx.serialization.**
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Google Gson
-dontwarn com.google.gson.**
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
}

# Square Moshi
-dontwarn com.squareup.moshi.**
-keep class com.squareup.moshi.** { *; }
-keepclasseswithmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}
-keep class * extends com.squareup.moshi.JsonAdapter { *; }
-keep class *JsonAdapter { *; }

# -----------------------------------------------------------------------------
# 3. AndroidX Security Crypto & Google Tink (Keystore & Encryption)
# -----------------------------------------------------------------------------
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# Google Tink crypto engine
-keep class com.google.crypto.tink.** { *; }
-keepclassmembers class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# Tink shaded Protobuf classes (prevents NoClassDefFoundError / Keyset deserialization crashes)
-keep class com.google.crypto.tink.shaded.protobuf.** { *; }
-dontwarn com.google.crypto.tink.shaded.protobuf.**
-keepclassmembers class * extends com.google.crypto.tink.shaded.protobuf.GeneratedMessageLite { *; }
-keepclassmembers class * extends com.google.crypto.tink.Key { *; }

# Java Cryptography Architecture (JCA) & Android KeyStore
-keep class java.security.** { *; }
-keep interface java.security.** { *; }
-keep class javax.crypto.** { *; }
-keep interface javax.crypto.** { *; }
-keep class android.security.** { *; }
-keep class android.security.keystore.** { *; }
-dontwarn java.security.**
-dontwarn javax.crypto.**
-dontwarn android.security.**
-dontwarn android.security.keystore.**

# -----------------------------------------------------------------------------
# 4. AndroidX Biometric Prompt
# -----------------------------------------------------------------------------
-keep class androidx.biometric.** { *; }
-keepclassmembers class androidx.biometric.** { *; }
-keepclassmembers class androidx.biometric.BiometricPrompt$* { *; }
-dontwarn androidx.biometric.**

# -----------------------------------------------------------------------------
# 5. CameraX & Hardware APIs
# -----------------------------------------------------------------------------
-keep class androidx.camera.core.** { *; }
-keep class androidx.camera.camera2.** { *; }
-keep class androidx.camera.lifecycle.** { *; }
-keep class androidx.camera.view.** { *; }
-keep class androidx.camera.core.impl.** { *; }
-dontwarn androidx.camera.**
-keep class * implements androidx.camera.core.CameraXConfig$Provider { *; }
-keep class * extends androidx.camera.core.impl.MetadataHolderService { *; }

# -----------------------------------------------------------------------------
# 6. ZXing Barcode Scanning & ML Kit
# -----------------------------------------------------------------------------
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**
-keep class com.journeyapps.barcodescanner.** { *; }
-keepclassmembers class com.journeyapps.barcodescanner.** { *; }
-dontwarn com.journeyapps.barcodescanner.**
-keep class com.journeyapps.barcodescanner.camera.** { *; }
-keep class com.journeyapps.barcodescanner.CaptureActivity { *; }

# Google ML Kit Barcode Scanning Keep Rules
-keep class com.google.mlkit.** { *; }
-keep interface com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Keep Google Play services and component injection dependencies
-keep class com.google.android.gms.** { *; }
-keep interface com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

-keep class com.google.firebase.components.** { *; }
-keep interface com.google.firebase.components.** { *; }

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# -----------------------------------------------------------------------------
# 7. Jetpack Compose
# -----------------------------------------------------------------------------
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.animation.** { *; }
-dontwarn androidx.compose.**
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
    @androidx.compose.runtime.ReadOnlyComposable *;
}

# -----------------------------------------------------------------------------
# 8. Kotlin Coroutines & Flow
# -----------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}
-keep class kotlinx.coroutines.flow.** { *; }
-dontwarn kotlinx.coroutines.**

# -----------------------------------------------------------------------------
# 9. Room & SQLCipher (if present)
# -----------------------------------------------------------------------------
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class * {
    @androidx.room.Dao *;
}
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-dontwarn net.sqlcipher.**
