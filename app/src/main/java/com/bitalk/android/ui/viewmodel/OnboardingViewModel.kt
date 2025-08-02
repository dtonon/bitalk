package com.bitalk.android.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.bitalk.android.data.UserManager
import com.bitalk.android.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for onboarding flow
 */
class OnboardingViewModel : ViewModel() {
    
    private var userManager: UserManager? = null
    
    // UI State
    private val _uiState = MutableStateFlow(OnboardingUIState())
    val uiState: StateFlow<OnboardingUIState> = _uiState.asStateFlow()
    
    /**
     * Initialize with Android context
     */
    fun initialize(context: Context) {
        if (userManager != null) return
        
        userManager = UserManager(context)
        
        // Load existing profile if any
        val existingProfile = userManager?.getUserProfile()
        existingProfile?.let { profile ->
            _uiState.value = _uiState.value.copy(
                selectedTopics = profile.topics,
                description = profile.description,
                username = profile.username
            )
        }
    }
    
    /**
     * Toggle topic selection and save immediately
     */
    fun toggleTopic(topic: String) {
        val currentTopics = _uiState.value.selectedTopics.toMutableList()
        
        if (currentTopics.contains(topic)) {
            currentTopics.remove(topic)
        } else {
            currentTopics.add(topic)
        }
        
        _uiState.value = _uiState.value.copy(selectedTopics = currentTopics)
        
        // Save topics immediately
        saveCurrentState()
    }
    
    /**
     * Update description and save immediately
     */
    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
        
        // Save description immediately
        saveCurrentState()
    }
    
    /**
     * Save current state to storage (called after each step)
     */
    private fun saveCurrentState() {
        val state = _uiState.value
        val profile = UserProfile(
            username = state.username.ifEmpty { generateRandomUsername() },
            description = state.description,
            topics = state.selectedTopics,
            exactMatchMode = false
        )
        
        userManager?.saveUserProfile(profile)
        android.util.Log.d("OnboardingViewModel", "Saved profile: username=${profile.username}, topics=${profile.topics}, description='${profile.description}'")
        
        // Verify save by reading back
        val savedProfile = userManager?.getUserProfile()
        android.util.Log.d("OnboardingViewModel", "Verified saved profile: username=${savedProfile?.username}, topics=${savedProfile?.topics}, description='${savedProfile?.description}'")
    }
    
    /**
     * Complete onboarding and save profile
     */
    fun completeOnboarding() {
        val state = _uiState.value
        val profile = UserProfile(
            username = state.username.ifEmpty { generateRandomUsername() },
            description = state.description,
            topics = state.selectedTopics,
            exactMatchMode = false
        )
        
        userManager?.saveUserProfile(profile)
        android.util.Log.d("OnboardingViewModel", "Completed onboarding with profile: username=${profile.username}, topics=${profile.topics}, description='${profile.description}'")
        
        // Update the UI state to reflect the saved profile
        _uiState.value = _uiState.value.copy(
            username = profile.username,
            description = profile.description,
            selectedTopics = profile.topics
        )
        
        // Final verification
        val savedProfile = userManager?.getUserProfile()
        android.util.Log.d("OnboardingViewModel", "Final verification - profile saved: username=${savedProfile?.username}, topics=${savedProfile?.topics}, description='${savedProfile?.description}'")
        android.util.Log.d("OnboardingViewModel", "Onboarding completed: ${userManager?.hasCompletedOnboarding()}")
    }
    
    /**
     * Generate random username
     */
    private fun generateRandomUsername(): String {
        return "anon${(100..9999).random()}"
    }
}

/**
 * UI state for onboarding
 */
data class OnboardingUIState(
    val selectedTopics: List<String> = emptyList(),
    val description: String = "",
    val username: String = "",
    val isLoading: Boolean = false
)