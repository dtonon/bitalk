package com.bitalk.android.ui.screens.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bitalk.android.R
import com.bitalk.android.ui.theme.BitalkAccent
import com.bitalk.android.ui.viewmodel.OnboardingViewModel
import com.google.accompanist.permissions.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OnboardingPermissionsScreen(
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    val viewModel: OnboardingViewModel = viewModel()
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }
    
    // Required permissions
    val permissions = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
            } else {
                add(Manifest.permission.BLUETOOTH)
                add(Manifest.permission.BLUETOOTH_ADMIN)
            }
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    
    val multiplePermissionsState = rememberMultiplePermissionsState(permissions)
    
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
            text = stringResource(R.string.onboarding_permissions_title),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = BitalkAccent
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Subtitle
        Text(
            text = stringResource(R.string.onboarding_permissions_subtitle),
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            lineHeight = 20.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Permissions list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PermissionItem(
                    title = stringResource(R.string.permission_bluetooth),
                    description = stringResource(R.string.permission_bluetooth_desc),
                    isGranted = multiplePermissionsState.permissions.filter { 
                        it.permission.contains("BLUETOOTH") 
                    }.all { it.status == PermissionStatus.Granted }
                )
            }
            
            item {
                PermissionItem(
                    title = stringResource(R.string.permission_location),
                    description = stringResource(R.string.permission_location_desc),
                    isGranted = multiplePermissionsState.permissions.filter { 
                        it.permission.contains("LOCATION") 
                    }.all { it.status == PermissionStatus.Granted }
                )
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                item {
                    PermissionItem(
                        title = stringResource(R.string.permission_notifications),
                        description = stringResource(R.string.permission_notifications_desc),
                        isGranted = multiplePermissionsState.permissions.any { 
                            it.permission == Manifest.permission.POST_NOTIFICATIONS && 
                            it.status == PermissionStatus.Granted 
                        }
                    )
                }
            }
            
            item {
                PermissionItem(
                    title = stringResource(R.string.permission_battery),
                    description = stringResource(R.string.permission_battery_desc),
                    isGranted = true // We'll handle this separately
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Action buttons
        if (multiplePermissionsState.allPermissionsGranted) {
            Button(
                onClick = {
                    viewModel.completeOnboarding()
                    onComplete()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BitalkAccent
                )
            ) {
                Text(
                    text = stringResource(R.string.done),
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Column {
                Button(
                    onClick = {
                        multiplePermissionsState.launchMultiplePermissionRequest()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BitalkAccent
                    )
                ) {
                    Text(
                        text = "Grant Permissions",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = {
                        // Open app settings
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Settings")
                }
            }
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) {
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            } else {
                Color(0xFFFF9800).copy(alpha = 0.1f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Filled.Check else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF4CAF50) else Color(0xFFFF9800),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = description,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}