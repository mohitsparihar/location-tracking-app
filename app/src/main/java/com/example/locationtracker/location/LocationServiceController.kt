package com.example.locationtracker.location

import android.content.Context
import android.content.Intent
import android.os.Build

object LocationServiceController {
    fun start(context: Context) {
        val intent = Intent(context, LocationCaptureService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, LocationCaptureService::class.java))
    }
}
