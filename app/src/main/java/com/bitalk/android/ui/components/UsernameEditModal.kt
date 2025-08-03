package com.bitalk.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bitalk.android.R
import com.bitalk.android.ui.theme.BitalkAccent

@Composable
fun UsernameEditModal(
        currentUsername: String,
        currentDescription: String,
        onUserInfoChanged: (String, String) -> Unit,
        onDismiss: () -> Unit
) {
    var username by remember { mutableStateOf(currentUsername) }
    var description by remember { mutableStateOf(currentDescription) }
    val isUsernameValid =
            username.isNotBlank() &&
                    username.length in 3..20 &&
                    username.matches(Regex("^[a-zA-Z0-9_]+$"))

    val isDescriptionValid = description.length <= 100
    val isValid = isUsernameValid && isDescriptionValid

    Dialog(onDismissRequest = onDismiss) {
        Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                // Header
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                            text = stringResource(R.string.change_your_info),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = BitalkAccent
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Username input
                OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(stringResource(R.string.username)) },
                        placeholder = { Text(stringResource(R.string.username_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions =
                                KeyboardOptions(capitalization = KeyboardCapitalization.None),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp),
                        isError = username.isNotBlank() && !isUsernameValid,
                        supportingText = {
                            if (username.isNotBlank() && !isUsernameValid) {
                                Text(
                                        text = stringResource(R.string.username_validation_error),
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 16.sp
                                )
                            }
                        },
                        colors =
                                OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BitalkAccent,
                                        focusedLabelColor = BitalkAccent
                                )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Description input
                OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(R.string.description)) },
                        placeholder = { Text(stringResource(R.string.description_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions =
                                KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp),
                        maxLines = 3,
                        colors =
                                OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BitalkAccent,
                                        focusedLabelColor = BitalkAccent
                                )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Character count for description
                Text(
                        text = "${description.length}/100",
                        fontSize = 16.sp,
                        color =
                                if (description.length > 100) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                },
                        modifier = Modifier.align(Alignment.End)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.cancel), fontSize = 18.sp)
                    }

                    Button(
                            onClick = {
                                onUserInfoChanged(username, description)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            enabled = isValid,
                            colors = ButtonDefaults.buttonColors(containerColor = BitalkAccent)
                    ) { Text(text = stringResource(R.string.save), fontSize = 18.sp) }
                }
            }
        }
    }
}
