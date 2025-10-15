package com.example.video_player_example.ui.player.exo_player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
    duration: String = "Live",
    onBackPressed: (() -> Unit)? = null
) {
    val context: Context = LocalContext.current
    val vm: PlayerViewModel = viewModel()
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(url) {
        vm.preloadStream(url)
        vm.ensurePlaying(url) 
    }
    val myPlayer: ExoPlayer = vm.player
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
                    
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(2000)
                        try {
                            vm.reconnectRtsp(url)
                        } catch (e: Exception) {
                            Log.e("RTSP", "Retry failed", e)
                        }
                    }
                } else {
                    Toast.makeText(context, "Connection failed: ${error.errorCodeName}", Toast.LENGTH_LONG).show()
                    
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
                if (playbackState != lastLoggedState) {
                    Log.d("RTSP", "State changed: $state (buffered: ${myPlayer.bufferedPercentage}%)")
                    lastLoggedState = playbackState
                    
                    if (playbackState == Player.STATE_READY && myPlayer.bufferedPercentage > 0) {
                        connectionAttempts = 0
                        isLoading = false // Hide loading indicator
                        Log.d("RTSP", "Connection successful, reset retry counter")
                    }
                    
                    if (playbackState == Player.STATE_BUFFERING) {
                        isLoading = true
                    }
                }
                
                if (url.startsWith("rtsp://")) {
                    if (playbackState == Player.STATE_ENDED) {
                        Log.w("RTSP", "Live stream ended unexpectedly, attempting reconnection")
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(1000)
                            try {
                                vm.reconnectRtsp(url)
                            } catch (e: Exception) {
                                Log.e("RTSP", "Failed to restart stream", e)
                            }
                        }
                    }
                } else {
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

            if (state == Player.STATE_READY && myPlayer.playWhenReady && bufferedPercentage > 0) {
                if (pos == lastPos && bufferedPercentage == lastBufferedPercentage) {
                    stagnantMs += 3000
                    consecutiveStagnantChecks++
                } else {
                    stagnantMs = 0
                    consecutiveStagnantChecks = 0
                }

                if (stagnantMs >= 60000L && consecutiveStagnantChecks >= 10) {
                    Log.w("RTSP", "Stream appears truly stagnant (${stagnantMs}ms), attempting reconnection")
                    try {
                        vm.reconnectRtsp(url)
                    } catch (_: Throwable) {}
                    stagnantMs = 0
                    consecutiveStagnantChecks = 0
                }
            } else {
                stagnantMs = 0
                consecutiveStagnantChecks = 0
            }
            lastPos = pos
            lastBufferedPercentage = bufferedPercentage
            delay(3000)
        }
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var volumeSliderVisible by remember { mutableStateOf(false) }
    var isUserTouchingSlider by remember { mutableStateOf(false) }
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var volume by rememberSaveable { mutableFloatStateOf(1f) }
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    var isPlaying by remember { mutableStateOf(myPlayer.isPlaying) }
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val playButtonBottomPadding = if (isPortrait) screenHeight * 0.015f else screenHeight * 0.025f
    val playButtonStartPadding = if (isPortrait) screenWidth * 0.08f else screenWidth * 0.12f
    val playButtonEndPadding = if (isPortrait) 0.dp else screenWidth * 0.04f
    
    val controlBarBottomPadding = if (isPortrait) screenHeight * 0.002f else 0.dp
    val controlBarStartPadding = if (isPortrait) screenWidth * 0.12f else screenWidth * 0.08f
    val controlBarHorizontalPadding = screenWidth * 0.03f
    val controlBarVerticalPadding = if (isPortrait) screenHeight * 0.01f else screenHeight * 0.025f
    
    val volumeSliderWidth = if (isPortrait) screenWidth * 0.28f else screenWidth * 0.15f
    val controlBarWidthFraction = if (isPortrait) 0.9f else 0.8f
    val spacerWidth = screenWidth * 0.02f

    DisposableEffect(myPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        myPlayer.addListener(listener)
        onDispose {
            myPlayer.removeListener(listener)
        }
    }

    LaunchedEffect(volume) { myPlayer.volume = volume }

    LaunchedEffect(isFullscreen, configuration.orientation) {

        delay(100)

        val activity = context as? Activity ?: return@LaunchedEffect
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, !isFullscreen)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (isFullscreen) controller.hide(WindowInsetsCompat.Type.systemBars())
        else controller.show(WindowInsetsCompat.Type.systemBars())
    }

    LaunchedEffect(volumeSliderVisible, isUserTouchingSlider) {
        if (volumeSliderVisible && !isUserTouchingSlider) {
            delay(3000)
            volumeSliderVisible = false
        }
    }

    LaunchedEffect(controlsVisible, isPlaying, isPortrait) {
        if (controlsVisible && isPlaying && !isPortrait) {
            delay(3000)
            controlsVisible = false
        }
    }

    LaunchedEffect(isPlaying, isPortrait) {
        if (!isPlaying || isPortrait) {
            controlsVisible = true
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    keepScreenOn = true
                    setKeepContentOnPlayerReset(true)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    player = myPlayer
                    setOnClickListener {
                        controlsVisible = !controlsVisible
                    }
                    
                    playerView = this
                }
            },
            update = { pv ->
                if (pv.player != myPlayer) {
                    pv.player = myPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (controlsVisible) {
            if (onBackPressed != null) {
                IconButton(
                    onClick = {
                        // Rotate to portrait before going back
                        val activity = context as? Activity
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        
                        vm.stopPlayback()
                        onBackPressed()
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(16.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            // Play/Pause button at bottom left
            IconButton(
                onClick = {
                    if (isPlaying) {
                        myPlayer.pause()
                    } else {
                        myPlayer.play()
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(
                        bottom = playButtonBottomPadding,
                        start = playButtonStartPadding,
                        end = playButtonEndPadding
                    )
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isPortrait) Arrangement.SpaceAround
                        else Arrangement.SpaceBetween,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .background(
                        Color(0x66000000).copy(alpha = 0.4f),
                        CircleShape
                    )
                    .padding(
                        horizontal = controlBarHorizontalPadding,
                        vertical = controlBarVerticalPadding
                    )
                    .padding(bottom = controlBarBottomPadding)
                    .padding(start = controlBarStartPadding)
                    .fillMaxWidth(controlBarWidthFraction)
            ) {
                Row (
                    verticalAlignment = Alignment.CenterVertically,
                ){
                    IconButton(
                        onClick = { volumeSliderVisible = !volumeSliderVisible },
                        modifier = Modifier.background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            CircleShape
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Volume",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (volumeSliderVisible) {
                        Slider(
                            value = volume,
                            onValueChange = {
                                volume = it
                                isUserTouchingSlider = true
                            },
                            onValueChangeFinished = {
                                isUserTouchingSlider = false
                            },
                            valueRange = 0f..1f,
                            modifier = Modifier.width(volumeSliderWidth)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(spacerWidth))
                IconButton(onClick = {
                    isFullscreen = !isFullscreen
                    toggleFullscreenWithRotation(context, isFullscreen, isPortrait)
                },
                    modifier = Modifier.background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        CircleShape
                    )
                    ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isFullscreen) "Exit Fullscreen" else "Enter Fullscreen",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun toggleFullscreenWithRotation(context: Context, enable: Boolean, isCurrentlyPortrait: Boolean) {
    val activity = context as? Activity ?: return

    if (enable) {
        if (isCurrentlyPortrait) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    } else {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    val window = activity.window
    WindowCompat.setDecorFitsSystemWindows(window, !enable)
    val controller = WindowInsetsControllerCompat(window, window.decorView)
    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    if (enable) controller.hide(WindowInsetsCompat.Type.systemBars())
    else controller.show(WindowInsetsCompat.Type.systemBars())
}
