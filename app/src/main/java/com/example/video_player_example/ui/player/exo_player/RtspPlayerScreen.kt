package com.example.video_player_example.ui.player.exo_player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun RtspPlayerScreen(
    url: String = "rtsp://dev.gradotech.eu:8554/stream",
    onBackPressed: (() -> Unit)? = null
) {
    val context: Context = LocalContext.current
    val vm: PlayerViewModel = viewModel()
    var isLoading by remember { mutableStateOf(true) }
    
    // Preload the stream for faster startup
    LaunchedEffect(url) { 
        vm.preloadStream(url)
        vm.ensurePlaying(url) 
    }
    val myPlayer: ExoPlayer = vm.player
    // Add listener only once per player instance
    DisposableEffect(myPlayer) {
        var lastLoggedState = -1
        var connectionAttempts = 0
        val maxRetries = 3
        
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e("RTSP", "Playback error: ${error.errorCodeName} (attempt ${connectionAttempts + 1})", error)
                
                if (url.startsWith("rtsp://") && connectionAttempts < maxRetries) {
                    connectionAttempts++
                    Log.w("RTSP", "Retrying RTSP connection (attempt $connectionAttempts/$maxRetries)")
                    
                    // Wait a bit before retrying
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(2000)
                        try {
                            vm.reconnectRtsp(url)
                        } catch (e: Exception) {
                            Log.e("RTSP", "Retry failed", e)
                        }
                    }
                } else {
                    // Show error after max retries or for non-RTSP streams
                    Toast.makeText(context, "Connection failed: ${error.errorCodeName}", Toast.LENGTH_LONG).show()
                    
                    // For regular video files, try immediate retry
                    if (!url.startsWith("rtsp://")) {
                        try {
                            myPlayer.prepare()
                            myPlayer.playWhenReady = true
                        } catch (_: Throwable) {}
                    }
                }
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                val state = when (playbackState) {
                    Player.STATE_IDLE -> "IDLE"
                    Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    else -> playbackState.toString()
                }
                // Only log actual state changes to reduce spam
                if (playbackState != lastLoggedState) {
                    Log.d("RTSP", "State changed: $state (buffered: ${myPlayer.bufferedPercentage}%)")
                    lastLoggedState = playbackState
                    
                    // Reset connection attempts on successful connection
                    if (playbackState == Player.STATE_READY && myPlayer.bufferedPercentage > 0) {
                        connectionAttempts = 0
                        isLoading = false // Hide loading indicator
                        Log.d("RTSP", "Connection successful, reset retry counter")
                    }
                    
                    // Show loading for buffering states
                    if (playbackState == Player.STATE_BUFFERING) {
                        isLoading = true
                    }
                }
                
                // Handle state changes appropriately for different stream types
                if (url.startsWith("rtsp://")) {
                    // For RTSP live streams, ENDED state means connection lost - restart
                    if (playbackState == Player.STATE_ENDED) {
                        Log.w("RTSP", "Live stream ended unexpectedly, attempting reconnection")
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(1000) // Brief delay before reconnecting
                            try {
                                vm.reconnectRtsp(url)
                            } catch (e: Exception) {
                                Log.e("RTSP", "Failed to restart stream", e)
                            }
                        }
                    }
                } else {
                    // For regular video files, handle IDLE and ENDED normally
                    if (playbackState == Player.STATE_IDLE) {
                        try {
                            myPlayer.prepare()
                            myPlayer.playWhenReady = true
                        } catch (_: Throwable) {}
                    }
                    if (playbackState == Player.STATE_ENDED) {
                        try {
                            myPlayer.seekToDefaultPosition()
                            myPlayer.playWhenReady = true
                        } catch (_: Throwable) {}
                    }
                }
            }
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                Log.d("RTSP", "Video size: ${videoSize.width}x${videoSize.height}")
            }
        }
        
        myPlayer.addListener(listener)
        
        onDispose {
            myPlayer.removeListener(listener)
        }
    }

    // Connection monitoring for RTSP streams only - more conservative approach
    LaunchedEffect(myPlayer, url) {
        if (!url.startsWith("rtsp://")) return@LaunchedEffect
        
        var lastPos = -1L
        var stagnantMs = 0L
        var consecutiveStagnantChecks = 0
        var lastBufferedPercentage = 0
        
        while (true) {
            val pos = myPlayer.currentPosition
            val state = myPlayer.playbackState
            val bufferedPercentage = myPlayer.bufferedPercentage
            
            // Only monitor for true stagnation during READY state with actual buffering
            if (state == Player.STATE_READY && myPlayer.playWhenReady && bufferedPercentage > 0) {
                if (pos == lastPos && bufferedPercentage == lastBufferedPercentage) {
                    stagnantMs += 3000
                    consecutiveStagnantChecks++
                } else {
                    stagnantMs = 0
                    consecutiveStagnantChecks = 0
                }
                
                // Only restart if truly stagnant for 60+ seconds AND multiple consecutive checks
                // This is much more conservative to avoid unnecessary restarts
                if (stagnantMs >= 60000L && consecutiveStagnantChecks >= 10) {
                    Log.w("RTSP", "Stream appears truly stagnant (${stagnantMs}ms), attempting reconnection")
                    try {
                        vm.reconnectRtsp(url)
                    } catch (_: Throwable) {}
                    stagnantMs = 0
                    consecutiveStagnantChecks = 0
                }
            } else {
                // Reset counters during buffering or other states
                stagnantMs = 0
                consecutiveStagnantChecks = 0
            }
            lastPos = pos
            lastBufferedPercentage = bufferedPercentage
            delay(3000) // Check even less frequently
        }
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var volume by rememberSaveable { mutableFloatStateOf(1f) }
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    // Apply volume to player
    LaunchedEffect(volume) { myPlayer.volume = volume }

    // Ensure fullscreen state is applied after orientation changes - only manage system UI
    LaunchedEffect(isFullscreen, configuration.orientation) {
        // Small delay to ensure orientation change is complete
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
    
    // Auto-enter fullscreen when rotating to landscape if not already in fullscreen
    LaunchedEffect(configuration.orientation) {
        if (!isPortrait && !isFullscreen) {
            // Optionally auto-enter fullscreen in landscape
            // Uncomment the next line if you want automatic fullscreen in landscape
            // isFullscreen = true
        }
    }

    // Let PlayerView handle all timing - we just react immediately

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    // Use built-in controller (play/pause, seek, etc.)
                    useController = true
                    keepScreenOn = true
                    setKeepContentOnPlayerReset(true)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    // Optimize for faster startup
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING) // Show buffering only when playing
                    player = myPlayer
                    
                    // Adjust control padding for portrait mode
                    if (isPortrait) {
                        // Add bottom padding to raise controls higher in portrait
                        setPadding(0, 0, 0, 30) // 120dp bottom padding
                    }
                    
                    // Set up control visibility listener with immediate sync
                    setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                        // Use post to ensure immediate UI update
                        post {
                            controlsVisible = visibility == View.VISIBLE
                        }
                    })
                    
                    // Ensure consistent timing
                    controllerShowTimeoutMs = 3000
                    
                    // Hide inappropriate controls for RTSP streams
                    if (url.startsWith("rtsp://")) {
                        // Hide fast forward, rewind, next, previous buttons for live streams
                        setShowFastForwardButton(false)
                        setShowRewindButton(false)
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                    } else {
                        // Show all controls for regular video files
                        setShowFastForwardButton(true)
                        setShowRewindButton(true)
                        setShowNextButton(false) // Usually false unless you have a playlist
                        setShowPreviousButton(false) // Usually false unless you have a playlist
                    }
                    
                    playerView = this
                }
            },
            update = { pv ->
                // Ensure player is always attached during recomposition
                if (pv.player != myPlayer) {
                    pv.player = myPlayer
                }
                
                // Update padding based on orientation
                if (isPortrait) {
                    pv.setPadding(0, 0, 0, 80)
                } else {
                    pv.setPadding(0, 0, 0, 0)
                }
                
                // Update control visibility based on stream type
                if (url.startsWith("rtsp://")) {
                    pv.setShowFastForwardButton(false)
                    pv.setShowRewindButton(false)
                    pv.setShowNextButton(false)
                    pv.setShowPreviousButton(false)
                } else {
                    pv.setShowFastForwardButton(true)
                    pv.setShowRewindButton(true)
                    pv.setShowNextButton(false)
                    pv.setShowPreviousButton(false)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Player controls overlay
        if (controlsVisible) {
            // Back button (top-left)
            if (onBackPressed != null) {
                IconButton(
                    onClick = {
                        // Properly stop the video using ViewModel
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
            
            // Bottom controls: volume slider + fullscreen button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(Color(0x00000000))
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .padding(bottom = if (isPortrait) 0.dp else 0.dp)
                
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
