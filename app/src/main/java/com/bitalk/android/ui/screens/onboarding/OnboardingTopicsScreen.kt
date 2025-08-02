package com.bitalk.android.ui.screens.onboarding

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bitalk.android.R
import com.bitalk.android.model.DefaultTopics
import com.bitalk.android.ui.theme.BitalkAccent
import com.bitalk.android.ui.viewmodel.OnboardingViewModel
// FlowRow is now part of Compose Material3
// import com.google.accompanist.flowlayout.FlowRow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingTopicsScreen(
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val viewModel: OnboardingViewModel = viewModel()
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }
    
    val uiState by viewModel.uiState.collectAsState()
    
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
            text = stringResource(R.string.onboarding_topics_title),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = BitalkAccent
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Subtitle
        Text(
            text = stringResource(R.string.onboarding_topics_subtitle),
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            lineHeight = 20.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
                
        // Topics grid
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DefaultTopics.topics.forEach { topic ->
                        TopicChip(
                            topic = topic,
                            isSelected = uiState.selectedTopics.contains(topic),
                            onToggle = { viewModel.toggleTopic(topic) }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Next button
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = uiState.selectedTopics.isNotEmpty(),
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

@Composable
fun TopicChip(
    topic: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) BitalkAccent.copy(alpha = 0.1f) else Color.Transparent,
        modifier = Modifier
            .clickable { onToggle() }
            .border(
                width = 1.dp,
                color = if (isSelected) BitalkAccent else Color.Gray,
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Text(
            text = "$topic",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontSize = 18.sp,
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Gray
        )
    }
}