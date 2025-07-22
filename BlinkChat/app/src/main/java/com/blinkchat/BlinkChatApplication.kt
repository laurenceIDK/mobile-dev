package com.blinkchat

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * BlinkChat Application class
 */
@HiltAndroidApp
class BlinkChatApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize any app-wide configurations here
        // For example, Firebase initialization is automatic with the google-services plugin
        
        // You can add analytics, crash reporting, or other initializations here
    }
}
