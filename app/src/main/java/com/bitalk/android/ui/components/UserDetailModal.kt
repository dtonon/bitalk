package com.bitalk.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bitalk.android.R
import com.bitalk.android.model.NearbyUser
import com.bitalk.android.ui.theme.BitalkAccent
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun UserDetailModal(
    user: NearbyUser,
    onDismiss: () -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val timeSinceFirstSeen = (System.currentTimeMillis() - user.firstSeen) / 1000

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // User info
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Username
                    Text(
                        text = user.username,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = BitalkAccent,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Description
                    Text(
                        text = user.description,
                        fontSize = 21.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Distance
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BitalkAccent.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = user.formattedDistance,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Medium,
                            color = BitalkAccent,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Topics list
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sort topics to show matching ones first
                    val sortedTopics = user.topics.sortedBy { !user.matchingTopics.contains(it) }
                    items(sortedTopics) { topic ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (user.matchingTopics.contains(topic)) {
                                BitalkAccent.copy(alpha = 0.2f)
                            } else {
                                Color.Gray.copy(alpha = 0.1f)
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (user.matchingTopics.contains(topic)) {
                                    BitalkAccent
                                } else {
                                    Color.Gray.copy(alpha = 0.3f)
                                }
                            )
                        ) {
                            Text(
                                text = "$topic",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 18.sp,
                                color = if (user.matchingTopics.contains(topic)) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    Color.Gray
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Connection info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.first_seen),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = timeFormat.format(Date(user.firstSeen)),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Column {
                        Text(
                            text = stringResource(R.string.duration),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = formatDuration(timeSinceFirstSeen),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Column {
                        Text(
                            text = stringResource(R.string.signal),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "${user.rssi} dBm",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BitalkAccent
                    )
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }
}
