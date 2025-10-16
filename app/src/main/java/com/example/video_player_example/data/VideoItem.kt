package com.example.video_player_example.data

data class VideoItem(
    val id: String,
    val title: String,
    val url: String,
    val thumbnailUrl: String? = null,
    val description: String? = null,
    val duration: String? = null,
    val useVlc: Boolean = false
)

object VideoRepository {
    private val videos = listOf(
        VideoItem(
            id = "1",
            title = "RTSP Stream (VLC Player)",
            url = "rtsp://dev.gradotech.eu:8554/stream",
            description = "Sample RTSP stream using VLC player",
            duration = "Live",
            useVlc = true
        ),
        VideoItem(
            id = "2",
            title = "RTSP Stream (Exo Player)",
            url = "rtsp://dev.gradotech.eu:8554/stream",
            description = "Same RTSP stream using RTSP player",
            duration = "Live",
            useVlc = false
        ),
        VideoItem(
            id = "3", 
            title = "Big Buck Bunny",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            description = "Open source animated short film",
            duration = "10:34"
        ),
        VideoItem(
            id = "4",
            title = "RTSP emulator (My Server)",
            url = "http://10.0.2.2:8082/streams/d9518a3c-f1aa-41a8-8956-803d84c16e32/index.m3u8",
            description = "Open source animated short film",
            duration = "Live",
            useVlc = false
        ),
        VideoItem(
            id = "5",
            title = "RTSP real device (My Server)",
            url = "http://192.168.1.14:8082/streams/d9518a3c-f1aa-41a8-8956-803d84c16e32/index.m3u8",
            description = "Open source animated short film",
            duration = "Live",
            useVlc = false
        ),
    )
    
    fun getSampleVideos(): List<VideoItem> = videos
    
    fun findVideoByUrl(url: String): VideoItem? = videos.find { it.url == url }

    fun findVideoById(id: String): VideoItem? = videos.find { it.id == id }
}
