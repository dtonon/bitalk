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
     * Convert to compact binary format for BLE transmission
     */
    fun toByteArray(): ByteArray {
        return try {
            // Create compact format: [username_len][username][desc_len][desc][topic_count][topic1_len][topic1]...
            val usernameBytes = username.toByteArray(Charsets.UTF_8)
            val descBytes = description.toByteArray(Charsets.UTF_8)
            val topicBytes = topics.map { it.toByteArray(Charsets.UTF_8) }
            
            // Calculate total size
            var totalSize = 1 + usernameBytes.size + 1 + descBytes.size + 1 // username_len + username + desc_len + desc + topic_count
            topicBytes.forEach { totalSize += 1 + it.size } // topic_len + topic for each topic
            
            if (totalSize > MAX_PACKET_SIZE - 4) { // Leave 4 bytes for length prefix
                android.util.Log.w("TopicBroadcast", "Data too large ($totalSize bytes), truncating...")
                // Fall back to minimal format with just username and first few topics
                return createMinimalFormat()
            }
            
            val buffer = ByteArray(4 + totalSize) // 4 bytes for length prefix
            var pos = 0
            
            // Write total length (4 bytes)
            buffer[pos++] = (totalSize shr 24).toByte()
            buffer[pos++] = (totalSize shr 16).toByte()
            buffer[pos++] = (totalSize shr 8).toByte()
            buffer[pos++] = totalSize.toByte()
            
            // Write username
            buffer[pos++] = usernameBytes.size.toByte()
            usernameBytes.copyInto(buffer, pos)
            pos += usernameBytes.size
            
            // Write description
            buffer[pos++] = descBytes.size.toByte()
            descBytes.copyInto(buffer, pos)
            pos += descBytes.size
            
            // Write topics
            buffer[pos++] = topics.size.toByte()
            topicBytes.forEach { topicData ->
                buffer[pos++] = topicData.size.toByte()
                topicData.copyInto(buffer, pos)
                pos += topicData.size
            }
            
            android.util.Log.d("TopicBroadcast", "Serialized broadcast: username=$username, topics=$topics (${buffer.size} bytes)")
            buffer
        } catch (e: Exception) {
            android.util.Log.e("TopicBroadcast", "Failed to serialize broadcast", e)
            createMinimalFormat()
        }
    }
    
    /**
     * Create minimal format when data is too large
     */
    private fun createMinimalFormat(): ByteArray {
        try {
            val shortUsername = username.take(20)
            val shortTopics = topics.take(3).map { it.take(15) }
            val usernameBytes = shortUsername.toByteArray(Charsets.UTF_8)
            val topicBytes = shortTopics.map { it.toByteArray(Charsets.UTF_8) }
            
            var totalSize = 1 + usernameBytes.size + 1 + 0 + 1 // username + empty desc + topic count
            topicBytes.forEach { totalSize += 1 + it.size }
            
            val buffer = ByteArray(4 + totalSize)
            var pos = 0
            
            // Write length
            buffer[pos++] = (totalSize shr 24).toByte()
            buffer[pos++] = (totalSize shr 16).toByte()
            buffer[pos++] = (totalSize shr 8).toByte()
            buffer[pos++] = totalSize.toByte()
            
            // Write username
            buffer[pos++] = usernameBytes.size.toByte()
            usernameBytes.copyInto(buffer, pos)
            pos += usernameBytes.size
            
            // Empty description
            buffer[pos++] = 0
            
            // Write topics
            buffer[pos++] = shortTopics.size.toByte()
            topicBytes.forEach { topicData ->
                buffer[pos++] = topicData.size.toByte()
                topicData.copyInto(buffer, pos)
                pos += topicData.size
            }
            
            android.util.Log.d("TopicBroadcast", "Created minimal format: ${buffer.size} bytes")
            return buffer
        } catch (e: Exception) {
            android.util.Log.e("TopicBroadcast", "Failed to create minimal format", e)
            return ByteArray(0)
        }
    }
    
    companion object {
        /**
         * Parse from binary format received via BLE
         */
        fun fromByteArray(data: ByteArray): TopicBroadcast? {
            return try {
                if (data.size < 5) {
                    android.util.Log.w("TopicBroadcast", "Data too small: ${data.size} bytes")
                    return null
                }
                
                var pos = 0
                
                // Read total length (4 bytes)
                val totalLength = ((data[pos++].toInt() and 0xFF) shl 24) or
                                 ((data[pos++].toInt() and 0xFF) shl 16) or
                                 ((data[pos++].toInt() and 0xFF) shl 8) or
                                 (data[pos++].toInt() and 0xFF)
                
                android.util.Log.d("TopicBroadcast", "Deserializing broadcast: ${data.size} bytes, expected length: $totalLength")
                
                if (pos + totalLength > data.size) {
                    android.util.Log.w("TopicBroadcast", "Incomplete data: expected ${pos + totalLength}, got ${data.size}")
                    return null
                }
                
                // Read username
                val usernameLen = data[pos++].toInt() and 0xFF
                if (pos + usernameLen > data.size) {
                    android.util.Log.w("TopicBroadcast", "Invalid username length: $usernameLen")
                    return null
                }
                val username = String(data, pos, usernameLen, Charsets.UTF_8)
                pos += usernameLen
                
                // Read description
                val descLen = data[pos++].toInt() and 0xFF
                if (pos + descLen > data.size) {
                    android.util.Log.w("TopicBroadcast", "Invalid description length: $descLen")
                    return null
                }
                val description = if (descLen > 0) String(data, pos, descLen, Charsets.UTF_8) else ""
                pos += descLen
                
                // Read topics
                val topicCount = data[pos++].toInt() and 0xFF
                val topics = mutableListOf<String>()
                
                for (i in 0 until topicCount) {
                    if (pos >= data.size) break
                    
                    val topicLen = data[pos++].toInt() and 0xFF
                    if (pos + topicLen > data.size) {
                        android.util.Log.w("TopicBroadcast", "Invalid topic length: $topicLen at position $pos")
                        break
                    }
                    
                    val topic = String(data, pos, topicLen, Charsets.UTF_8)
                    topics.add(topic)
                    pos += topicLen
                }
                
                val broadcast = TopicBroadcast(username, description, topics)
                android.util.Log.d("TopicBroadcast", "Deserialized broadcast: username=$username, topics=$topics")
                broadcast
                
            } catch (e: Exception) {
                android.util.Log.e("TopicBroadcast", "Failed to deserialize broadcast from ${data.size} bytes", e)
                null
            }
        }
        
        /**
         * Maximum BLE packet size - keep broadcasts small
         */
        const val MAX_PACKET_SIZE = 512
    }
}