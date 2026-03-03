# Fix for Compose LocalLifecycleOwner not present crash in Release builds
# This is a known issue with R8 aggressive optimization on Compose lifecycle classes

# Keep all lifecycle-related Compose classes
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class androidx.lifecycle.** { *; }

# Keep Compose runtime internals needed for CompositionLocal
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class androidx.compose.runtime.** { *; }

# Keep Compose UI platform classes
-keep class androidx.compose.ui.platform.** { *; }
-keepclassmembers class androidx.compose.ui.platform.** { *; }

# Prevent R8 from stripping CompositionLocal providers
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Keep Network Models for Gson serialization/deserialization
-keep class com.geoiq.trackiq.data.network.** { *; }

# Keep the Application and Activity classes
-keep class com.geoiq.trackiq.** { *; }
