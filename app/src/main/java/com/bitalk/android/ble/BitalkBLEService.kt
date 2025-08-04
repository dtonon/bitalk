package com.bitalk.android.ble

import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.bitalk.android.model.TopicBroadcast
import com.bitalk.android.model.NearbyUser
import com.bitalk.android.model.UserProfile
import com.bitalk.android.util.DistanceCalculator
import com.bitalk.android.matching.TopicMatcher
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.HashMap

/**
 * Simplified BLE service for Bitalk topic broadcasting and discovery
 * No encryption - focuses on discoverability and simplicity
 */
class BitalkBLEService(private val context: Context) {
    
    companion object {
        private const val TAG = "BitalkBLEService"
        
        // Custom service UUID for Bitalk
        private val BITALK_SERVICE_UUID = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")
        private val BITALK_CHARACTERISTIC_UUID = UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8")
        
        // Scan and advertising settings - aggressive for background discovery
        private const val SCAN_PERIOD_MS = 10000L // 10 seconds - shorter bursts
        private const val SCAN_INTERVAL_MS = 5000L // 5 seconds between scans - more frequent
        private const val ADVERTISE_TIMEOUT_MS = 0 // Continuous advertising
        private const val USER_TIMEOUT_MS = 60000L // 60 seconds - shorter timeout for responsiveness
        private const val CLEANUP_INTERVAL_MS = 15000L // 15 seconds - more frequent cleanup
        private const val SCAN_RESTART_INTERVAL_MS = 300000L // 5 minutes - less disruptive restarts
    }
    
    // Bluetooth components
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    private val bluetoothLeAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser
    
    // Permission manager
    private val permissionManager = BluetoothPermissionManager(context)
    
    // Service state
    private var isActive = false
    private var userProfile: UserProfile? = null
    private val nearbyUsers = HashMap<String, NearbyUser>() // Key: username (not device address)
    private val rssiFilters = HashMap<String, DistanceCalculator.RSSIFilter>() // Key: device address
    private val deviceToUsername = HashMap<String, String>() // Map device addresses to usernames
    
    // Coroutines
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // GATT Server for characteristic broadcasting
    private var gattServer: BluetoothGattServer? = null
    private var dataCharacteristic: BluetoothGattCharacteristic? = null
    
    // Delegate for callbacks
    var delegate: BitalkBLEDelegate? = null
    
    /**
     * Start BLE services - advertising and scanning
     */
    fun startServices(profile: UserProfile): Boolean {
        if (!permissionManager.hasBluetoothPermissions()) {
            Log.e(TAG, "Missing Bluetooth permissions")
            return false
        }
        
        if (bluetoothAdapter?.isEnabled != true) {
            Log.e(TAG, "Bluetooth not enabled")
            return false
        }
        
        userProfile = profile
        isActive = true
        
        Log.i(TAG, "Starting Bitalk BLE services for user: ${profile.username} with topics: ${profile.topics}")
        
        // Start GATT server for data exchange
        startGattServer()
        
        // Start advertising our presence
        startAdvertising()
        
        // Start scanning for others
        startPeriodicScanning()
        
        // Start periodic cleanup of stale users
        startPeriodicCleanup()
        
        // Start periodic scan restarts for background reliability
        startPeriodicScanRestarts()
        
        return true
    }
    
    /**
     * Stop all BLE services
     */
    fun stopServices() {
        Log.i(TAG, "Stopping Bitalk BLE services")
        isActive = false
        
        // Stop advertising
        try {
            bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied stopping advertising: ${e.message}")
        }
        
        // Stop scanning
        try {
            bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied stopping scan: ${e.message}")
        }
        
        // Stop GATT server
        try {
            gattServer?.close()
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied closing GATT server: ${e.message}")
        }
        gattServer = null
        
        // Clear data
        nearbyUsers.clear()
        rssiFilters.clear()
        deviceToUsername.clear()
        
        serviceScope.cancel()
    }
    
    /**
     * Update user profile and restart advertising
     */
    fun updateUserProfile(profile: UserProfile) {
        userProfile = profile
        if (isActive) {
            // Restart advertising with new data
            try {
                bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
                startAdvertising()
            } catch (e: SecurityException) {
                Log.w(TAG, "Permission denied updating advertising: ${e.message}")
            }
        }
    }
    
