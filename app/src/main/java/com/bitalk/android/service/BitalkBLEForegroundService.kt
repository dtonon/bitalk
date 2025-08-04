package com.bitalk.android.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bitalk.android.MainActivity
import com.bitalk.android.R
import com.bitalk.android.ble.BitalkBLEService
import com.bitalk.android.ble.BitalkBLEDelegate
import com.bitalk.android.data.UserManager
import com.bitalk.android.model.NearbyUser
import com.bitalk.android.model.UserProfile

/**
 * Foreground service for continuous BLE scanning in background
 * Ensures reliable scanning when app is not visible
 */
class BitalkBLEForegroundService : Service(), BitalkBLEDelegate {
    
    companion object {
        private const val TAG = "BitalkBLEForegroundService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "bitalk_scanning_channel"
        private const val MATCH_NOTIFICATION_CHANNEL_ID = "bitalk_matches_channel"
        private const val WAKE_LOCK_TAG = "Bitalk::BLEScanning"
        
        const val ACTION_START_SCANNING = "com.bitalk.android.START_SCANNING"
        const val ACTION_STOP_SCANNING = "com.bitalk.android.STOP_SCANNING"
    }
    
    // Service components
    private val binder = BitalkBinder()
    private var bleService: BitalkBLEService? = null
    private var userManager: UserManager? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceRunning = false
    
    // Notification managers
    private lateinit var notificationManager: NotificationManager
    
    inner class BitalkBinder : Binder() {
        fun getService(): BitalkBLEForegroundService = this@BitalkBLEForegroundService
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        userManager = UserManager(this)
        
        // Create notification channels
        createNotificationChannels()
        
        // Acquire wake lock for reliable background operation
        acquireWakeLock()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started with action: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_SCANNING -> {
                startForegroundImmediately()
                // Add a small delay to allow MainActivity to bind first before starting BLE
                Handler(Looper.getMainLooper()).postDelayed({
                    startBLEScanning()
                }, 1000) // 1 second delay
            }
            ACTION_STOP_SCANNING -> {
                stopForegroundScanning()
            }
            else -> {
                // Default action - start scanning if user profile exists
                val userProfile = userManager?.getUserProfile()
                if (userProfile != null) {
                    startForegroundImmediately()
                    // Add delay for background starts too
                    Handler(Looper.getMainLooper()).postDelayed({
                        startBLEScanning()
                    }, 1000)
                } else {
                    Log.w(TAG, "No user profile found, cannot start scanning")
                    stopSelf()
                }
            }
        }
        
