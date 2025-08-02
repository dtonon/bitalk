package com.bitalk.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bitalk.android.R
import com.bitalk.android.ui.theme.BitalkAccent

@Composable
fun TopicPreferencesModal(
    exactMatchMode: Boolean,
    onPreferenceChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(exactMatchMode) }

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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.topic_preferences),
                        fontSize = 24.sp,
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

                Spacer(modifier = Modifier.height(24.dp))

                // Matching mode options
                Text(
                    text = "Topic Matching Mode",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Partial matching option
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = { selectedMode = false },
                    colors = CardDefaults.cardColors(
                        containerColor = if (!selectedMode)
                            BitalkAccent.copy(alpha = 0.1f)
                        else
                            MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (!selectedMode) 2.dp else 1.dp,
                        color = if (!selectedMode) BitalkAccent else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = !selectedMode,
                                onClick = { selectedMode = false },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = BitalkAccent
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = stringResource(R.string.partial_matching),
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (!selectedMode) BitalkAccent else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "Matches partial topics (e.g., \"art\" matches \"artistic\", \"pop-art\")",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Exact matching option
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = { selectedMode = true },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedMode)
                            BitalkAccent.copy(alpha = 0.1f)
                        else
                            MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (selectedMode) 2.dp else 1.dp,
                        color = if (selectedMode) BitalkAccent else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMode,
                                onClick = { selectedMode = true },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = BitalkAccent
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = stringResource(R.string.exact_matching),
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (selectedMode) BitalkAccent else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "Only matches identical topics (e.g., \"bitcoin\" only matches \"bitcoin\")",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            fontSize = 18.sp
                        )
                    }

                    Button(
                        onClick = {
                            onPreferenceChanged(selectedMode)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BitalkAccent
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.save),
                            fontSize = 18.sp

                        )
                    }
                }
            }
        }
    }
}
