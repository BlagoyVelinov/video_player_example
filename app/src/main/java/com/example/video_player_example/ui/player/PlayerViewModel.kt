package com.example.video_player_example.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private var _player: ExoPlayer? = null
    private var currentUrl: String? = null

    val player: ExoPlayer
        get() {
            return _player ?: ExoPlayer.Builder(getApplication())
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .build()
                .also { _player = it }
        }

    fun ensurePlaying(url: String) {
        if (currentUrl == url && player.mediaItemCount > 0) return
        currentUrl = url
        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMimeType(MimeTypes.APPLICATION_RTSP)
            .build()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    override fun onCleared() {
        super.onCleared()
        _player?.release()
        _player = null
    }
}
