package com.example.video_player_example.ui.player

import android.app.Application
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private var _player: ExoPlayer? = null
    private var currentUrl: String? = null

    val player: ExoPlayer
        @OptIn(UnstableApi::class)
        get() {
            return _player ?: ExoPlayer.Builder(getApplication())
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                .build()
                .also { _player = it }
        }

    fun ensurePlaying(url: String) {
        if (currentUrl == url && player.mediaItemCount > 0) return
        currentUrl = url
        
        val mediaItemBuilder = MediaItem.Builder().setUri(url)
        
        // Configure for RTSP streams
        if (url.startsWith("rtsp://")) {
            mediaItemBuilder
                .setMimeType(MimeTypes.APPLICATION_RTSP)
                .setLiveConfiguration(
                    MediaItem.LiveConfiguration.Builder()
                        .setTargetOffsetMs(3000)
                        .setMinPlaybackSpeed(0.97f)
                        .setMaxPlaybackSpeed(1.03f)
                        .build()
                )
        }
        
        val mediaItem = mediaItemBuilder.build()
        player.setMediaItem(mediaItem)
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

    override fun onCleared() {
        super.onCleared()
        _player?.release()
        _player = null
    }
}
