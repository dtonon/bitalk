package com.bitalk.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.google.gson.Gson

/**
 * Lightweight BLE packet for broadcasting user info
 * This is sent unencrypted for discoverability
 */
@Parcelize
data class TopicBroadcast(
    val username: String,
    val description: String,
    val topics: List<String>,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {
    
    /**
     * Convert to JSON bytes for BLE transmission
     */
    fun toByteArray(): ByteArray {
        return try {
            val gson = Gson()
            gson.toJson(this).toByteArray(Charsets.UTF_8)
        } catch (e: Exception) {
            ByteArray(0)
        }
    }
    
    companion object {
        /**
         * Parse from JSON bytes received via BLE
         */
        fun fromByteArray(data: ByteArray): TopicBroadcast? {
            return try {
                val gson = Gson()
                val json = String(data, Charsets.UTF_8)
                gson.fromJson(json, TopicBroadcast::class.java)
            } catch (e: Exception) {
                null
            }
        }
        
        /**
         * Maximum BLE packet size - keep broadcasts small
         */
        const val MAX_PACKET_SIZE = 512
    }
}