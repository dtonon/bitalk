package com.bitalk.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * User profile data that persists across app restarts
 */
@Parcelize
data class UserProfile(
    val username: String,
    val description: String,
    val topics: List<String>,
    val allCustomTopics: List<String> = emptyList(),
    val exactMatchMode: Boolean = false
) : Parcelable

/**
 * Default topics list covering diverse interests
 */
object DefaultTopics {
    val topics = listOf(
        "bitcoin", "nostr", "crypto", "ethereum",
        "pizza", "coffee", "food", "cooking", 
        "music", "art", "photography", "design",
        "tech", "programming", "android", "linux",
        "travel", "hiking", "fitness", "yoga",
        "gaming", "books", "movies", "anime",
        "startup", "investing", "trading",
        "weird-interests", "memes", "cats"
    )
}