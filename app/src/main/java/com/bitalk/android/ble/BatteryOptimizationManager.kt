package com.bitalk.android.ble

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * Manages battery optimization settings for Bitalk
 * Helps ensure reliable background BLE scanning
 */
class BatteryOptimizationManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BatteryOptimizationManager"
    }
    
    /**
     * Check if the app is ignoring battery optimizations
     */
    fun isBatteryOptimizationIgnored(): Boolean {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val isIgnored = powerManager.isIgnoringBatteryOptimizations(context.packageName)
            Log.d(TAG, "Battery optimization ignored: $isIgnored")
            isIgnored
        } catch (e: Exception) {
            Log.e(TAG, "Error checking battery optimization status: ${e.message}")
            false
        }
    }
    
    /**
     * Request battery optimization exemption
     * Opens system settings for the user to grant exemption
     */
    fun requestBatteryOptimizationExemption(activity: Activity) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            activity.startActivity(intent)
            Log.d(TAG, "Launched battery optimization exemption request")
        } catch (e: Exception) {
            Log.e(TAG, "Error launching battery optimization request: ${e.message}")
            // Fallback to general battery optimization settings
            try {
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                activity.startActivity(fallbackIntent)
                Log.d(TAG, "Launched fallback battery optimization settings")
            } catch (e2: Exception) {
                Log.e(TAG, "Error launching fallback battery settings: ${e2.message}")
            }
        }
    }
}