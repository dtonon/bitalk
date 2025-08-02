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
        
        // Scan and advertising settings
        private const val SCAN_PERIOD_MS = 10000L // 10 seconds
        private const val SCAN_INTERVAL_MS = 30000L // 30 seconds between scans
        private const val ADVERTISE_TIMEOUT_MS = 0 // Continuous advertising
        private const val USER_TIMEOUT_MS = 45000L // 45 seconds - remove users not seen recently
        private const val CLEANUP_INTERVAL_MS = 15000L // 15 seconds - cleanup stale users
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
        gattServer?.close()
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
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
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
     * Start periodic scanning
     */
    private fun startPeriodicScanning() {
        serviceScope.launch {
            while (isActive) {
                startScan()
                delay(SCAN_PERIOD_MS)
                stopScan()
                delay(SCAN_INTERVAL_MS - SCAN_PERIOD_MS)
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
     * Start BLE scan
     */
    private fun startScan() {
        try {
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            
            val filters = listOf(
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(BITALK_SERVICE_UUID))
                    .build()
            )
            
            bluetoothLeScanner?.startScan(filters, settings, scanCallback)
            Log.d(TAG, "Started BLE scan")
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied starting scan: ${e.message}")
        }
    }
    
    /**
     * Stop BLE scan
     */
    private fun stopScan() {
        try {
            bluetoothLeScanner?.stopScan(scanCallback)
            Log.d(TAG, "Stopped BLE scan")
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
    
    // Scan callback
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let { handleScanResult(it) }
        }
        
        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error: $errorCode")
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