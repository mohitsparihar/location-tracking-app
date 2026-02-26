package com.example.locationtracker.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.locationtracker.AppContainer
import com.example.locationtracker.data.local.LocationEntity
import com.example.locationtracker.location.LocationServiceController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LocationTrackerAppRoot(container: AppContainer) {
    val vm: MainViewModel = viewModel(factory = MainViewModelFactory(container))
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var fgGranted by remember { mutableStateOf(hasForegroundPermission(context)) }
    var bgGranted by remember { mutableStateOf(hasBackgroundPermission(context)) }
    var notificationGranted by remember { mutableStateOf(hasNotificationPermission(context)) }

    val foregroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        fgGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        bgGranted = granted
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationGranted = granted
    }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner, state.isSignedIn) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                fgGranted = hasForegroundPermission(context)
                bgGranted = hasBackgroundPermission(context)
                notificationGranted = hasNotificationPermission(context)
            }
            if (event == Lifecycle.Event.ON_START && state.isSignedIn) {
                vm.syncPendingLocations()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(fgGranted, bgGranted) {
        if (!fgGranted) {
            foregroundLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else if (!bgGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else if (!notificationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val allPermissionsGranted = fgGranted &&
        bgGranted &&
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || notificationGranted)

    if (!allPermissionsGranted) {
        PermissionBlockingScreen(
            onRequestForeground = {
                foregroundLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            onRequestBackground = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            },
            onRequestNotification = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        )
        return
    }

    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn) {
            LocationServiceController.start(context)
            vm.syncPendingLocations()
        } else {
            LocationServiceController.stop(context)
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when {
                state.checkingAuth -> {
                    CircularProgressIndicator()
                }

                !state.isSignedIn -> {
                    LoginScreen(
                        loading = state.loginInProgress,
                        error = state.authError,
                        onLogin = vm::login
                    )
                }

                else -> {
                    LocationsScreen(
                        locations = state.locations,
                        onLogout = vm::logout
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionBlockingScreen(
    onRequestForeground: () -> Unit,
    onRequestBackground: () -> Unit,
    onRequestNotification: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Location permission required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text("This app is blocked until foreground and background location access is granted.")
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onRequestForeground, modifier = Modifier.fillMaxWidth()) {
            Text("Grant Foreground Location")
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = onRequestBackground, modifier = Modifier.fillMaxWidth()) {
            Text("Grant Background Location")
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Button(onClick = onRequestNotification, modifier = Modifier.fillMaxWidth()) {
                Text("Grant Notifications")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open App Settings")
        }
    }
}

@Composable
private fun LoginScreen(
    loading: Boolean,
    error: String?,
    onLogin: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
        Text("Sign in", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (error != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = error, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onLogin(email, password) },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "Signing in..." else "Sign In")
        }
    }
}

@Composable
private fun LocationsScreen(
    locations: List<LocationEntity>,
    onLogout: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Recorded Locations", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Button(onClick = onLogout) { Text("Logout") }
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (locations.isEmpty()) {
        Text("No locations recorded yet. Keep the app running to capture points every 10 minutes.")
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(locations, key = { it.id }) { item ->
            LocationRow(item = item, dateFormat = dateFormat)
        }
    }
}

@Composable
private fun LocationRow(item: LocationEntity, dateFormat: SimpleDateFormat) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
    ) {
        Text("Lat: ${item.latitude}, Lng: ${item.longitude}")
        Text("Accuracy: ${item.accuracy?.toString() ?: "N/A"} m")
        Text("Time: ${dateFormat.format(Date(item.timestamp))}")
        Text("Mode: ${if (item.isBackground) "Background" else "Foreground"}")
        Text(if (item.uploaded) "Uploaded" else "Pending upload")
    }
}

private fun hasForegroundPermission(context: android.content.Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val coarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    return fine || coarse
}

private fun hasBackgroundPermission(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun hasNotificationPermission(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}
