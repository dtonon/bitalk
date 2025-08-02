package com.bitalk.android.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bitalk.android.R
import com.bitalk.android.ui.theme.BitalkAccent
import com.bitalk.android.ui.viewmodel.OnboardingViewModel

@Composable
fun OnboardingDescriptionScreen(
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val viewModel: OnboardingViewModel = viewModel()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
        // Auto-focus the input field when screen loads
        focusRequester.requestFocus()
    }
    
    val uiState by viewModel.uiState.collectAsState()
    var description by remember { mutableStateOf(uiState.description) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Top bar with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Title
        Text(
            text = stringResource(R.string.onboarding_description_title),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = BitalkAccent
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Subtitle
        Text(
            text = stringResource(R.string.onboarding_description_subtitle),
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            lineHeight = 20.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Description input
        OutlinedTextField(
            value = description,
            onValueChange = { 
                description = it
                viewModel.updateDescription(it)
            },
            label = { Text("Description") },
            placeholder = { Text("e.g., yellow shirt and sunglasses") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences
            ),
            maxLines = 3,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BitalkAccent,
                focusedLabelColor = BitalkAccent
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Character count
        Text(
            text = "${description.length}/100",
            fontSize = 16.sp,
            color = if (description.length > 100) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            },
            modifier = Modifier.align(Alignment.End)
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Next button
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = description.isNotBlank() && description.length <= 100,
            colors = ButtonDefaults.buttonColors(
                containerColor = BitalkAccent
            )
        ) {
            Text(
                text = stringResource(R.string.next),
                fontSize = 21.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}