    /**
     * Get list of nearby users
     */
    fun getNearbyUsers(): List<NearbyUser> {
        return nearbyUsers.values.filter { it.isActive() }
    }
    
    /**
     * Check if BLE services are active
     */
    fun isActive(): Boolean {
        return isActive
    }
    
    /**
     * Start GATT server for data exchange
     */
    private fun startGattServer() {
        try {
            val gattServerCallback = object : BluetoothGattServerCallback() {
                override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
                    Log.d(TAG, "GATT connection state changed: $newState for ${device?.address}")
                }
                
                override fun onCharacteristicReadRequest(
                    device: BluetoothDevice?,
                    requestId: Int,
                    offset: Int,
                    characteristic: BluetoothGattCharacteristic?
                ) {
                    Log.d(TAG, "GATT read request from ${device?.address} for characteristic ${characteristic?.uuid}, offset=$offset")
                    if (characteristic?.uuid == BITALK_CHARACTERISTIC_UUID) {
                        val broadcast = userProfile?.let { 
                            TopicBroadcast(it.username, it.description, it.topics)
                        }
                        val allData = broadcast?.toByteArray() ?: ByteArray(0)
                        
                        Log.d(TAG, "Full broadcast data size: ${allData.size} bytes for ${broadcast?.username}")
                        
                        // Handle offset for large data
                        val responseData = if (offset >= allData.size) {
                            Log.w(TAG, "Offset $offset >= data size ${allData.size}, sending empty response")
                            ByteArray(0)
                        } else {
                            val remainingSize = allData.size - offset
                            val chunkSize = minOf(remainingSize, 512) // BLE characteristic max size
                            allData.copyOfRange(offset, offset + chunkSize)
                        }
                        
                        Log.d(TAG, "Sending ${responseData.size} bytes to ${device?.address} (offset=$offset)")
                        
                        try {
                            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, responseData)
                        } catch (e: SecurityException) {
                            Log.w(TAG, "Permission denied sending GATT response: ${e.message}")
                        }
                    }
                }
            }
            
            gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
            
            // Create characteristic for data exchange
            dataCharacteristic = BluetoothGattCharacteristic(
                BITALK_CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
            )
            
