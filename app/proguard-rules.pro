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