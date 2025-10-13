package com.example.video_player_example.data

data class VideoItem(
    val id: String,
    val title: String,
    val url: String,
    val thumbnailUrl: String? = null,
    val description: String? = null,
    val duration: String? = null
)

object VideoRepository {
    private val videos = listOf(
        VideoItem(
            id = "1",
            title = "RTSP Stream 1",
            url = "rtsp://dev.gradotech.eu:8554/stream",
            description = "Sample RTSP stream for testing",
            duration = "Live"
        ),
        VideoItem(
            id = "2", 
            title = "Big Buck Bunny",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            description = "Open source animated short film",
            duration = "10:34"
        ),
        VideoItem(
            id = "3",
            title = "Elephant Dream", 
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            description = "Open source animated film",
            duration = "10:53"
        ),
        VideoItem(
            id = "4",
            title = "For Bigger Blazes",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4", 
            description = "Sample video content",
            duration = "00:15"
        ),
        VideoItem(
            id = "5",
            title = "Sintel",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            description = "Open source fantasy short film",
            duration = "14:48"
        )
    )
    
    fun getSampleVideos(): List<VideoItem> = videos
    
    fun findVideoByUrl(url: String): VideoItem? = videos.find { it.url == url }
}
