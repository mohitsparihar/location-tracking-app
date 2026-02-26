package com.example.locationtracker.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.locationtracker.AppContainer
import com.example.locationtracker.data.local.LocationEntity
import com.example.locationtracker.location.LocationServiceController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// Shared color palette
private val Purple = Color(0xFF6B21F5)
private val LightBackground = Color(0xFFF2F2F7)
private val PlaceholderBg = Color(0xFFEDE8FF)
private val TextGray = Color(0xFF6B7280)
private val IconGray = Color(0xFF9CA3AF)
private val DarkText = Color(0xFF1A1A2E)

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

    DisposableEffect(lifecycleOwner, state.isSignedIn) {
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

    Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
        when {
            state.checkingAuth -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            !state.isSignedIn -> {
                LoginScreen(
                    loading = state.loginInProgress,
                    error = state.authError,
                    onLogin = vm::login,
                    paddingValues = paddingValues
                )
            }

            else -> {
                LocationsScreen(
                    locations = state.locations,
                    userEmail = state.userEmail,
                    onLogout = vm::logout,
                    paddingValues = paddingValues
                )
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
    onLogin: (String, String) -> Unit,
    paddingValues: PaddingValues = PaddingValues()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // GeoIQ logo at top
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(
                        id = com.example.locationtracker.R.drawable.ic_geoiq_logo
                    ),
                    contentDescription = "GeoIQ Logo",
                    modifier = Modifier
                        .height(36.dp)
                        .fillMaxWidth(0.5f),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Location Tracker",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
                Spacer(modifier = Modifier.height(4.dp))
               
                Spacer(modifier = Modifier.height(20.dp))

                // Email field
                Text(
                    text = "Username or Email",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkText,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("name@company.com", color = IconGray) },
                    leadingIcon = {
                        Icon(Icons.Filled.Email, contentDescription = null, tint = IconGray)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Password field
                Text(
                    text = "Password",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkText,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("••••••••••", color = IconGray) },
                    leadingIcon = {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = IconGray)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = IconGray
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { onLogin(email, password) },
                    enabled = !loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Purple)
                ) {
                    Text(
                        text = if (loading) "Signing in..." else "Log In",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // TextButton(onClick = { }) {
                //     Text(
                //         text = buildAnnotatedString {
                //             withStyle(SpanStyle(color = TextGray)) {
                //                 append("Don't have an account? ")
                //             }
                //             withStyle(SpanStyle(color = Purple, fontWeight = FontWeight.Bold)) {
                //                 append("Sign up for free")
                //             }
                //         },
                //         style = MaterialTheme.typography.bodySmall
                //     )
                // }
            }
        }
    }
}

@Composable
private fun LocationsScreen(
    locations: List<LocationEntity>,
    userEmail: String?,
    onLogout: () -> Unit,
    paddingValues: PaddingValues = PaddingValues()
) {
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        SettingsScreen(
            userEmail = userEmail,
            onBack = { showSettings = false },
            onLogout = onLogout,
            paddingValues = paddingValues
        )
        return
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.US) }
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("All Records", "Synced", "Offline")

    val filteredLocations = remember(locations, selectedTab) {
        when (selectedTab) {
            1 -> locations.filter { it.uploaded }
            2 -> locations.filter { !it.uploaded }
            else -> locations
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(paddingValues)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = Purple,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Location History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = DarkText,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showSettings = true }) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = DarkText,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Tab row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = Purple
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTab == index) Purple else IconGray,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        if (filteredLocations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No locations recorded yet.",
                    color = IconGray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                items(filteredLocations, key = { it.id }) { item ->
                    LocationCard(item = item, dateFormat = dateFormat)
                }
            }
        }
    }
}

@Composable
private fun LocationCard(item: LocationEntity, dateFormat: SimpleDateFormat) {
    val latDir = if (item.latitude >= 0) "N" else "S"
    val lngDir = if (item.longitude >= 0) "E" else "W"
    val coordText = "%.4f° %s, %.4f° %s".format(
        abs(item.latitude), latDir,
        abs(item.longitude), lngDir
    )

    val syncedGreen = Color(0xFF22C55E)
    val syncedBg = Color(0xFFDCFCE7)
    val pendingOrange = Color(0xFFF59E0B)
    val pendingBg = Color(0xFFFEF3C7)
    val lightPurpleBg = Color(0xFFEDE8FF)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon box
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(lightPurpleBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.uploaded) Icons.Filled.GpsFixed else Icons.Filled.Navigation,
                    contentDescription = null,
                    tint = Purple,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = coordText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CalendarToday,
                        contentDescription = null,
                        tint = IconGray,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = dateFormat.format(Date(item.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Straighten,
                        contentDescription = null,
                        tint = IconGray,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Accuracy: ${item.accuracy?.let { "${it.toInt()}m" } ?: "N/A"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Status badge
            val badgeColor = if (item.uploaded) syncedGreen else pendingOrange
            val badgeBg = if (item.uploaded) syncedBg else pendingBg
            val badgeText = if (item.uploaded) "SYNCED" else "PENDING"
            val badgeIcon = if (item.uploaded) Icons.Filled.CheckCircle else Icons.Filled.Schedule

            Row(
                modifier = Modifier
                    .background(badgeBg, RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }
        }
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

// ── Settings ─────────────────────────────────────────────────────────────────

private val SlateIcon = Color(0xFF505870)
private val LogoutRed = Color(0xFFEA3943)
private val LogoutBg = Color(0xFFFFECED)
private val EmailCardBg = Color(0xFFF0F2F8)

@Composable
private fun SettingsScreen(
    userEmail: String?,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    paddingValues: PaddingValues = PaddingValues()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(paddingValues)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = DarkText
                )
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DarkText,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        androidx.compose.material3.HorizontalDivider()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Email card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EmailCardBg, RoundedCornerShape(14.dp))
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userEmail ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )
            }

            // Menu section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(14.dp))
            ) {
                SettingsMenuItem(
                    icon = { Icon(Icons.Filled.HelpOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) },
                    title = "Help Center"
                )
                androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(start = 68.dp))
                SettingsMenuItem(
                    icon = { Icon(Icons.Filled.Article, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) },
                    title = "Terms of Service"
                )
            }

            // Log out button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LogoutBg, RoundedCornerShape(14.dp))
                    .clickable { onLogout() }
                    .padding(vertical = 18.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.ExitToApp,
                    contentDescription = null,
                    tint = LogoutRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Log out",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = LogoutRed
                )
            }
        }
    }
}

@Composable
private fun SettingsMenuItem(
    icon: @Composable () -> Unit,
    title: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(SlateIcon, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = DarkText,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = IconGray,
            modifier = Modifier.size(20.dp)
        )
    }
}
