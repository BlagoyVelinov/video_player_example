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
import androidx.lifecycle.viewmodel.compose.viewModel

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun RtspPlayerScreen(
    url: String = "rtsp://dev.gradotech.eu:8554/stream"
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
            }
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                Log.d("RTSP", "Video size: ${videoSize.width}x${videoSize.height}")
            }
        })
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }
    var volume by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(controlsVisible, myPlayer.isPlaying) {
        if (controlsVisible && myPlayer.isPlaying) {
            delay(3000)
            controlsVisible = false
        }
    }

    // Apply volume to player
    LaunchedEffect(volume) { myPlayer.volume = volume }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { controlsVisible = !controlsVisible })
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    // Use built-in controller (play/pause, seek, etc.)
                    useController = true
                    keepScreenOn = true
                    setKeepContentOnPlayerReset(true)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    player = myPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Minimal overlay: volume slider + fullscreen button
        if (controlsVisible) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(Color(0x66000000))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                
            ) {
                Text(text = "Volume", color = Color.White, style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Slider(
                    value = volume,
                    onValueChange = { volume = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.width(180.dp)
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
