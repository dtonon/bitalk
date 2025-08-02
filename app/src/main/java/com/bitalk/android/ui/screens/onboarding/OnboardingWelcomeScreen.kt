package com.bitalk.android.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bitalk.android.R
import com.bitalk.android.ui.theme.BitalkAccent

@Composable
fun OnboardingWelcomeScreen(
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App icon/logo placeholder
        Box(
            modifier = Modifier
                .size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🗨️",
                fontSize = 83.sp
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Welcome title
        Text(
            text = stringResource(R.string.onboarding_welcome),
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = BitalkAccent,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Welcome description
        Text(
            text = "Discover like-minded people nearby using Bluetooth mesh networking",
            fontSize = 21.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            lineHeight = 22.sp
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Next button
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
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