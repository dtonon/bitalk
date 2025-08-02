package com.bitalk.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents a discovered nearby user with distance and matching info
 */
@Parcelize
data class NearbyUser(
    val username: String,
    val description: String,
    val topics: List<String>,
    val rssi: Int,
    val estimatedDistance: Double,  // meters
    val matchingTopics: List<String>,
    val firstSeen: Long,
    val lastSeen: Long,
    val deviceAddress: String? = null
) : Parcelable {
    
    /**
     * Calculate match score (0.0 to 1.0) based on common topics
     */
    val matchScore: Float
        get() = if (topics.isEmpty()) 0f else matchingTopics.size.toFloat() / topics.size.toFloat()
    
    /**
     * Check if user is considered "active" (seen recently)
     */
    fun isActive(timeoutMs: Long = 30_000): Boolean {
        return System.currentTimeMillis() - lastSeen < timeoutMs
    }
    
    /**
     * Get time since first detection in milliseconds
     */
    val timeSinceFirstSeen: Long
        get() = System.currentTimeMillis() - firstSeen
        
    /**
     * Get formatted distance string
     */
    val formattedDistance: String
        get() = when {
            estimatedDistance < 1.0 -> "<1m"
            estimatedDistance < 10.0 -> "~${estimatedDistance.toInt()}m"
            else -> "~${(estimatedDistance / 10).toInt() * 10}m+"
        }
}