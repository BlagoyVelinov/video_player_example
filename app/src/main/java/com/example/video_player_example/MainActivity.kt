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
import com.example.video_player_example.ui.theme.Video_player_exampleTheme
import com.example.video_player_example.ui.player.RtspPlayerScreen
import com.example.video_player_example.ui.main.MainScreen
import com.example.video_player_example.data.VideoItem
import com.example.video_player_example.data.VideoRepository

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
    var selectedVideoUrl by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedVideo = selectedVideoUrl?.let { VideoRepository.findVideoByUrl(it) }
    
    Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
        if (selectedVideo == null) {
            MainScreen(
                onVideoSelected = { video ->
                    selectedVideoUrl = video.url
                }
            )
        } else {
            RtspPlayerScreen(
                url = selectedVideo.url,
                onBackPressed = {
                    selectedVideoUrl = null
                }
            )
        }
    }
}