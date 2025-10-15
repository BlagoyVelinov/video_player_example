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
                            /* minBufferMs = */ 2000,
                            /* maxBufferMs = */ 15000,
                            /* bufferForPlaybackMs = */ 500,
                            /* bufferForPlaybackAfterRebufferMs = */ 1500
                        )
                        .setPrioritizeTimeOverSizeThresholds(true)
                        .setTargetBufferBytes(-1)
                        .setBackBuffer(5000, true)
                        .build()
                )
                .build()
                .also { _player = it }
        }

    @OptIn(UnstableApi::class)
    fun ensurePlaying(url: String) {
        if (currentUrl == url && player.mediaItemCount > 0 && 
            (player.playbackState == Player.STATE_READY ||
             player.playbackState == Player.STATE_BUFFERING)) {
            return
        }
        currentUrl = url

        player.stop()
        player.clearMediaItems()

        if (url.startsWith("rtsp://")) {
            Log.d("PlayerViewModel", "Setting up RTSP stream: $url")

            val rtspMediaSource = RtspMediaSource.Factory()
                .setForceUseRtpTcp(true)
                .setTimeoutMs(8000)
                .setDebugLoggingEnabled(false)
                .createMediaSource(
                    MediaItem.Builder()
                        .setUri(url)
                        .setLiveConfiguration(
                            MediaItem.LiveConfiguration.Builder()
                                .setTargetOffsetMs(1000)
                                .setMinPlaybackSpeed(0.95f)
                                .setMaxPlaybackSpeed(1.05f)
                                .setMinOffsetMs(500)
                                .setMaxOffsetMs(5000)
                                .build()
                        )
                        .build()
                )

            player.setMediaSource(rtspMediaSource)
            Log.d("PlayerViewModel", "RTSP media source configured")
        } else {

            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .build()
            player.setMediaItem(mediaItem)
        }

        player.prepare()
        player.playWhenReady = true
        
        Log.d("PlayerViewModel", "Player prepared and ready for: $url")
    }
    
    @OptIn(UnstableApi::class)
    fun preloadStream(url: String) {
        if (isPreloading || currentUrl == url) return
        
        Log.d("PlayerViewModel", "Preloading stream: $url")
        isPreloading = true

        player
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
        currentUrl = null
        ensurePlaying(url)
    }

    override fun onCleared() {
        super.onCleared()
        _player?.release()
        _player = null
    }
}
