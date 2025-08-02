package com.bitalk.android.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bitalk.android.R
import com.bitalk.android.model.DefaultTopics
import com.bitalk.android.ui.theme.BitalkAccent

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TopicEditModal(
    currentTopics: List<String>,
    onTopicsChanged: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTopics by remember { mutableStateOf(currentTopics.toSet()) }
    var customTopic by remember { mutableStateOf("") }
    var showAddCustom by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Topics",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = BitalkAccent
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Selected count
                Text(
                    text = "Selected: ${selectedTopics.size}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = BitalkAccent
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Topics list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Default topics
                    items(DefaultTopics.topics) { topic ->
                        TopicSelectionItem(
                            topic = topic,
                            isSelected = selectedTopics.contains(topic),
                            onToggle = {
                                selectedTopics = if (selectedTopics.contains(topic)) {
                                    selectedTopics - topic
                                } else {
                                    selectedTopics + topic
                                }
                            }
                        )
                    }
                    
                    // Custom topics (not in default list)
                    val customTopics = selectedTopics.filter { !DefaultTopics.topics.contains(it) }
                    if (customTopics.isNotEmpty()) {
                        item {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                text = "Custom Topics",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        
                        items(customTopics) { topic ->
                            TopicSelectionItem(
                                topic = topic,
                                isSelected = true,
                                isCustom = true,
                                onToggle = {
                                    selectedTopics = selectedTopics - topic
                                }
                            )
                        }
                    }
                    
                    // Add custom topic
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (showAddCustom) {
                            OutlinedTextField(
                                value = customTopic,
                                onValueChange = { customTopic = it },
                                label = { Text("Custom Topic") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words
                                ),
                                trailingIcon = {
                                    TextButton(
                                        onClick = {
                                            if (customTopic.isNotBlank()) {
                                                selectedTopics = selectedTopics + customTopic.trim().lowercase()
                                                customTopic = ""
                                                showAddCustom = false
                                            }
                                        }
                                    ) {
                                        Text("Add")
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BitalkAccent,
                                    focusedLabelColor = BitalkAccent
                                )
                            )
                        } else {
                            OutlinedButton(
                                onClick = { showAddCustom = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Custom Topic")
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    
                    Button(
                        onClick = {
                            onTopicsChanged(selectedTopics.toList())
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BitalkAccent
                        )
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}

@Composable
fun TopicSelectionItem(
    topic: String,
    isSelected: Boolean,
    isCustom: Boolean = false,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) BitalkAccent.copy(alpha = 0.1f) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .border(
                width = 1.dp,
                color = if (isSelected) BitalkAccent else Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isCustom) "⭐ $topic" else "🏷️ $topic",
                fontSize = 14.sp,
                color = if (isSelected) BitalkAccent else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove",
                    tint = BitalkAccent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}