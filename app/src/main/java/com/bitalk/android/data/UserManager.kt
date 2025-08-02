package com.bitalk.android.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.bitalk.android.model.UserProfile
import com.google.gson.Gson
import kotlin.random.Random

/**
 * Manages user profile persistence and username generation
 */
class UserManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "bitalk_user_prefs"
        private const val KEY_USER_PROFILE = "user_profile"
        private const val USERNAME_PREFIX = "anon"
    }
    
    private val gson = Gson()
    
    // Encrypted shared preferences for security
    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
            
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    /**
     * Save user profile to encrypted storage
     */
    fun saveUserProfile(profile: UserProfile) {
        val json = gson.toJson(profile)
        encryptedPrefs.edit()
            .putString(KEY_USER_PROFILE, json)
            .apply()
    }
    
    /**
     * Load user profile from storage, or create default if none exists
     */
    fun getUserProfile(): UserProfile {
        val json = encryptedPrefs.getString(KEY_USER_PROFILE, null)
        
        return if (json != null) {
            try {
                gson.fromJson(json, UserProfile::class.java)
            } catch (e: Exception) {
                createDefaultProfile()
            }
        } else {
            createDefaultProfile()
        }
    }
    
    /**
     * Update username only
     */
    fun updateUsername(newUsername: String) {
        val currentProfile = getUserProfile()
        val updatedProfile = currentProfile.copy(username = newUsername)
        saveUserProfile(updatedProfile)
    }
    
    /**
     * Update topics only
     */
    fun updateTopics(newTopics: List<String>) {
        val currentProfile = getUserProfile()
        val updatedProfile = currentProfile.copy(topics = newTopics)
        saveUserProfile(updatedProfile)
    }
    
    /**
     * Update description only
     */
    fun updateDescription(newDescription: String) {
        val currentProfile = getUserProfile()
        val updatedProfile = currentProfile.copy(description = newDescription)
        saveUserProfile(updatedProfile)
    }
    
    /**
     * Update exact match mode preference
     */
    fun updateExactMatchMode(exactMatch: Boolean) {
        val currentProfile = getUserProfile()
        val updatedProfile = currentProfile.copy(exactMatchMode = exactMatch)
        saveUserProfile(updatedProfile)
    }
    
    /**
     * Check if user has completed onboarding
     */
    fun hasCompletedOnboarding(): Boolean {
        val profile = getUserProfile()
        return profile.topics.isNotEmpty() && profile.description.isNotBlank()
    }
    
    /**
     * Clear all user data (for panic mode or reset)
     */
    fun clearUserData() {
        encryptedPrefs.edit().clear().apply()
    }
    
    /**
     * Create default user profile with generated username
     */
    private fun createDefaultProfile(): UserProfile {
        val username = generateRandomUsername()
        return UserProfile(
            username = username,
            description = "",
            topics = emptyList(),
            exactMatchMode = false
        )
    }
    
    /**
     * Generate random username like "anon123"
     */
    private fun generateRandomUsername(): String {
        val number = Random.nextInt(100, 10000)
        return "$USERNAME_PREFIX$number"
    }
    
    /**
     * Validate username format
     */
    fun isValidUsername(username: String): Boolean {
        return username.isNotBlank() && 
               username.length in 3..20 && 
               username.matches(Regex("^[a-zA-Z0-9_]+$"))
    }
    
    /**
     * Validate description
     */
    fun isValidDescription(description: String): Boolean {
        return description.isNotBlank() && description.length <= 100
    }
}