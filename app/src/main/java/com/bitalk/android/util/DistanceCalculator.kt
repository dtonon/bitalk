package com.bitalk.android.util

import kotlin.math.pow

/**
 * Converts RSSI values to estimated distance in meters
 * Based on the free-space path loss model with empirical adjustments
 */
object DistanceCalculator {
    
    // Calibration constants for BLE distance estimation
    private const val TX_POWER = -59.0 // Measured power at 1 meter (dBm)
    private const val PATH_LOSS_EXPONENT = 2.0 // Free space = 2.0, indoor = 2.7-4.3
    
    /**
     * Convert RSSI to estimated distance in meters
     * 
     * @param rssi Received Signal Strength Indicator (negative value)
     * @param txPower Transmission power at 1 meter reference distance
     * @return Estimated distance in meters
     */
    fun rssiToDistance(
        rssi: Int, 
        txPower: Double = TX_POWER
    ): Double {
        if (rssi == 0) return -1.0 // Cannot determine distance
        
        val ratio = rssi * 1.0 / txPower
        return if (ratio < 1.0) {
            ratio.pow(10.0)
        } else {
            val accuracy = (0.89976) * ratio.pow(7.7095) + 0.111
            accuracy
        }
    }
    
    /**
     * More accurate distance calculation with environmental factors
     * 
     * @param rssi Received Signal Strength Indicator
     * @param txPower Transmission power at reference distance
     * @param pathLossExponent Environmental path loss (2.0-4.3)
     * @return Estimated distance in meters
     */
    fun rssiToDistanceAdvanced(
        rssi: Int,
        txPower: Double = TX_POWER,
        pathLossExponent: Double = PATH_LOSS_EXPONENT
    ): Double {
        if (rssi == 0) return -1.0
        
        return 10.0.pow((txPower - rssi) / (10.0 * pathLossExponent))
    }
    
    /**
     * Get bubble size multiplier based on distance
     * Closer users get bigger bubbles (inverse relationship)
     * 
     * @param distance Distance in meters
     * @return Size multiplier (0.3 to 1.0)
     */
    fun getBubbleSizeMultiplier(distance: Double): Float {
        return when {
            distance < 0 -> 0.5f // Unknown distance
            distance < 1.0 -> 1.0f // Very close - largest bubble
            distance < 3.0 -> 0.8f // Close - large bubble  
            distance < 6.0 -> 0.6f // Medium distance - medium bubble
            distance < 10.0 -> 0.4f // Far - small bubble
            else -> 0.3f // Very far - smallest bubble
        }
    }
    
    /**
     * Smooth RSSI values to reduce noise and provide stable distance readings
     */
    class RSSIFilter(private val windowSize: Int = 5) {
        private val readings = mutableListOf<Int>()
        
        fun addReading(rssi: Int): Int {
            readings.add(rssi)
            if (readings.size > windowSize) {
                readings.removeAt(0)
            }
            return readings.average().toInt()
        }
        
        fun getSmoothedDistance(rssi: Int): Double {
            val smoothedRssi = addReading(rssi)
            return rssiToDistanceAdvanced(smoothedRssi)
        }
    }
}