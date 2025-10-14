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
    
    // Playback state
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
            "--network-caching=3000",       // Increased caching for better stability
            "--live-caching=3000",         // Increased live stream caching
            "--file-caching=3000",         // File caching for local content
            
            // Clock and synchronization for stability
            "--clock-jitter=0",            // Disable clock jitter
            "--clock-synchro=1",           // Enable clock synchronization for stability
            
            // Video stability optimizations
            "--no-skip-frames",            // Don't skip frames to avoid freezing
            "--no-drop-late-frames",       // Don't drop late frames
            "--avcodec-skiploopfilter=0",  // Don't skip loop filter
            
            // RTSP specific optimizations
            "--rtsp-tcp",                  // Force TCP for RTSP (more reliable)
            "--rtsp-timeout=10",           // RTSP timeout
            
            // Audio/Video sync
            "--audio-desync=0",            // No audio desync
            "--no-audio-time-stretch",     // Disable audio time stretching
            
            // Hardware acceleration (more conservative)
            "--codec=all",                 // Use all available codecs
            "--no-mediacodec-dr",          // Disable direct rendering for stability
            
            // Buffer management
            "--avcodec-threads=0",         // Auto-detect thread count
            "--sout-mux-caching=3000",     // Output mux caching
            
            // Reduce startup time
            "--no-video-title-show",       // Don't show video title
            "--no-snapshot-preview",       // Disable snapshot preview
            "--no-sub-autodetect-file"     // Don't auto-detect subtitle files
        )
    }
    
    private fun setupMediaPlayerOptions(player: MediaPlayer) {
        // Set up event listeners for better debugging
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
            // Stop current playback
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            
            // Create media with optimized options
            val media = if (url.startsWith("rtsp://") || url.startsWith("http://") || url.startsWith("https://")) {
                // For network streams, use Uri to ensure proper parsing
                val uri = Uri.parse(url)
                Log.d("VlcPlayerViewModel", "Creating media from URI: $uri")
                Media(libVLC, uri)
            } else {
                // For local files, use string path
                Log.d("VlcPlayerViewModel", "Creating media from file path: $url")
                Media(libVLC, url)
            }.apply {
                // Add media-specific options for stability
                addOption(":network-caching=3000")
                addOption(":live-caching=3000")
                addOption(":file-caching=3000")
                
                // Additional stability options
                addOption(":rtsp-tcp")
                addOption(":no-skip-frames")
                addOption(":clock-synchro=1")
                addOption(":avcodec-skiploopfilter=0")
            }
            
            // Set media and play
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
