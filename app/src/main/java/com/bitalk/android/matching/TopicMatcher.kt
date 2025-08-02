package com.bitalk.android.matching

import com.bitalk.android.model.TopicBroadcast
import com.bitalk.android.model.UserProfile

/**
 * Handles topic matching logic with support for partial and exact matching
 */
object TopicMatcher {
    
    /**
     * Find matching topics between user profile and discovered broadcast
     * 
     * @param userProfile Current user's profile
     * @param broadcast Discovered user's broadcast
     * @return List of matching topics
     */
    fun findMatchingTopics(
        userProfile: UserProfile,
        broadcast: TopicBroadcast
    ): List<String> {
        val userTopics = userProfile.topics.map { it.lowercase() }
        val broadcastTopics = broadcast.topics.map { it.lowercase() }
        
        return if (userProfile.exactMatchMode) {
            findExactMatches(userTopics, broadcastTopics)
        } else {
            findPartialMatches(userTopics, broadcastTopics)
        }
    }
    
    /**
     * Check if there are any matching topics
     */
    fun hasMatchingTopics(userProfile: UserProfile, broadcast: TopicBroadcast): Boolean {
        return findMatchingTopics(userProfile, broadcast).isNotEmpty()
    }
    
    /**
     * Calculate match score (0.0 to 1.0) based on topic overlap
     */
    fun calculateMatchScore(userProfile: UserProfile, broadcast: TopicBroadcast): Float {
        val matchingTopics = findMatchingTopics(userProfile, broadcast)
        val totalTopics = (userProfile.topics + broadcast.topics).distinct().size
        
        return if (totalTopics == 0) 0f else matchingTopics.size.toFloat() / totalTopics.toFloat()
    }
    
    /**
     * Find exact topic matches (case-insensitive)
     */
    private fun findExactMatches(userTopics: List<String>, broadcastTopics: List<String>): List<String> {
        return userTopics.intersect(broadcastTopics.toSet()).toList()
    }
    
    /**
     * Find partial topic matches (substring matching)
     * Examples: "bitcoin" matches "bitcoin-core", "lightning", "crypto"
     */
    private fun findPartialMatches(userTopics: List<String>, broadcastTopics: List<String>): List<String> {
        val matches = mutableSetOf<String>()
        
        for (userTopic in userTopics) {
            for (broadcastTopic in broadcastTopics) {
                if (isPartialMatch(userTopic, broadcastTopic)) {
                    matches.add(userTopic) // Add original user topic
                }
            }
        }
        
        return matches.toList()
    }
    
    /**
     * Check if two topics are partial matches
     * Handles bidirectional substring matching and common prefixes
     */
    private fun isPartialMatch(topic1: String, topic2: String): Boolean {
        // Exact match
        if (topic1 == topic2) return true
        
        // Substring matching (bidirectional)
        if (topic1.contains(topic2) || topic2.contains(topic1)) return true
        
        // Common prefix matching (minimum 3 characters)
        if (topic1.length >= 3 && topic2.length >= 3) {
            val minLength = minOf(topic1.length, topic2.length)
            for (i in 3..minLength) {
                if (topic1.substring(0, i) == topic2.substring(0, i)) {
                    return true
                }
            }
        }
        
        // Fuzzy matching for common variations
        return isFuzzyMatch(topic1, topic2)
    }
    
    /**
     * Handle common topic variations and synonyms
     */
    private fun isFuzzyMatch(topic1: String, topic2: String): Boolean {
        val synonyms = mapOf(
            "crypto" to listOf("bitcoin", "ethereum", "blockchain"),
            "tech" to listOf("programming", "coding", "software"),
            "fitness" to listOf("gym", "workout", "exercise"),
            "food" to listOf("cooking", "recipes", "eating"),
            "music" to listOf("songs", "audio", "sound"),
            "art" to listOf("drawing", "painting", "creative")
        )
        
        for ((key, values) in synonyms) {
            if ((topic1 == key && values.contains(topic2)) ||
                (topic2 == key && values.contains(topic1)) ||
                (values.contains(topic1) && values.contains(topic2))) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Get suggested topics based on existing user topics
     */
    fun getSuggestedTopics(userTopics: List<String>, allTopics: List<String>): List<String> {
        return allTopics.filter { topic ->
            !userTopics.map { it.lowercase() }.contains(topic.lowercase()) &&
            userTopics.any { userTopic -> isPartialMatch(userTopic.lowercase(), topic.lowercase()) }
        }.take(5)
    }
}