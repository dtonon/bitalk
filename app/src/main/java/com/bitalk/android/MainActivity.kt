package com.bitalk.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.bitalk.android.data.UserManager
import com.bitalk.android.model.NearbyUser
import com.bitalk.android.navigation.BitalkDestinations
import com.bitalk.android.navigation.BitalkNavigation
import com.bitalk.android.notification.NotificationService
import com.bitalk.android.notification.NotificationClickHandler
import com.bitalk.android.ui.theme.BitalkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle notification click
        handleNotificationIntent()
        
        setContent {
            BitalkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Check if user has completed onboarding
                    val userManager = UserManager(this@MainActivity)
                    val profile = userManager.getUserProfile()
                    val hasCompleted = userManager.hasCompletedOnboarding()
                    
                    android.util.Log.d("MainActivity", "Profile: username=${profile.username}, topics=${profile.topics}, description='${profile.description}'")
                    android.util.Log.d("MainActivity", "Has completed onboarding: $hasCompleted")
                    
                    val startDestination = if (hasCompleted) {
                        android.util.Log.d("MainActivity", "Starting with MAIN_SCREEN")
                        BitalkDestinations.MAIN_SCREEN
                    } else {
                        android.util.Log.d("MainActivity", "Starting with ONBOARDING_WELCOME")
                        BitalkDestinations.ONBOARDING_WELCOME
                    }
                    
                    BitalkNavigation(startDestination = startDestination)
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the intent
        handleNotificationIntent()
    }
    
    /**
     * Handle notification click to show user details
     */
    private fun handleNotificationIntent() {
        val username = intent.getStringExtra(NotificationService.EXTRA_USER_USERNAME)
        val description = intent.getStringExtra(NotificationService.EXTRA_USER_DESCRIPTION)
        val topics = intent.getStringArrayExtra(NotificationService.EXTRA_USER_TOPICS)
        val matchingTopics = intent.getStringArrayExtra(NotificationService.EXTRA_MATCHING_TOPICS)
        
        if (username != null && topics != null && matchingTopics != null) {
            android.util.Log.d("MainActivity", "Handling notification click for user: $username")
            NotificationClickHandler.setUserData(
                username = username,
                description = description ?: "",
                topics = topics,
                matchingTopics = matchingTopics
            )
        }
    }
}