package com.bitalk.android.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitalk.android.ble.BitalkBLEService
import com.bitalk.android.ble.BitalkBLEDelegate
import com.bitalk.android.data.UserManager
import com.bitalk.android.model.DefaultTopics
import com.bitalk.android.model.NearbyUser
import com.bitalk.android.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Main screen ViewModel managing app state and BLE service
 */
class MainViewModel : ViewModel(), BitalkBLEDelegate {
    
    companion object {
        private const val TAG = "MainViewModel"
    }
    
    // Services
    private var bleService: BitalkBLEService? = null
    private var userManager: UserManager? = null
    
    // UI State
    private val _uiState = MutableStateFlow(MainUIState())
    val uiState: StateFlow<MainUIState> = _uiState.asStateFlow()
    
    /**
     * Initialize ViewModel with Android context
     */
    fun initialize(context: Context) {
        if (bleService != null) return // Already initialized
        
        Log.d(TAG, "Initializing MainViewModel")
        
        // Initialize services
        userManager = UserManager(context)
        bleService = BitalkBLEService(context).apply {
            delegate = this@MainViewModel
        }
        
        // Load user profile
        loadUserProfile()
        
        // Start BLE service if user has completed onboarding
        val currentProfile = _uiState.value.userProfile
        if (userManager?.hasCompletedOnboarding() == true && currentProfile.topics.isNotEmpty()) {
            startBLEService(currentProfile)
        }
        // Don't create test profile - let onboarding handle profile creation
    }
    
    /**
     * Toggle scanning on/off
     */
    fun toggleScanning() {
        val currentState = _uiState.value
        if (currentState.isScanning) {
            stopBLEService()
        } else {
            startBLEService(currentState.userProfile)
        }
    }
    
    /**
     * Update user profile
     */
    fun updateUserProfile(newProfile: UserProfile) {
        userManager?.saveUserProfile(newProfile)
        _uiState.value = _uiState.value.copy(userProfile = newProfile)
        
        // Update BLE service with new profile
        bleService?.updateUserProfile(newProfile)
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
     * Start BLE service
     */
    private fun startBLEService(profile: UserProfile) {
        viewModelScope.launch {
            val success = bleService?.startServices(profile) ?: false
            _uiState.value = _uiState.value.copy(isScanning = success)
            
            if (success) {
                Log.d(TAG, "BLE service started successfully")
            } else {
                Log.e(TAG, "Failed to start BLE service")
            }
        }
    }
    
    /**
     * Stop BLE service
     */
    private fun stopBLEService() {
        bleService?.stopServices()
        _uiState.value = _uiState.value.copy(
            isScanning = false
            // DON'T clear nearbyUsers - let them timeout naturally via onUserLost()
        )
        Log.d(TAG, "BLE service stopped - keeping existing users until they timeout")
    }
    
    // BitalkBLEDelegate implementation
    override fun onUserDiscovered(user: NearbyUser) {
        Log.d(TAG, "User discovered: ${user.username} at ${user.formattedDistance}")
        
        val currentUsers = _uiState.value.nearbyUsers.toMutableList()
        
        // Check if user already exists by username (prevent duplicates)
        val existingIndex = currentUsers.indexOfFirst { it.username == user.username }
        if (existingIndex >= 0) {
            // Update existing user instead of adding duplicate
            Log.d(TAG, "User ${user.username} already exists, updating instead of adding")
            currentUsers[existingIndex] = user
        } else {
            // Add new user
            currentUsers.add(user)
        }
        
        _uiState.value = _uiState.value.copy(nearbyUsers = currentUsers)
        
        // TODO: Show notification
    }
    
    override fun onUserUpdated(user: NearbyUser) {
        Log.d(TAG, "User updated: ${user.username} at ${user.formattedDistance}")
        
        val currentUsers = _uiState.value.nearbyUsers.toMutableList()
        
        // Find user by username (not device address)
        val index = currentUsers.indexOfFirst { it.username == user.username }
        
        if (index >= 0) {
            currentUsers[index] = user
            _uiState.value = _uiState.value.copy(nearbyUsers = currentUsers)
        } else {
            Log.w(TAG, "Tried to update non-existent user: ${user.username}")
            // Add user if not found (fallback)
            currentUsers.add(user)
            _uiState.value = _uiState.value.copy(nearbyUsers = currentUsers)
        }
    }
    
    override fun onUserLost(user: NearbyUser) {
        Log.d(TAG, "User lost: ${user.username}")
        
        val currentUsers = _uiState.value.nearbyUsers.toMutableList()
        
        // Remove user by username (not device address)
        val removed = currentUsers.removeAll { it.username == user.username }
        
        if (removed) {
            Log.d(TAG, "Removed user ${user.username}")
            _uiState.value = _uiState.value.copy(nearbyUsers = currentUsers)
        } else {
            Log.w(TAG, "Tried to remove non-existent user: ${user.username}")
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopBLEService()
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