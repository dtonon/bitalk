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
        "android", "anime", "art", "bitcoin", "books", "cats", "coffee", "cooking", "design", "fitness", "gaming", "hiking", "linux", "memes", "movies", "music", "nostr", "open-source", "photography", "programming", "pizza", "startup", "tech", "travel", "yoga"
    )
}