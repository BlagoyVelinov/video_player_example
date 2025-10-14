package com.example.video_player_example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.example.video_player_example.data.VideoRepository
import com.example.video_player_example.ui.main.MainScreen
import com.example.video_player_example.ui.player.exo_player.RtspPlayerScreen
import com.example.video_player_example.ui.player.vlc_player.VlcPlayerScreen
import com.example.video_player_example.ui.theme.Video_player_exampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Video_player_exampleTheme {
                VideoPlayerApp()
            }
        }
    }
}

@Composable
fun VideoPlayerApp() {
    var selectedVideoId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedVideo = selectedVideoId?.let { VideoRepository.findVideoById(it) }
    
    Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
        if (selectedVideo == null) {
            MainScreen(
                onVideoSelected = { video ->
                    selectedVideoId = video.id
                }
            )
        } else {
            if (selectedVideo.useVlc) {
                VlcPlayerScreen(
                    url = selectedVideo.url,
                    onBackPressed = {
                        selectedVideoId = null
                    }
                )
            } else {
                RtspPlayerScreen(
                    url = selectedVideo.url,
                    onBackPressed = {
                        selectedVideoId = null
                    }
                )
            }
        }
    }
}