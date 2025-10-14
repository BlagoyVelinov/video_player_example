package com.example.video_player_example.ui.player

import android.app.Application
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.datasource.DefaultDataSource
import android.util.Log

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private var _player: ExoPlayer? = null
    private var currentUrl: String? = null

    val player: ExoPlayer
        @OptIn(UnstableApi::class)
        get() {
            return _player ?: ExoPlayer.Builder(getApplication())
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                .setLoadControl(
                    DefaultLoadControl.Builder()
                        .setBufferDurationsMs(
                            /* minBufferMs = */ 5000,    // Increased for live streams
                            /* maxBufferMs = */ 20000,   // Larger buffer for stability
                            /* bufferForPlaybackMs = */ 2000,  // Higher threshold to start
                            /* bufferForPlaybackAfterRebufferMs = */ 5000  // More buffer after rebuffer
                        )
                        .setPrioritizeTimeOverSizeThresholds(true)
                        .setTargetBufferBytes(-1) // Use time-based buffering only
                        .setBackBuffer(10000, true) // Keep back buffer for seeking
                        .build()
                )
                .build()
                .also { _player = it }
        }

    @OptIn(UnstableApi::class)
    fun ensurePlaying(url: String) {
        // Prevent repeated calls with same URL when already playing
        if (currentUrl == url && player.mediaItemCount > 0 && 
            (player.playbackState == androidx.media3.common.Player.STATE_READY || 
             player.playbackState == androidx.media3.common.Player.STATE_BUFFERING)) {
            return
        }
        currentUrl = url

        // Stop current playback before setting new media
        player.stop()
        player.clearMediaItems()

        if (url.startsWith("rtsp://")) {
            Log.d("PlayerViewModel", "Setting up RTSP stream: $url")
            
            // Use dedicated RTSP media source with enhanced configuration for live streams
            val rtspMediaSource = RtspMediaSource.Factory()
                .setForceUseRtpTcp(false) // Try UDP first, fallback to TCP
                .setTimeoutMs(20000) // Increased timeout for initial connection
                .setDebugLoggingEnabled(true) // Enable debug logging
                .createMediaSource(
                    MediaItem.Builder()
                        .setUri(url)
                        .setLiveConfiguration(
                            MediaItem.LiveConfiguration.Builder()
                                .setTargetOffsetMs(3000) // Increased buffer for stability
                                .setMinPlaybackSpeed(0.9f) // More tolerance for speed variation
                                .setMaxPlaybackSpeed(1.1f)
                                .setMinOffsetMs(1000) // Minimum live offset
                                .setMaxOffsetMs(10000) // Maximum live offset
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

        player.prepare()
        player.playWhenReady = true
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
