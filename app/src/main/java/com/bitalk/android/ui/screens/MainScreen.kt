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
                EmptyState()
            } else {
                BubbleArea(
                    nearbyUsers = uiState.nearbyUsers,
                    onUserClick = { user ->
                        // TODO: Show user detail modal
                    }
                )
            }
        }
        
        // Topics Section
        TopicsSection(
            topics = uiState.userProfile.topics,
            onTopicsClick = {
                // TODO: Show topics modal
            },
            onPreferencesClick = {
                // TODO: Show preferences modal
            }
        )
        
        // Username Footer
        UsernameFooter(
            username = uiState.userProfile.username,
            onUsernameClick = {
                // TODO: Show username change modal
            }
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
                    fontSize = 14.sp,
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
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = BitalkAccent,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Description
            Text(
                text = user.description,
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                maxLines = 2
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
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No users found yet",
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Make sure Bluetooth is enabled and you're in a public place",
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TopicsSection(
    topics: List<String>,
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
                topics.take(3).forEach { topic ->
                    TopicChip(topic = topic)
                }
                if (topics.size > 3) {
                    Text(
                        text = "+${topics.size - 3}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            
            // Preferences icon
            IconButton(onClick = onPreferencesClick) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Preferences",
                    tint = Color.Gray
                )
            }
        }
    }
}

@Composable
fun TopicChip(topic: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = BitalkAccent.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, BitalkAccent)
    ) {
        Text(
            text = "🏷️ $topic",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            color = BitalkAccent
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
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}