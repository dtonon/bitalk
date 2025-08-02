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
        if (userManager?.hasCompletedOnboarding() == true) {
            startBLEService(currentProfile)
        } else {
            // Create default profile for testing
            createTestProfile()
        }
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
     * Load user profile from storage
     */
    private fun loadUserProfile() {
        val profile = userManager?.getUserProfile() ?: UserProfile(
            username = "anon123",
            description = "",
            topics = emptyList()
        )
        
        _uiState.value = _uiState.value.copy(userProfile = profile)
        Log.d(TAG, "Loaded user profile: ${profile.username}")
    }
    
    /**
     * Create test profile for development
     */
    private fun createTestProfile() {
        val testProfile = UserProfile(
            username = "anon${(100..999).random()}",
            description = "test user",
            topics = DefaultTopics.topics.take(3)
        )
        
        updateUserProfile(testProfile)
        startBLEService(testProfile)
        
        Log.d(TAG, "Created test profile: ${testProfile.username}")
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
            isScanning = false,
            nearbyUsers = emptyList()
        )
        Log.d(TAG, "BLE service stopped")
    }
    
    // BitalkBLEDelegate implementation
    override fun onUserDiscovered(user: NearbyUser) {
        Log.d(TAG, "User discovered: ${user.username} at ${user.formattedDistance}")
        
        val currentUsers = _uiState.value.nearbyUsers.toMutableList()
        currentUsers.add(user)
        
        _uiState.value = _uiState.value.copy(nearbyUsers = currentUsers)
        
        // TODO: Show notification
    }
    
    override fun onUserUpdated(user: NearbyUser) {
        Log.d(TAG, "User updated: ${user.username} at ${user.formattedDistance}")
        
        val currentUsers = _uiState.value.nearbyUsers.toMutableList()
        val index = currentUsers.indexOfFirst { it.deviceAddress == user.deviceAddress }
        
        if (index >= 0) {
            currentUsers[index] = user
            _uiState.value = _uiState.value.copy(nearbyUsers = currentUsers)
        }
    }
    
    override fun onUserLost(user: NearbyUser) {
        Log.d(TAG, "User lost: ${user.username}")
        
        val currentUsers = _uiState.value.nearbyUsers.toMutableList()
        currentUsers.removeAll { it.deviceAddress == user.deviceAddress }
        
        _uiState.value = _uiState.value.copy(nearbyUsers = currentUsers)
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