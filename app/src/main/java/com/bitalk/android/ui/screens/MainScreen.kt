package com.bitalk.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bitalk.android.model.NearbyUser
import com.bitalk.android.notification.NotificationClickHandler
import com.bitalk.android.ui.components.*
import com.bitalk.android.ui.theme.BitalkAccent
import com.bitalk.android.ui.viewmodel.MainViewModel

@Composable
fun MainScreen() {
    val viewModel: MainViewModel = viewModel()
    val context = LocalContext.current

    // Initialize ViewModel with context
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    val uiState by viewModel.uiState.collectAsState()

    // Modal states
    var showTopicsModal by remember { mutableStateOf(false) }
    var showPreferencesModal by remember { mutableStateOf(false) }
    var showUsernameModal by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<NearbyUser?>(null) }

    // Handle notification clicks
    LaunchedEffect(Unit) {
        // Check for any pending notification data first
        NotificationClickHandler.getPendingUserData()?.let { userData ->
            val userFromNotification = NearbyUser(
                username = userData.username,
                description = userData.description,
                topics = userData.topics,
                rssi = -50, // Default values since we don't have real BLE data
                estimatedDistance = 0.0,
                matchingTopics = userData.matchingTopics,
                firstSeen = System.currentTimeMillis(),
                lastSeen = System.currentTimeMillis(),
                deviceAddress = null
            )
            selectedUser = userFromNotification
        }

        // Set listener for future notification clicks
        NotificationClickHandler.setOnUserDataAvailable { userData ->
            val userFromNotification = NearbyUser(
                username = userData.username,
                description = userData.description,
                topics = userData.topics,
                rssi = -50, // Default values since we don't have real BLE data
                estimatedDistance = 0.0,
                matchingTopics = userData.matchingTopics,
                firstSeen = System.currentTimeMillis(),
                lastSeen = System.currentTimeMillis(),
                deviceAddress = null
            )
            selectedUser = userFromNotification
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Status Bar
        StatusBar(
            nearbyUserCount = uiState.nearbyUsers.size,
            isScanning = uiState.isScanning,
            onToggleScanning = { viewModel.toggleScanning() }
        )

        // Main Content Area - Bubbles
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (uiState.nearbyUsers.isEmpty()) {
                EmptyState(isScanning = uiState.isScanning)
            } else {
                BubbleArea(
                    nearbyUsers = uiState.nearbyUsers,
                    onUserClick = { user ->
                        selectedUser = user
                    }
                )
            }
        }

        // Topics Section
        TopicsSection(
            topics = uiState.userProfile.topics,
            exactMatchMode = uiState.userProfile.exactMatchMode,
            onTopicsClick = {
                showTopicsModal = true
            },
            onPreferencesClick = {
                showPreferencesModal = true
            }
        )

        // Username Footer
        UsernameFooter(
            username = uiState.userProfile.username,
            onUsernameClick = {
                showUsernameModal = true
            }
        )
    }

    // Modals
    if (showTopicsModal) {
        TopicEditModal(
            currentTopics = uiState.userProfile.topics,
            allCustomTopics = uiState.userProfile.allCustomTopics,
            onTopicsChanged = { newTopics, allCustomTopics ->
                viewModel.updateTopics(newTopics, allCustomTopics)
            },
            onDismiss = { showTopicsModal = false }
        )
    }

    if (showPreferencesModal) {
        TopicPreferencesModal(
            exactMatchMode = uiState.userProfile.exactMatchMode,
            onPreferenceChanged = { exactMode ->
                viewModel.updateExactMatchMode(exactMode)
            },
            onDismiss = { showPreferencesModal = false }
        )
    }

    if (showUsernameModal) {
        UsernameEditModal(
            currentUsername = uiState.userProfile.username,
            onUsernameChanged = { newUsername ->
                viewModel.updateUsername(newUsername)
            },
            onDismiss = { showUsernameModal = false }
        )
    }

    selectedUser?.let { user ->
        UserDetailModal(
            user = user,
            onDismiss = { selectedUser = null }
        )
    }
}

@Composable
fun StatusBar(
    nearbyUserCount: Int,
    isScanning: Boolean,
    onToggleScanning: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFE0E0E0)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (isScanning) Color.Green else Color.Gray,
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isScanning) "Scanning: $nearbyUserCount nearby users" else "Paused",
                    fontSize = 18.sp,
                    color = Color.Black
                )
            }

            IconButton(
                onClick = onToggleScanning,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isScanning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isScanning) "Pause" else "Play",
                    tint = Color.Black
                )
            }
        }
    }
}

@Composable
fun BubbleArea(
    nearbyUsers: List<NearbyUser>,
    onUserClick: (NearbyUser) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom,
        contentPadding = PaddingValues(16.dp)
    ) {
        items(nearbyUsers.sortedBy { it.estimatedDistance }) { user ->
            UserBubble(
                user = user,
                onClick = { onUserClick(user) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun UserBubble(
    user: NearbyUser,
    onClick: () -> Unit
) {
    // Calculate bubble size based on distance
    val baseSize = 120.dp
    val sizeMultiplier = when {
        user.estimatedDistance < 1.0 -> 1.2f
        user.estimatedDistance < 3.0 -> 1.0f
        user.estimatedDistance < 6.0 -> 0.8f
        else -> 0.6f
    }
    val bubbleSize = baseSize * sizeMultiplier

    Card(
        modifier = Modifier
            .size(bubbleSize)
            .clickable { onClick() },
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(2.dp, BitalkAccent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Username
            Text(
                text = user.username,
                fontSize = 18.sp,
                color = BitalkAccent,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Description
            Text(
                text = user.description,
                fontSize = 18.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Distance
            Text(
                text = user.formattedDistance,
                fontSize = 11.sp,
                color = BitalkAccent,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EmptyState(isScanning: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isScanning) "Scanning for users..." else "Scanning paused",
            fontSize = 22.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isScanning) {
                "Make sure Bluetooth is enabled and you're in a public place"
            } else {
                "Tap the play button to start scanning"
            },
            fontSize = 18.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TopicsSection(
    topics: List<String>,
    exactMatchMode: Boolean,
    onTopicsClick: () -> Unit,
    onPreferencesClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Topics chips
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTopicsClick() },
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (topics.isEmpty()) {
                    Text(
                        text = "Tap to add topics",
                        fontSize = 18.sp,
                        color = Color.Gray
                    )
                } else {
                    topics.take(3).forEach { topic ->
                        TopicChip(topic = topic)
                    }
                    if (topics.size > 3) {
                        Text(
                            text = "+${topics.size - 3}",
                            fontSize = 18.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Preferences icon with indicator
            Box {
                IconButton(onClick = onPreferencesClick) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Preferences",
                        tint = Color.Gray
                    )
                }

                if (exactMatchMode) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(BitalkAccent, CircleShape)
                            .align(Alignment.TopEnd)
                    )
                }
            }
        }
    }
}

@Composable
fun TopicChip(topic: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = BitalkAccent.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, BitalkAccent)
    ) {
        Text(
            text = topic,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun UsernameFooter(
    username: String,
    onUsernameClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = "bitalk/$username",
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onUsernameClick() }
                .padding(16.dp),
            fontSize = 18.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}
