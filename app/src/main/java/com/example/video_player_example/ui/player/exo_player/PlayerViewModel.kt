package com.example.video_player_example.ui.player.exo_player

import android.app.Application
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import android.util.Log
import androidx.media3.common.Player

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private var _player: ExoPlayer? = null
    private var currentUrl: String? = null
    private var isPreloading = false

    val player: ExoPlayer
        @OptIn(UnstableApi::class)
        get() {
            return _player ?: ExoPlayer.Builder(getApplication())
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                .setLoadControl(
                    DefaultLoadControl.Builder()
                        .setBufferDurationsMs(
                            /* minBufferMs = */ 2000,    // Reduced for faster startup
                            /* maxBufferMs = */ 15000,   // Still large enough for stability
                            /* bufferForPlaybackMs = */ 500,   // Much lower threshold for faster start
                            /* bufferForPlaybackAfterRebufferMs = */ 1500  // Reduced rebuffer requirement
                        )
                        .setPrioritizeTimeOverSizeThresholds(true)
                        .setTargetBufferBytes(-1) // Use time-based buffering only
                        .setBackBuffer(5000, true) // Smaller back buffer for faster startup
                        .build()
                )
                .build()
                .also { _player = it }
        }

    @OptIn(UnstableApi::class)
    fun ensurePlaying(url: String) {
        // Prevent repeated calls with same URL when already playing
        if (currentUrl == url && player.mediaItemCount > 0 && 
            (player.playbackState == Player.STATE_READY ||
             player.playbackState == Player.STATE_BUFFERING)) {
            return
        }
        currentUrl = url

        // Stop current playback before setting new media
        player.stop()
        player.clearMediaItems()

        if (url.startsWith("rtsp://")) {
            Log.d("PlayerViewModel", "Setting up RTSP stream: $url")
            
            // Use dedicated RTSP media source optimized for fast startup
            val rtspMediaSource = RtspMediaSource.Factory()
                .setForceUseRtpTcp(false) // Try UDP first for faster connection
                .setTimeoutMs(8000) // Reduced timeout for faster failure detection
                .setDebugLoggingEnabled(false) // Disable debug logging for performance
                .createMediaSource(
                    MediaItem.Builder()
                        .setUri(url)
                        .setLiveConfiguration(
                            MediaItem.LiveConfiguration.Builder()
                                .setTargetOffsetMs(1000) // Reduced for faster startup
                                .setMinPlaybackSpeed(0.95f) // Tighter range for better performance
                                .setMaxPlaybackSpeed(1.05f)
                                .setMinOffsetMs(500) // Lower minimum for faster start
                                .setMaxOffsetMs(5000) // Lower maximum for faster start
                                .build()
                        )
                        .build()
                )

            player.setMediaSource(rtspMediaSource)
            Log.d("PlayerViewModel", "RTSP media source configured")
        } else {
            // Regular video files
            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .build()
            player.setMediaItem(mediaItem)
        }

        // Prepare immediately for faster startup
        player.prepare()
        player.playWhenReady = true
        
        Log.d("PlayerViewModel", "Player prepared and ready for: $url")
    }
    
    @OptIn(UnstableApi::class)
    fun preloadStream(url: String) {
        if (isPreloading || currentUrl == url) return
        
        Log.d("PlayerViewModel", "Preloading stream: $url")
        isPreloading = true
        
        // Initialize player early to reduce startup time
        player // This triggers player creation
        isPreloading = false
    }
    fun stopPlayback() {
        _player?.let { player ->
            player.pause()
            player.stop()
            player.clearMediaItems()
        }
        currentUrl = null
    }
    
    @OptIn(UnstableApi::class)
    fun reconnectRtsp(url: String) {
        Log.d("PlayerViewModel", "Reconnecting RTSP stream: $url")
        currentUrl = null // Force reconnection
        ensurePlaying(url)
    }

    override fun onCleared() {
        super.onCleared()
        _player?.release()
        _player = null
    }
}