        // Restart service if killed by system
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        stopForegroundScanning()
        releaseWakeLock()
        super.onDestroy()
    }
    
    /**
     * Start foreground service immediately with notification
     */
    private fun startForegroundImmediately() {
        val userProfile = userManager?.getUserProfile()
        if (userProfile == null) {
            Log.e(TAG, "Cannot start foreground service without user profile")
            stopSelf()
            return
        }
        
        Log.i(TAG, "Starting foreground service for user: ${userProfile.username}")
        
        // Create persistent notification and start foreground immediately
        val notification = createScanningNotification(userProfile)
        startForeground(NOTIFICATION_ID, notification)
    }
    
    /**
     * Start BLE scanning (called with delay after foreground service is established)
     */
    private fun startBLEScanning() {
        if (isServiceRunning) {
            Log.d(TAG, "BLE scanning already running")
            return
        }
        
        val userProfile = userManager?.getUserProfile()
        if (userProfile == null) {
            Log.e(TAG, "Cannot start BLE scanning without user profile")
            stopSelf()
            return
        }
        
        Log.i(TAG, "Starting BLE scanning for user: ${userProfile.username}")
        
        // Initialize and start BLE service
        bleService = BitalkBLEService(this).apply {
            delegate = this@BitalkBLEForegroundService
            if (startServices(userProfile)) {
                isServiceRunning = true
                Log.i(TAG, "BLE scanning started successfully")
                
                // Start background scanning monitoring
                startBackgroundScanningMonitor()
            } else {
                Log.e(TAG, "Failed to start BLE scanning")
                stopSelf()
            }
        }
    }
    
    /**
     * Start foreground scanning with persistent notification (legacy method for compatibility)
     */
    private fun startForegroundScanning() {
        startForegroundImmediately()
        startBLEScanning()
    }
    
    /**
     * Stop foreground scanning
     */
    private fun stopForegroundScanning() {
        if (!isServiceRunning) return
        
        Log.i(TAG, "Stopping foreground BLE scanning")
        
        bleService?.stopServices()
        bleService = null
        isServiceRunning = false
        
        stopForeground(true)
    }
    
    /**
     * Get nearby users from BLE service
     */
    fun getNearbyUsers(): List<NearbyUser> {
        return bleService?.getNearbyUsers() ?: emptyList()
    }
    
    /**
     * Toggle BLE scanning on/off (keeps foreground service running)
     */
    fun toggleBLEScanning(): Boolean {
        return bleService?.let { service ->
            if (service.isActive()) {
                Log.i(TAG, "Stopping BLE scanning (keeping foreground service)")
                service.stopServices()
                false
            } else {
                Log.i(TAG, "Starting BLE scanning")
                val userProfile = userManager?.getUserProfile()
                if (userProfile != null) {
                    service.startServices(userProfile)
                    true
                } else {
                    Log.e(TAG, "Cannot start BLE - no user profile")
                    false
                }
            }
        } ?: false
    }
    
    /**
     * Check if BLE scanning is active
     */
    fun isBLEScanning(): Boolean {
        return bleService?.isActive() ?: false
    }
    
    /**
     * Update user profile and restart scanning
     */
    fun updateUserProfile(profile: UserProfile) {
        userManager?.saveUserProfile(profile)
        bleService?.updateUserProfile(profile)
        
        // Update notification with new user info
        if (isServiceRunning) {
            val notification = createScanningNotification(profile)
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }
    
    /**
     * Create notification channels for Android O+
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel for persistent scanning notification
            val scanningChannel = NotificationChannel(
                CHANNEL_ID,
                "Background Scanning",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when Bitalk is scanning for nearby users"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            
            // Channel for match notifications
            val matchChannel = NotificationChannel(
                MATCH_NOTIFICATION_CHANNEL_ID,
                "User Matches",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when interesting people are found nearby"
                enableVibration(true)
            }
            
            notificationManager.createNotificationChannel(scanningChannel)
            notificationManager.createNotificationChannel(matchChannel)
        }
    }
    
    /**
     * Create persistent notification for scanning
     */
    private fun createScanningNotification(userProfile: UserProfile): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = Intent(this, BitalkBLEForegroundService::class.java).apply {
            action = ACTION_STOP_SCANNING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Bitalk Active")
            .setContentText("Scanning for people with topics: ${userProfile.topics.take(3).joinToString(", ")}${if (userProfile.topics.size > 3) "..." else ""}")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
    
    /**
     * Show notification when a matching user is found
     */
    private fun showMatchNotification(user: NearbyUser) {
        try {
            Log.d(TAG, "Creating match notification for user: ${user.username}")
            
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                this, user.username.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notificationId = "match_${user.username}".hashCode()
            val title = "Found: ${user.username}"
            val text = "Matching topics: ${user.matchingTopics.joinToString(", ")} • ${user.formattedDistance}"
            
            Log.d(TAG, "Notification details - ID: $notificationId, Title: $title, Text: $text")
            
            val notification = NotificationCompat.Builder(this, MATCH_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setDefaults(NotificationCompat.DEFAULT_ALL) // Add sound, vibration
                .build()
            
            Log.d(TAG, "Publishing notification with ID: $notificationId")
            notificationManager.notify(notificationId, notification)
            Log.i(TAG, "MATCH NOTIFICATION PUBLISHED: ${user.username}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing match notification: ${e.message}", e)
        }
    }
    
    /**
     * Acquire wake lock for reliable background operation
     */
    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                WAKE_LOCK_TAG
            ).apply {
                acquire(30 * 60 * 1000L) // 30 minutes timeout - longer for background scanning
            }
            Log.d(TAG, "Wake lock acquired for 30 minutes")
            
            // Schedule wake lock renewal
            scheduleWakeLockRenewal()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock: ${e.message}")
        }
    }
    
    /**
     * Schedule periodic wake lock renewal
     */
    private fun scheduleWakeLockRenewal() {
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                if (isServiceRunning && wakeLock?.isHeld == true) {
                    Log.d(TAG, "Renewing wake lock")
                    wakeLock?.acquire(30 * 60 * 1000L) // Renew for another 30 minutes
                    scheduleWakeLockRenewal() // Schedule next renewal
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error renewing wake lock: ${e.message}")
            }
        }, 25 * 60 * 1000L) // Renew after 25 minutes (before 30min timeout)
    }
    
    /**
     * Monitor background scanning effectiveness
     */
    private fun startBackgroundScanningMonitor() {
        var lastDiscoveryTime = System.currentTimeMillis()
        var discoveryCount = 0
        
        Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
            override fun run() {
                if (!isServiceRunning) return
                
                val currentTime = System.currentTimeMillis()
                val timeSinceLastDiscovery = currentTime - lastDiscoveryTime
                
                Log.i(TAG, "*** BACKGROUND SCAN MONITOR ***")
                Log.i(TAG, "Discoveries in last 5 minutes: $discoveryCount")
                Log.i(TAG, "Time since last discovery: ${timeSinceLastDiscovery / 1000}s")
                Log.i(TAG, "Wake lock held: ${wakeLock?.isHeld}")
                Log.i(TAG, "Service running: $isServiceRunning")
                
                // Reset counter
                discoveryCount = 0
                
                // Schedule next check
                Handler(Looper.getMainLooper()).postDelayed(this, 5 * 60 * 1000L) // Every 5 minutes
            }
        }, 5 * 60 * 1000L) // First check after 5 minutes
    }
    
    /**
     * Release wake lock
     */
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "Wake lock released")
            }
        }
        wakeLock = null
    }
    
    // BLE Service Delegate Methods
    override fun onUserDiscovered(user: NearbyUser) {
        Log.i(TAG, "*** NEW USER DISCOVERED: ${user.username} with topics: ${user.matchingTopics} ***")
        Log.i(TAG, "Showing match notification for user: ${user.username}")
        showMatchNotification(user)
        Log.i(TAG, "Match notification shown for user: ${user.username}")
    }
    
    override fun onUserUpdated(user: NearbyUser) {
        Log.d(TAG, "User updated: ${user.username}")
        // Don't show notification for updates to avoid spam
    }
    
    override fun onUserLost(user: NearbyUser) {
        Log.d(TAG, "User lost: ${user.username}")
        // Cancel match notification when user is no longer nearby
        notificationManager.cancel("match_${user.username}".hashCode())
    }
}