package com.bitalk.android.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bitalk.android.MainActivity
import com.bitalk.android.R
import com.bitalk.android.model.NearbyUser

/**
 * Service for managing app notifications
 */
class NotificationService(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "user_matches"
        private const val CHANNEL_NAME = "User Matches"
        private const val CHANNEL_DESCRIPTION = "Notifications for nearby users with matching topics"
        private const val NOTIFICATION_ID_BASE = 1000
        
        // Intent extras for notification handling
        const val EXTRA_USER_USERNAME = "user_username"
        const val EXTRA_USER_DESCRIPTION = "user_description"
        const val EXTRA_USER_TOPICS = "user_topics"
        const val EXTRA_MATCHING_TOPICS = "matching_topics"
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Create notification channel for user matches (required for Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
                setShowBadge(true)
                enableVibration(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Show notification for a new user match
     */
    fun showUserMatchNotification(user: NearbyUser) {
        if (!areNotificationsEnabled()) {
            return
        }
        
        val matchingTopicsText = user.matchingTopics.joinToString(", ")
        val title = "Found match: $matchingTopicsText"
        val content = if (user.description.isNotBlank()) {
            "${user.username}: ${user.description}"
        } else {
            user.username
        }
        
        // Create intent to open app and show user details
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_USER_USERNAME, user.username)
            putExtra(EXTRA_USER_DESCRIPTION, user.description)
            putExtra(EXTRA_USER_TOPICS, user.topics.toTypedArray())
            putExtra(EXTRA_MATCHING_TOPICS, user.matchingTopics.toTypedArray())
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            user.username.hashCode(), // Unique request code per user
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_user)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Remove notification when clicked
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        
        // Use username hash as notification ID to avoid duplicates
        val notificationId = NOTIFICATION_ID_BASE + user.username.hashCode()
        
        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Permission not granted, ignore silently
        }
    }
    
    /**
     * Check if notifications are enabled
     */
    private fun areNotificationsEnabled(): Boolean {
        return try {
            notificationManager.areNotificationsEnabled()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications() {
        try {
            notificationManager.cancelAll()
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    /**
     * Cancel notification for specific user
     */
    fun cancelNotificationForUser(username: String) {
        try {
            val notificationId = NOTIFICATION_ID_BASE + username.hashCode()
            notificationManager.cancel(notificationId)
        } catch (e: Exception) {
            // Ignore
        }
    }
}