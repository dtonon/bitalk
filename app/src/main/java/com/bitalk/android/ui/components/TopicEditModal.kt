package com.bitalk.android.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TopicEditModal(
    currentTopics: List<String>?,
    allCustomTopics: List<String>? = null,
    onTopicsChanged: (selectedTopics: List<String>, allCustomTopics: List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTopics by remember { mutableStateOf((currentTopics ?: emptyList()).toSet()) }
    var allCustomTopicsList by remember { mutableStateOf((allCustomTopics ?: emptyList()).toSet()) }
    var customTopic by remember { mutableStateOf("") }
    var showAddCustom by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    
    // Create a stable list that maintains order and includes all custom topics (selected and unselected)
    val allTopics = remember(selectedTopics, allCustomTopicsList) {
        try {
            val selectedCustomTopics = allCustomTopicsList.filter { selectedTopics.contains(it) }
            val selectedDefaults = DefaultTopics.topics.filter { selectedTopics.contains(it) }
            val unselectedCustomTopics = allCustomTopicsList.filter { !selectedTopics.contains(it) }.sorted()
            val unselectedDefaults = DefaultTopics.topics.filter { !selectedTopics.contains(it) }.sorted()
            
            // Selected topics first (custom + default), then unselected sorted (custom + default)
            selectedCustomTopics + selectedDefaults + unselectedCustomTopics + unselectedDefaults
        } catch (e: Exception) {
            // Fallback to simple list
            DefaultTopics.topics
        }
    }
    
    // Auto-scroll to top when custom topic is added
    LaunchedEffect(allCustomTopicsList.size) {
        if (allCustomTopicsList.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }
    
    // Auto-focus when input appears
    LaunchedEffect(showAddCustom) {
        if (showAddCustom) {
            delay(100)
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore focus errors
            }
        }
    }
    
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
                
                // Topics list - maintains position, only changes selection state
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allTopics) { topic ->
                        val isSelected = selectedTopics.contains(topic)
                        val isCustom = !DefaultTopics.topics.contains(topic)
                        
                        TopicSelectionItem(
                            topic = topic,
                            isSelected = isSelected,
                            isCustom = isCustom,
                            onToggle = {
                                selectedTopics = if (isSelected) {
                                    selectedTopics - topic
                                } else {
                                    selectedTopics + topic
                                }
                            }
                        )
                    }
                    
                    // Add custom topic
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (showAddCustom) {
                            OutlinedTextField(
                                value = customTopic,
                                onValueChange = { customTopic = it },
                                label = { Text("Custom Topic") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words
                                ),
                                trailingIcon = {
                                    TextButton(
                                        onClick = {
                                            if (customTopic.isNotBlank()) {
                                                val newCustomTopic = customTopic.trim().lowercase()
                                                allCustomTopicsList = allCustomTopicsList + newCustomTopic
                                                selectedTopics = selectedTopics + newCustomTopic
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
                            
                            // Add extra space below input
                            Spacer(modifier = Modifier.height(16.dp))
                        } else {
                            OutlinedButton(
                                onClick = { 
                                    showAddCustom = true
                                },
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
                            onTopicsChanged(selectedTopics.toList(), allCustomTopicsList.toList())
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