# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Gson classes
-keep class com.google.gson.** { *; }
-keep class com.bitalk.android.model.** { *; }

# Keep BLE related classes
-keep class no.nordicsemi.android.ble.** { *; }

# Keep security crypto
-keep class androidx.security.crypto.** { *; }

# Keep Google Tink crypto classes
-keep class com.google.crypto.tink.** { *; }
-dontwarn javax.annotation.**
-dontwarn javax.annotation.concurrent.**

# Keep JSR 305 annotations
-keep class javax.annotation.** { *; }
-keep class javax.annotation.concurrent.** { *; }

# Google Tink optional dependencies - suppress warnings for unused classes
-dontwarn com.google.api.client.**
-dontwarn org.joda.time.**
-dontwarn com.google.auto.service.**
-dontwarn org.conscrypt.**
-dontwarn com.google.errorprone.annotations.**