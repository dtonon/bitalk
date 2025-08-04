package com.bitalk.android.service

import com.bitalk.android.model.NearbyUser

/**
 * Singleton to manage service instance and provide access to UI components
 */
object ServiceManager {
    private var bleService: BitalkBLEForegroundService? = null
    
    fun setBLEService(service: BitalkBLEForegroundService?) {
        bleService = service
    }
    
    fun getNearbyUsers(): List<NearbyUser> {
        return bleService?.getNearbyUsers() ?: emptyList()
    }
    
    fun isServiceRunning(): Boolean {
        return bleService != null
    }
    
    fun toggleScanning(): Boolean {
        return bleService?.let { service ->
            // Toggle the actual BLE scanning within the service
            service.toggleBLEScanning()
        } ?: false
    }
    
    fun isBLEScanning(): Boolean {
        return bleService?.isBLEScanning() ?: false
    }
}