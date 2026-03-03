package com.example.locationtracker.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.locationtracker.LocationTrackerApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync() // Keep receiver alive for async work
        val app = context.applicationContext as LocationTrackerApp

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // COMPLIANCE ADDED: Restart service on device boot if user is signed in
                val signedIn = app.container.authRepository.isSignedIn.firstOrNull() == true
                if (signedIn) {
                    LocationServiceController.start(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
