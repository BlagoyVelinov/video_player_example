package com.example.video_player_example.ui.player.vlc_player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import org.videolan.libvlc.util.VLCVideoLayout

@Composable
fun VlcPlayerScreen(
    url: String = "rtsp://dev.gradotech.eu:8554/stream",
    onBackPressed: (() -> Unit)? = null
) {
    val context: Context = LocalContext.current
    val vm: VlcPlayerViewModel = viewModel()
    var isLoading by remember { mutableStateOf(true) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var volume by rememberSaveable { mutableFloatStateOf(1f) }
    
    // Track if we've initialized this URL to prevent restart on rotation
    var hasInitialized by rememberSaveable(url) { mutableStateOf(false) }
    
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    
    // Initialize playback only when needed, not on rotation
    LaunchedEffect(url) {
        if (!hasInitialized) {
            Log.d("VlcPlayerScreen", "First initialization for URL: $url")
            vm.playMedia(url)
            hasInitialized = true
            delay(1000)
            isLoading = false
        } else {
            Log.d("VlcPlayerScreen", "Already initialized, skipping playback start for: $url")
            // Just hide loading if already initialized
            isLoading = false
        }
    }
    
    // Apply volume changes
    LaunchedEffect(volume) {
        vm.setVolume(volume)
    }
    
    // Auto-hide controls
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(3000)
            controlsVisible = false
        }
    }
    
    // Fullscreen handling - only manage system UI, not rotation in LaunchedEffect
    LaunchedEffect(isFullscreen, configuration.orientation) {
        delay(100)
        // Only handle system UI visibility, rotation is handled by button click
        val activity = context as? Activity ?: return@LaunchedEffect
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, !isFullscreen)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (isFullscreen) controller.hide(WindowInsetsCompat.Type.systemBars())
        else controller.show(WindowInsetsCompat.Type.systemBars())
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // VLC Video Layout
        AndroidView(
            factory = { ctx ->
                VLCVideoLayout(ctx).apply {
                    // Attach the media player to the video layout
                    vm.mediaPlayer.attachViews(this, null, false, false)
                    
                    // Set click listener to toggle controls
                    setOnClickListener {
                        controlsVisible = !controlsVisible
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Connecting to stream...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        // Player controls overlay
        if (controlsVisible && !isLoading) {
            // Back button (top-left)
            if (onBackPressed != null) {
                IconButton(
                    onClick = {
                        vm.stopPlayback()
                        onBackPressed()
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(16.dp)
                        .background(Color(0x66000000), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
            
            // Center play/pause button
            IconButton(
                onClick = { vm.togglePlayPause() },
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0x66000000), CircleShape)
                    .size(64.dp)
            ) {
                Icon(
                    imageVector = if (vm.isPlaying.value) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (vm.isPlaying.value) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // Bottom controls: volume slider + fullscreen button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(Color(0x00000000))
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .padding(bottom = if (isPortrait) 120.dp else 0.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Volume",
                    tint = Color.White,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Slider(
                    value = volume,
                    onValueChange = { volume = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.width(120.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {
                    isFullscreen = !isFullscreen
                    toggleFullscreenWithRotation(context, isFullscreen, isPortrait)
                }) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isFullscreen) "Exit Fullscreen" else "Enter Fullscreen",
                        tint = Color.White
                    )
                }
            }
        }
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            vm.stopPlayback()
        }
    }
}

private fun toggleFullscreenWithRotation(context: Context, enable: Boolean, isCurrentlyPortrait: Boolean) {
    val activity = context as? Activity ?: return
    
    // Handle screen rotation
    if (enable) {
        // Entering fullscreen - rotate to landscape if currently in portrait
        if (isCurrentlyPortrait) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    } else {
        // Exiting fullscreen - return to portrait
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
    
    // Handle system UI visibility
    val window = activity.window
    WindowCompat.setDecorFitsSystemWindows(window, !enable)
    val controller = WindowInsetsControllerCompat(window, window.decorView)
    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    if (enable) controller.hide(WindowInsetsCompat.Type.systemBars())
    else controller.show(WindowInsetsCompat.Type.systemBars())
}
