package com.bitalk.android.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bitalk.android.ui.screens.onboarding.*
import com.bitalk.android.ui.screens.MainScreen

/**
 * Navigation routes for Bitalk app
 */
object BitalkDestinations {
    const val ONBOARDING_WELCOME = "onboarding_welcome"
    const val ONBOARDING_TOPICS = "onboarding_topics"
    const val ONBOARDING_DESCRIPTION = "onboarding_description"
    const val ONBOARDING_PERMISSIONS = "onboarding_permissions"
    const val MAIN_SCREEN = "main_screen"
}

/**
 * Main navigation component for Bitalk
 */
@Composable
fun BitalkNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = BitalkDestinations.ONBOARDING_WELCOME
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Onboarding flow
        composable(BitalkDestinations.ONBOARDING_WELCOME) {
            OnboardingWelcomeScreen(
                onNext = {
                    navController.navigate(BitalkDestinations.ONBOARDING_TOPICS)
                }
            )
        }
        
        composable(BitalkDestinations.ONBOARDING_TOPICS) {
            OnboardingTopicsScreen(
                onNext = {
                    navController.navigate(BitalkDestinations.ONBOARDING_DESCRIPTION)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(BitalkDestinations.ONBOARDING_DESCRIPTION) {
            OnboardingDescriptionScreen(
                onNext = {
                    navController.navigate(BitalkDestinations.ONBOARDING_PERMISSIONS)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(BitalkDestinations.ONBOARDING_PERMISSIONS) {
            OnboardingPermissionsScreen(
                onComplete = {
                    navController.navigate(BitalkDestinations.MAIN_SCREEN) {
                        // Clear backstack so user can't go back to onboarding
                        popUpTo(BitalkDestinations.ONBOARDING_WELCOME) {
                            inclusive = true
                        }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Main app screen
        composable(BitalkDestinations.MAIN_SCREEN) {
            MainScreen()
        }
    }
}