package com.example.video_player_example.ui.player.vlc_player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.util.Log
import android.view.WindowManager
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
    var volumeSliderVisible by remember { mutableStateOf(false) }
    var isUserTouchingSlider by remember { mutableStateOf(false) }
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var volume by rememberSaveable { mutableFloatStateOf(1f) }

    var hasInitialized by rememberSaveable(url) { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    LaunchedEffect(url) {
        if (!hasInitialized) {
            Log.d("VlcPlayerScreen", "First initialization for URL: $url")
            vm.playMedia(url)
            hasInitialized = true
            delay(1000)
            isLoading = false
        } else {
            Log.d("VlcPlayerScreen", "Already initialized, skipping playback start for: $url")
            isLoading = false
        }
    }

    LaunchedEffect(volume) {
        vm.setVolume(volume)
    }

    LaunchedEffect(controlsVisible, vm.isPlaying.value, isPortrait) {
        if (controlsVisible && vm.isPlaying.value && !isPortrait && !isUserTouchingSlider) {
            delay(3000)
            controlsVisible = false
        }
    }

    LaunchedEffect(vm.isPlaying.value, isPortrait) {
        if (!vm.isPlaying.value || isPortrait) {
            controlsVisible = true
        }
    }

    LaunchedEffect(volumeSliderVisible, isUserTouchingSlider) {
        if (volumeSliderVisible && !isUserTouchingSlider) {
            delay(3000)
            volumeSliderVisible = false
        }
    }

    LaunchedEffect(isFullscreen, configuration.orientation) {
        delay(100)

        val activity = context as? Activity ?: return@LaunchedEffect
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, !isFullscreen)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (isFullscreen) controller.hide(WindowInsetsCompat.Type.systemBars())
        else controller.show(WindowInsetsCompat.Type.systemBars())
    }

    DisposableEffect(vm.isPlaying.value) {
        val activity = context as? Activity
        if (vm.isPlaying.value && activity != null) {
            Log.d("VlcPlayerScreen", "Keeping screen awake")
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        
        onDispose {
            if (activity != null) {
                Log.d("VlcPlayerScreen", "Allowing screen to sleep")
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
    val playButtonBottomPadding = if (isPortrait) screenHeight * 0.0127f else screenHeight * 0.025f
    val playButtonStartPadding = if (isPortrait) screenWidth * 0.08f else screenWidth * 0.12f
    val playButtonEndPadding = if (isPortrait) 0.dp else screenWidth * 0.04f
    
    val controlBarBottomPadding = if (isPortrait) screenHeight * 0.002f else 0.dp
    val controlBarStartPadding = if (isPortrait) screenWidth * 0.12f else screenWidth * 0.08f
    val controlBarHorizontalPadding = screenWidth * 0.03f
    val controlBarVerticalPadding = if (isPortrait) screenHeight * 0.01f else screenHeight * 0.025f
    
    val volumeSliderWidth = if (isPortrait) screenWidth * 0.28f else screenWidth * 0.15f
    val controlBarWidthFraction = if (isPortrait) 0.9f else 0.8f
    val spacerWidth = screenWidth * 0.02f
    
    val backButtonTopPadding = if (isPortrait) screenHeight * 0.01f else screenHeight * 0.05f
    val backButtonStartPadding = if (isPortrait) screenWidth * 0.03f else screenWidth * 0.08f

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AndroidView(
            factory = { ctx ->
                VLCVideoLayout(ctx).apply {
                    vm.mediaPlayer.attachViews(this, null, false, false)
                    setOnClickListener {
                        controlsVisible = !controlsVisible
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

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
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (controlsVisible && !isLoading) {
            if (onBackPressed != null) {
                IconButton(
                    onClick = {
                        val activity = context as? Activity
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        vm.stopPlayback()
                        onBackPressed()
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(
                            top = backButtonTopPadding,
                            start = backButtonStartPadding
                        )
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

            IconButton(
                onClick = { vm.togglePlayPause() },
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
                    imageVector = if (vm.isPlaying.value) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (vm.isPlaying.value) "Pause" else "Play",
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

    DisposableEffect(Unit) {
        onDispose {
            vm.stopPlayback()
        }
    }
}

private fun toggleFullscreenWithRotation(
    context: Context,
    enable: Boolean,
    isCurrentlyPortrait: Boolean
) {
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
    controller.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    if (enable) controller.hide(WindowInsetsCompat.Type.systemBars())
    else controller.show(WindowInsetsCompat.Type.systemBars())
}
