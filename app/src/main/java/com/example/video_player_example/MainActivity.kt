package com.example.video_player_example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.video_player_example.ui.theme.Video_player_exampleTheme
import com.example.video_player_example.ui.player.RtspPlayerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Video_player_exampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    RtspPlayerScreen()
                }
            }
        }
    }
}