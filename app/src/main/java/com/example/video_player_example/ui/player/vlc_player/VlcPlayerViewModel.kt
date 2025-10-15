package com.example.video_player_example.ui.player.vlc_player

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

class VlcPlayerViewModel(application: Application) : AndroidViewModel(application) {
    private var _libVLC: LibVLC? = null
    private var _mediaPlayer: MediaPlayer? = null
    var currentUrl: String? = null
        private set
    
    init {
        Log.d("VlcPlayerViewModel", "ViewModel created: ${this.hashCode()}")
    }

    val isPlaying = mutableStateOf(false)
    val isPaused = mutableStateOf(false)
    
    val libVLC: LibVLC
        get() {
            return _libVLC ?: LibVLC(getApplication(), buildVlcOptions()).also { 
                _libVLC = it 
                Log.d("VlcPlayerViewModel", "LibVLC initialized")
            }
        }
    
    val mediaPlayer: MediaPlayer
        get() {
            return _mediaPlayer ?: MediaPlayer(libVLC).also { 
                _mediaPlayer = it
                setupMediaPlayerOptions(it)
                Log.d("VlcPlayerViewModel", "MediaPlayer initialized")
            }
        }
    
    private fun buildVlcOptions(): ArrayList<String> {
        return arrayListOf(
            // Network optimizations for stability
            "--network-caching=3000",
            "--live-caching=3000",
            "--file-caching=3000",
            
            // Clock and synchronization for stability
            "--clock-jitter=0",
            "--clock-synchro=1",
            
            // Video stability optimizations
            "--no-skip-frames",
            "--no-drop-late-frames",
            "--avcodec-skiploopfilter=0",
            
            // RTSP specific optimizations
            "--rtsp-tcp",
            "--rtsp-timeout=10",
            
            // Audio/Video sync
            "--audio-desync=0",
            "--no-audio-time-stretch",
            
            // Hardware acceleration (more conservative)
            "--codec=all",
            "--no-mediacodec-dr",
            
            // Buffer management
            "--avcodec-threads=0",
            "--sout-mux-caching=3000",
            
            // Reduce startup time
            "--no-video-title-show",
            "--no-snapshot-preview",
            "--no-sub-autodetect-file"
        )
    }
    
    private fun setupMediaPlayerOptions(player: MediaPlayer) {
        player.setEventListener { event ->
            when (event.type) {
                MediaPlayer.Event.Opening -> {
                    Log.d("VlcPlayer", "Opening media")
                }
                MediaPlayer.Event.Buffering -> {
                    Log.d("VlcPlayer", "Buffering: ${event.buffering}%")
                }
                MediaPlayer.Event.Playing -> {
                    Log.d("VlcPlayer", "Playing started")
                    isPlaying.value = true
                    isPaused.value = false
                }
                MediaPlayer.Event.Paused -> {
                    Log.d("VlcPlayer", "Paused")
                    isPlaying.value = false
                    isPaused.value = true
                }
                MediaPlayer.Event.Stopped -> {
                    Log.d("VlcPlayer", "Stopped")
                    isPlaying.value = false
                    isPaused.value = false
                }
                MediaPlayer.Event.EndReached -> {
                    Log.d("VlcPlayer", "End reached")
                    isPlaying.value = false
                    isPaused.value = false
                }
                MediaPlayer.Event.EncounteredError -> {
                    Log.e("VlcPlayer", "Error encountered - resetting states")
                    isPlaying.value = false
                    isPaused.value = false
                }
                MediaPlayer.Event.Vout -> {
                    Log.d("VlcPlayer", "Video output count: ${event.voutCount}")
                }
            }
        }
    }
    
    fun playMedia(url: String) {
        if (currentUrl == url && _mediaPlayer?.isPlaying == true) {
            Log.d("VlcPlayerViewModel", "Already playing: $url")
            return
        }
        
        Log.d("VlcPlayerViewModel", "Starting playback: $url")
        currentUrl = url
        
        try {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }

            val media = if (url.startsWith("rtsp://") || url.startsWith("http://") || url.startsWith("https://")) {

                val uri = Uri.parse(url)
                Log.d("VlcPlayerViewModel", "Creating media from URI: $uri")
                Media(libVLC, uri)
            } else {
                Log.d("VlcPlayerViewModel", "Creating media from file path: $url")
                Media(libVLC, url)
            }.apply {
                addOption(":network-caching=3000")
                addOption(":live-caching=3000")
                addOption(":file-caching=3000")
                addOption(":rtsp-tcp")
                addOption(":no-skip-frames")
                addOption(":clock-synchro=1")
                addOption(":avcodec-skiploopfilter=0")
            }

            mediaPlayer.media = media
            mediaPlayer.play()
            
            Log.d("VlcPlayerViewModel", "Playback started for: $url")
            
        } catch (e: Exception) {
            Log.e("VlcPlayerViewModel", "Failed to play media: $url", e)
        }
    }
    
    fun pausePlayback() {
        try {
            if (_mediaPlayer?.isPlaying == true) {
                _mediaPlayer?.pause()
                Log.d("VlcPlayerViewModel", "Playback paused")
            }
        } catch (e: Exception) {
            Log.e("VlcPlayerViewModel", "Error pausing playback", e)
        }
    }
    
    fun resumePlayback() {
        try {
            if (_mediaPlayer?.isPlaying == false && isPaused.value) {
                _mediaPlayer?.play()
                Log.d("VlcPlayerViewModel", "Playback resumed")
            }
        } catch (e: Exception) {
            Log.e("VlcPlayerViewModel", "Error resuming playback", e)
        }
    }
    
    fun togglePlayPause() {
        if (isPlaying.value) {
            pausePlayback()
        } else if (isPaused.value) {
            resumePlayback()
        }
    }
    
    fun stopPlayback() {
        try {
            if (_mediaPlayer?.isPlaying == true) {
                _mediaPlayer?.stop()
            }
            currentUrl = null
            Log.d("VlcPlayerViewModel", "Playback stopped")
        } catch (e: Exception) {
            Log.e("VlcPlayerViewModel", "Error stopping playback", e)
        }
    }
    
    fun setVolume(volume: Float) {
        try {
            val vlcVolume = (volume * 100).toInt().coerceIn(0, 100)
            mediaPlayer.volume = vlcVolume
            Log.d("VlcPlayerViewModel", "Volume set to: $vlcVolume")
        } catch (e: Exception) {
            Log.e("VlcPlayerViewModel", "Error setting volume", e)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        try {
            _mediaPlayer?.release()
            _libVLC?.release()
            Log.d("VlcPlayerViewModel", "Resources released")
        } catch (e: Exception) {
            Log.e("VlcPlayerViewModel", "Error releasing resources", e)
        }
        _mediaPlayer = null
        _libVLC = null
    }
}
