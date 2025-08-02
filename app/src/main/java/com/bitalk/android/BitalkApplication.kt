package com.bitalk.android

import android.app.Application
import android.util.Log

/**
 * Bitalk Application class
 */
class BitalkApplication : Application() {
    
    companion object {
        private const val TAG = "BitalkApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Bitalk application starting")
    }
}