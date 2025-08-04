package com.bitalk.android.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitalk.android.data.UserManager
import com.bitalk.android.service.ServiceManager
import com.bitalk.android.model.DefaultTopics
import com.bitalk.android.model.NearbyUser
import com.bitalk.android.model.UserProfile
import com.bitalk.android.notification.NotificationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Main screen ViewModel managing app state
 */
class MainViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "MainViewModel"
    }
    
    // Services
    private var userManager: UserManager? = null
    
    // UI State
    private val _uiState = MutableStateFlow(MainUIState())
    val uiState: StateFlow<MainUIState> = _uiState.asStateFlow()
    
    /**
     * Initialize ViewModel with Android context
     */
    fun initialize(context: Context) {
        if (userManager != null) return // Already initialized
        
        Log.d(TAG, "Initializing MainViewModel")
        
        // Initialize services
        userManager = UserManager(context)
        
        // Load user profile
        loadUserProfile()
        
        // Update scanning status based on actual BLE scanning state
        updateState { it.copy(isScanning = ServiceManager.isBLEScanning()) }
        
        // Start periodic updates to get nearby users from service
        startPeriodicUpdates()
    }
    
    /**
     * Toggle scanning on/off
     */
    fun toggleScanning() {
        Log.d(TAG, "Toggle scanning requested")
        val success = ServiceManager.toggleScanning()
        val isScanning = ServiceManager.isBLEScanning()
        updateState { it.copy(isScanning = isScanning) }
        Log.d(TAG, "Scanning toggled - success: $success, isScanning: $isScanning")
    }
    
    /**
     * Update user profile
     */
    fun updateUserProfile(newProfile: UserProfile) {
        userManager?.saveUserProfile(newProfile)
        updateState { it.copy(userProfile = newProfile) }
        
        // Note: Profile updates will be handled by the foreground service
        Log.d(TAG, "Profile updated: ${newProfile.username}")
    }
    
    /**
     * Update username
     */
    fun updateUsername(newUsername: String) {
        val currentProfile = _uiState.value.userProfile
        val updatedProfile = currentProfile.copy(username = newUsername)
        updateUserProfile(updatedProfile)
    }
    
    /**
     * Update description
     */
    fun updateDescription(newDescription: String) {
        val currentProfile = _uiState.value.userProfile
        val updatedProfile = currentProfile.copy(description = newDescription)
        updateUserProfile(updatedProfile)
    }
    
    /**
     * Update topics
     */
    fun updateTopics(newTopics: List<String>) {
        val currentProfile = _uiState.value.userProfile
        val updatedProfile = currentProfile.copy(topics = newTopics)
        updateUserProfile(updatedProfile)
    }
    
    /**
     * Update topics with all custom topics list
     */
    fun updateTopics(newTopics: List<String>, allCustomTopics: List<String>) {
        val currentProfile = _uiState.value.userProfile
        val updatedProfile = currentProfile.copy(topics = newTopics, allCustomTopics = allCustomTopics)
        updateUserProfile(updatedProfile)
    }
    
    /**
     * Update exact match mode
     */
    fun updateExactMatchMode(exactMode: Boolean) {
        val currentProfile = _uiState.value.userProfile
        val updatedProfile = currentProfile.copy(exactMatchMode = exactMode)
        updateUserProfile(updatedProfile)
    }
    
    /**
     * Load user profile from storage
     */
    private fun loadUserProfile() {
        val profile = userManager?.getUserProfile() ?: UserProfile(
            username = "anon123",
            description = "",
            topics = emptyList()
        )
        
        _uiState.value = _uiState.value.copy(userProfile = profile)
        Log.d(TAG, "Loaded user profile: ${profile.username} with topics: ${profile.topics}")
    }
    
    
    /**
     * Start periodic updates to get nearby users from service
     */
    private fun startPeriodicUpdates() {
        viewModelScope.launch {
            while (true) {
                // Get nearby users from service
                val nearbyUsers = ServiceManager.getNearbyUsers()
                val isScanning = ServiceManager.isBLEScanning()
                
                updateState { state ->
                    state.copy(
                        nearbyUsers = nearbyUsers,
                        isScanning = isScanning
                    )
                }
                
                // Wait before next update
                kotlinx.coroutines.delay(2000) // Update every 2 seconds
            }
        }
    }
    
    /**
     * Helper method to update state
     */
    private fun updateState(update: (MainUIState) -> MainUIState) {
        _uiState.value = update(_uiState.value)
    }
    
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "MainViewModel cleared")
    }
}

/**
 * UI state for main screen
 */
data class MainUIState(
    val userProfile: UserProfile = UserProfile("", "", emptyList()),
    val nearbyUsers: List<NearbyUser> = emptyList(),
    val isScanning: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)