package com.bitalk.android.notification

import com.bitalk.android.model.NearbyUser

/**
 * Singleton to handle notification click data
 */
object NotificationClickHandler {
    private var pendingUserData: UserFromNotification? = null
    private var onUserDataAvailable: ((UserFromNotification) -> Unit)? = null
    
    data class UserFromNotification(
        val username: String,
        val description: String,
        val topics: List<String>,
        val matchingTopics: List<String>
    )
    
    /**
     * Set user data from notification intent
     */
    fun setUserData(
        username: String,
        description: String,
        topics: Array<String>,
        matchingTopics: Array<String>
    ) {
        val userData = UserFromNotification(
            username = username,
            description = description,
            topics = topics.toList(),
            matchingTopics = matchingTopics.toList()
        )
        
        pendingUserData = userData
        
        // If someone is listening, notify them immediately
        onUserDataAvailable?.invoke(userData)
        
        // Clear the listener to prevent multiple calls
        onUserDataAvailable = null
    }
    
    /**
     * Get pending user data (one-time use)
     */
    fun getPendingUserData(): UserFromNotification? {
        val data = pendingUserData
        pendingUserData = null // Clear after reading
        return data
    }
    
    /**
     * Set listener for when user data becomes available
     */
    fun setOnUserDataAvailable(listener: (UserFromNotification) -> Unit) {
        onUserDataAvailable = listener
        
        // If data is already available, call immediately
        pendingUserData?.let { userData ->
            listener(userData)
            pendingUserData = null
        }
    }
    
    /**
     * Clear any pending data and listeners
     */
    fun clear() {
        pendingUserData = null
        onUserDataAvailable = null
    }
}