            // Create service and add characteristic
            val service = BluetoothGattService(BITALK_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
            service.addCharacteristic(dataCharacteristic)
            
            gattServer?.addService(service)
            
            Log.d(TAG, "GATT server started")
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied starting GATT server: ${e.message}")
        }
    }
    
    /**
     * Start BLE advertising
     */
    private fun startAdvertising() {
        val currentProfile = userProfile ?: return
        
        try {
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER) // Better for background
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(true)
                .setTimeout(ADVERTISE_TIMEOUT_MS)
                .build()
            
            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(ParcelUuid(BITALK_SERVICE_UUID))
                .build()
            
            bluetoothLeAdvertiser?.startAdvertising(settings, data, advertiseCallback)
            
            Log.d(TAG, "Started advertising for user: ${currentProfile.username}")
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied starting advertising: ${e.message}")
        }
    }
    
    /**
     * Start continuous scanning - no stops to avoid Android throttling
     */
    private fun startPeriodicScanning() {
        serviceScope.launch {
            Log.i(TAG, "*** STARTING CONTINUOUS BACKGROUND SCANNING - NO STOPS ***")
            
            // Start scanning once and never stop (except for restarts)
            if (startScan()) {
                Log.i(TAG, "Continuous scanning started successfully")
            } else {
                Log.e(TAG, "Failed to start continuous scanning")
            }
            
            // Just monitor scanning status
            var checkCount = 0
            while (isActive) {
                delay(10000) // Check every 10 seconds
                Log.i(TAG, "*** SCAN STATUS CHECK ${++checkCount} *** Active: $isActive")
                
                // Log current nearby users count
                val nearbyCount = nearbyUsers.size
                Log.i(TAG, "Current nearby users: $nearbyCount")
                if (nearbyCount > 0) {
                    nearbyUsers.values.forEach { user ->
                        Log.i(TAG, "  - ${user.username}: ${user.formattedDistance}, last seen ${(System.currentTimeMillis() - user.lastSeen)/1000}s ago")
                    }
                }
            }
        }
    }
    
    
    /**
     * Start periodic cleanup of stale users
     */
    private fun startPeriodicCleanup() {
        serviceScope.launch {
            while (isActive) {
                delay(CLEANUP_INTERVAL_MS)
                cleanupStaleUsers()
            }
        }
    }
    
    /**
     * Start periodic scan restarts to combat Android background restrictions
     */
    private fun startPeriodicScanRestarts() {
        serviceScope.launch {
            while (isActive) {
                delay(SCAN_RESTART_INTERVAL_MS)
                Log.i(TAG, "*** PERIODIC SCAN RESTART - Combating background restrictions ***")
                restartScanning()
            }
        }
    }
    
    /**
     * Restart scanning completely - helps with Android background restrictions
     */
    private fun restartScanning() {
        try {
            Log.i(TAG, "Restarting BLE scanning for background reliability")
            
            // Stop current scanning
            try {
                bluetoothLeScanner?.stopScan(scanCallback)
                bluetoothLeScanner?.stopScan(opportunisticScanCallback)
                Log.d(TAG, "Stopped current scans")
            } catch (e: SecurityException) {
                Log.w(TAG, "Permission denied stopping scan for restart: ${e.message}")
            }
            
            // Small delay before restart
            Thread.sleep(1000)
            
            // Start fresh scan
            startScan()
            Log.i(TAG, "BLE scanning restarted successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error restarting scanning: ${e.message}")
        }
    }
    
    /**
     * Remove users that haven't been seen recently
     */
    private fun cleanupStaleUsers() {
        val currentTime = System.currentTimeMillis()
        val staleUsers = nearbyUsers.values.filter { user ->
            !user.isActive(USER_TIMEOUT_MS)
        }
        
        staleUsers.forEach { user ->
            Log.d(TAG, "Removing stale user: ${user.username} (last seen ${(currentTime - user.lastSeen) / 1000}s ago)")
            nearbyUsers.remove(user.username)
            
            // Also clean up device mappings for this user
            deviceToUsername.entries.removeAll { it.value == user.username }
            
            // Notify delegate
            delegate?.onUserLost(user)
        }
        
        if (staleUsers.isNotEmpty()) {
            Log.d(TAG, "Cleaned up ${staleUsers.size} stale users")
        }
    }
    
    /**
     * Start BLE scan with dual-mode settings
     * @return true if scan started successfully, false otherwise
     */
    private fun startScan(): Boolean {
        return try {
            // Always try low-latency first for best performance
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // Fast discovery
                .setReportDelay(0) // Report immediately
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE) // Aggressive matching
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES) // All matches
                .build()
            
            val filters = listOf(
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(BITALK_SERVICE_UUID))
                    .build()
            )
            
            bluetoothLeScanner?.startScan(filters, settings, scanCallback)
            Log.d(TAG, "Started BLE scan with low-latency mode")
            
            // Also start opportunistic scan as fallback for background
            startOpportunisticScanFallback()
            
            true
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied starting scan: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error starting scan: ${e.message}")
            false
        }
    }
    
    /**
     * Start opportunistic scan as fallback for background scanning
     */
    private fun startOpportunisticScanFallback() {
        try {
            val opportunisticSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_OPPORTUNISTIC)
                .setReportDelay(0)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build()
            
            // Use empty filter for opportunistic mode to catch all BLE activity
            bluetoothLeScanner?.startScan(emptyList(), opportunisticSettings, opportunisticScanCallback)
            Log.d(TAG, "Started opportunistic scan fallback")
            
        } catch (e: Exception) {
            Log.w(TAG, "Could not start opportunistic scan fallback: ${e.message}")
        }
    }
    
    /**
     * Stop BLE scan
     */
    private fun stopScan() {
        try {
            bluetoothLeScanner?.stopScan(scanCallback)
            bluetoothLeScanner?.stopScan(opportunisticScanCallback)
            Log.d(TAG, "Stopped BLE scans")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied stopping scan: ${e.message}")
        }
    }
    
    // Advertising callback
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d(TAG, "Advertising started successfully")
        }
        
        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "Advertising failed with error: $errorCode")
        }
    }
    
    // Scan callback with detailed logging
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            Log.i(TAG, "*** PRIMARY SCAN RESULT *** callbackType: $callbackType, device: ${result?.device?.address}, RSSI: ${result?.rssi}")
            result?.let { 
                Log.i(TAG, "Service UUIDs: ${it.scanRecord?.serviceUuids}")
                handleScanResult(it) 
            }
        }
        
        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            Log.i(TAG, "*** PRIMARY BATCH SCAN RESULTS: ${results.size} devices found ***")
            if (results.isNotEmpty()) {
                results.forEach { result ->
                    Log.i(TAG, "Primary batch result: ${result.device.address}, RSSI: ${result.rssi}, serviceUUIDs: ${result.scanRecord?.serviceUuids}")
                    handleScanResult(result)
                }
            } else {
                Log.w(TAG, "Empty primary batch scan results - possible Android throttling")
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "*** PRIMARY SCAN FAILED *** Error code: $errorCode")
            when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> Log.e(TAG, "Scan already started")
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> Log.e(TAG, "App registration failed") 
                SCAN_FAILED_FEATURE_UNSUPPORTED -> Log.e(TAG, "Feature unsupported")
                SCAN_FAILED_INTERNAL_ERROR -> Log.e(TAG, "Internal error")
                else -> Log.e(TAG, "Unknown scan error: $errorCode")
            }
            
            // Try to restart scanning after failure
            serviceScope.launch {
                delay(5000) // Wait 5 seconds
                if (isActive) {
                    Log.i(TAG, "Restarting scan after failure")
                    restartScanning()
                }
            }
        }
    }
    
    // Opportunistic scan callback - filters for our service UUID
    private val opportunisticScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let { scanResult ->
                Log.d(TAG, "*** OPPORTUNISTIC SCAN RESULT *** device: ${scanResult.device.address}, serviceUUIDs: ${scanResult.scanRecord?.serviceUuids}")
                // Only process if it's advertising our service UUID
                val serviceUuids = scanResult.scanRecord?.serviceUuids
                if (serviceUuids?.contains(ParcelUuid(BITALK_SERVICE_UUID)) == true) {
                    Log.i(TAG, "*** OPPORTUNISTIC FOUND BITALK DEVICE: ${scanResult.device.address} ***")
                    handleScanResult(scanResult)
                } else {
                    Log.d(TAG, "Opportunistic scan - not a Bitalk device")
                }
            }
        }
        
        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            Log.i(TAG, "*** OPPORTUNISTIC BATCH RESULTS: ${results.size} devices ***")
            results.forEach { result ->
                val serviceUuids = result.scanRecord?.serviceUuids
                Log.d(TAG, "Opportunistic batch device: ${result.device.address}, serviceUUIDs: $serviceUuids")
                if (serviceUuids?.contains(ParcelUuid(BITALK_SERVICE_UUID)) == true) {
                    Log.i(TAG, "*** OPPORTUNISTIC BATCH FOUND BITALK DEVICE: ${result.device.address} ***")
                    handleScanResult(result)
                }
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "*** OPPORTUNISTIC SCAN FAILED *** Error code: $errorCode")
        }
    }
    
    /**
     * Handle discovered device
     */
    private fun handleScanResult(result: ScanResult) {
        val device = result.device
        val rssi = result.rssi
        val deviceAddress = device.address
        
        Log.d(TAG, "Discovered device: $deviceAddress, RSSI: $rssi")
        
        // Connect to read characteristic data
        serviceScope.launch {
            connectAndReadData(device, rssi)
        }
    }
    
    /**
     * Connect to device and read broadcast data
     */
    private suspend fun connectAndReadData(device: BluetoothDevice, rssi: Int) {
        try {
            val gattCallback = object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.d(TAG, "Connected to ${device.address}")
                        try {
                            gatt?.discoverServices()
                        } catch (e: SecurityException) {
                            Log.w(TAG, "Permission denied discovering services: ${e.message}")
                        }
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.d(TAG, "Disconnected from ${device.address}")
                        gatt?.close()
                    }
                }
                
                override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        val service = gatt?.getService(BITALK_SERVICE_UUID)
                        val characteristic = service?.getCharacteristic(BITALK_CHARACTERISTIC_UUID)
                        
                        characteristic?.let {
                            try {
                                gatt.readCharacteristic(it)
                            } catch (e: SecurityException) {
                                Log.w(TAG, "Permission denied reading characteristic: ${e.message}")
                            }
                        }
                    }
                }
                
                override fun onCharacteristicRead(
                    gatt: BluetoothGatt?,
                    characteristic: BluetoothGattCharacteristic?,
                    status: Int
                ) {
                    if (status == BluetoothGatt.GATT_SUCCESS && characteristic?.uuid == BITALK_CHARACTERISTIC_UUID) {
                        val data = characteristic.value ?: ByteArray(0)
                        Log.d(TAG, "Read ${data.size} bytes from ${device.address}")
                        
                        if (data.isNotEmpty()) {
                            processBroadcastData(device.address, data, rssi)
                        } else {
                            Log.w(TAG, "Received empty data from ${device.address}")
                        }
                    } else {
                        Log.w(TAG, "Failed to read characteristic from ${device.address}, status: $status")
                    }
                    gatt?.disconnect()
                }
            }
            
            device.connectGatt(context, false, gattCallback)
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied connecting to device: ${e.message}")
        }
    }
    
    /**
     * Process received broadcast data
     */
    private fun processBroadcastData(deviceAddress: String, data: ByteArray, rssi: Int) {
        Log.d(TAG, "Processing broadcast data from $deviceAddress, data size: ${data.size}")
        
        val broadcast = TopicBroadcast.fromByteArray(data)
        if (broadcast == null) {
            Log.w(TAG, "Failed to parse broadcast data from $deviceAddress")
            return
        }
        
        Log.d(TAG, "Received broadcast from ${broadcast.username}: topics=${broadcast.topics}")
        
        val currentProfile = userProfile
        if (currentProfile == null) {
            Log.w(TAG, "No user profile set, ignoring broadcast")
            return
        }
        
        Log.d(TAG, "Current user: ${currentProfile.username} with topics: ${currentProfile.topics}")
        
        // Skip our own broadcasts
        if (broadcast.username == currentProfile.username) {
            Log.d(TAG, "Skipping own broadcast from ${broadcast.username}")
            return
        }
        
        // Check for topic matches
        val matchingTopics = TopicMatcher.findMatchingTopics(currentProfile, broadcast)
        Log.d(TAG, "Topic matching result: $matchingTopics (exactMode: ${currentProfile.exactMatchMode})")
        
        if (matchingTopics.isEmpty()) {
            Log.d(TAG, "No matching topics found with ${broadcast.username}")
            return
        }
        
        // Calculate distance
        val filter = rssiFilters.getOrPut(deviceAddress) { DistanceCalculator.RSSIFilter() }
        val distance = filter.getSmoothedDistance(rssi)
        
        // Map device address to username for future cleanup
        deviceToUsername[deviceAddress] = broadcast.username
        
        // Check if we already know this user (by username, not device address)
        val existingUser = nearbyUsers[broadcast.username]
        val isNewUser = existingUser == null
        
        // Create or update nearby user
        val nearbyUser = NearbyUser(
            username = broadcast.username,
            description = broadcast.description,
            topics = broadcast.topics,
            rssi = rssi,
            estimatedDistance = distance,
            matchingTopics = matchingTopics,
            firstSeen = existingUser?.firstSeen ?: System.currentTimeMillis(),
            lastSeen = System.currentTimeMillis(),
            deviceAddress = deviceAddress
        )
        
        // Store by username to prevent duplicates
        nearbyUsers[broadcast.username] = nearbyUser
        
        Log.i(TAG, "*** MATCH FOUND *** User ${broadcast.username} at ${nearbyUser.formattedDistance} with matching topics: $matchingTopics (${if (isNewUser) "NEW" else "UPDATED"})")
        
        // Notify delegate
        if (isNewUser) {
            Log.d(TAG, "Notifying delegate of new user: ${broadcast.username}")
            delegate?.onUserDiscovered(nearbyUser)
        } else {
            Log.d(TAG, "Notifying delegate of updated user: ${broadcast.username}")
            delegate?.onUserUpdated(nearbyUser)
        }
    }
}

/**
 * Delegate interface for BLE service callbacks
 */
interface BitalkBLEDelegate {
    fun onUserDiscovered(user: NearbyUser)
    fun onUserUpdated(user: NearbyUser) 
    fun onUserLost(user: NearbyUser)
}