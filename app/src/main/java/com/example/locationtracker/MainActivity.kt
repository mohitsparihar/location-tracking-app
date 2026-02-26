package com.example.locationtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.example.locationtracker.ui.LocationTrackerAppRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // On API 35+ the status bar is always transparent (edge-to-edge enforced).
        // The status bar shows whatever the app background is (white/light gray).
        // isAppearanceLightStatusBars = true → DARK icons, which are visible on light backgrounds.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true   // dark icons → visible on white/light bg
            isAppearanceLightNavigationBars = true
        }

        val container = (application as LocationTrackerApp).container
        setContent {
            LocationTrackerAppRoot(container = container)
        }
    }
}
