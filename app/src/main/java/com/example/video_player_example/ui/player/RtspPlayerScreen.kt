package com.example.video_player_example.ui.player

import android.content.Context
import android.util.Log
import android.widget.Toast
import android.app.Activity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.lifecycle.viewmodel.compose.viewModel

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun RtspPlayerScreen(
    url: String = "rtsp://dev.gradotech.eu:8554/stream",
    onBackPressed: (() -> Unit)? = null
) {
    val context: Context = LocalContext.current
    val vm: PlayerViewModel = viewModel()
    LaunchedEffect(url) { vm.ensurePlaying(url) }
    val myPlayer: ExoPlayer = vm.player
    LaunchedEffect(myPlayer) {
        myPlayer.addListener(object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e("RTSP", "Playback error: ${error.errorCodeName}", error)
                Toast.makeText(context, "Playback error: ${error.errorCodeName}", Toast.LENGTH_LONG).show()
                try {
                    myPlayer.prepare()
                    myPlayer.playWhenReady = true
                } catch (_: Throwable) {}
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                val state = when (playbackState) {
                    Player.STATE_IDLE -> "IDLE"
                    Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    else -> playbackState.toString()
                }
                Log.d("RTSP", "State: $state")
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
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                Log.d("RTSP", "Video size: ${videoSize.width}x${videoSize.height}")
            }
        })
    }

    LaunchedEffect(myPlayer) {
        var lastPos = -1L
        var stagnantMs = 0L
        while (true) {
            val pos = myPlayer.currentPosition
            val state = myPlayer.playbackState
            if ((state == Player.STATE_BUFFERING || state == Player.STATE_READY) && myPlayer.playWhenReady) {
                if (pos == lastPos) stagnantMs += 1000 else stagnantMs = 0
                if (stagnantMs >= 15000L) {
                    try {
                        myPlayer.prepare()
                        myPlayer.playWhenReady = true
                    } catch (_: Throwable) {}
                    stagnantMs = 0
                }
            } else {
                stagnantMs = 0
            }
            lastPos = pos
            delay(1000)
        }
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }
    var volume by rememberSaveable { mutableFloatStateOf(1f) }
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

    // Apply volume to player
    LaunchedEffect(volume) { myPlayer.volume = volume }

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
                    // Prevent surface recreation during rotation
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    player = myPlayer
                    
                    // Adjust control padding for portrait mode
                    if (isPortrait) {
                        // Add bottom padding to raise controls higher in portrait
                        setPadding(0, 0, 0, 120) // 120dp bottom padding
                    }
                    
                    // Set up control visibility listener with immediate sync
                    setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                        // Use post to ensure immediate UI update
                        post {
                            controlsVisible = visibility == android.view.View.VISIBLE
                        }
                    })
                    
                    // Ensure consistent timing
                    controllerShowTimeoutMs = 3000
                    
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
                    pv.setPadding(0, 0, 0, 120)
                } else {
                    pv.setPadding(0, 0, 0, 0)
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
                        .background(Color(0x66000000), androidx.compose.foundation.shape.CircleShape)
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
                    toggleFullscreen(context, isFullscreen)
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

    // Do not release here; the ViewModel owns the player lifecycle
}

private fun toggleFullscreen(context: Context, enable: Boolean) {
    val activity = context as? Activity ?: return
    val window = activity.window
    WindowCompat.setDecorFitsSystemWindows(window, !enable)
    val controller = WindowInsetsControllerCompat(window, window.decorView)
    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    if (enable) controller.hide(WindowInsetsCompat.Type.systemBars())
    else controller.show(WindowInsetsCompat.Type.systemBars())
}
