package com.bitalk.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.bitalk.android.data.UserManager
import com.bitalk.android.navigation.BitalkDestinations
import com.bitalk.android.navigation.BitalkNavigation
import com.bitalk.android.ui.theme.BitalkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
}