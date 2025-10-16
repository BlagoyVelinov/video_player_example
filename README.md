# Stream Video Player

A professional Android video player application built with Kotlin and Jetpack Compose, supporting multiple streaming protocols and dual player engines (VLC and ExoPlayer).

## Overview

This application provides a robust video streaming solution with support for RTSP, HTTP, and HLS streams. It features two independent player implementations that can be compared side-by-side, making it ideal for testing stream quality, performance, and compatibility.

## Features

### Dual Player Architecture
- **VLC Player**: LibVLC-based player with extensive codec support and network resilience
- **ExoPlayer**: Google's media player with native Android integration and HLS support

### Streaming Protocol Support
- **RTSP** (Real-Time Streaming Protocol) - Live camera feeds and streaming servers
- **HTTP/HTTPS** - Standard web-based video streams
- **HLS** (HTTP Live Streaming) - Adaptive bitrate streaming with `.m3u8` playlists

### Player Capabilities

#### VLC Player Features
- ✅ Extensive codec support (H.264, H.265, VP8, VP9, etc.)
- ✅ Network error recovery with automatic reconnection
- ✅ Enhanced buffering for unstable connections (3000ms cache)
- ✅ TCP enforcement for RTSP streams (more reliable than UDP)
- ✅ Hardware acceleration support
- ✅ Volume control with visual slider
- ✅ Fullscreen mode with automatic landscape rotation
- ✅ Screen wake lock during playback
- ✅ Responsive UI that adapts to screen size and orientation

#### ExoPlayer Features
- ✅ Native Android media framework integration
- ✅ HLS adaptive streaming support
- ✅ Automatic retry logic for RTSP connections (up to 3 attempts)
- ✅ Stagnant stream detection and recovery
- ✅ Built-in buffering indicators
- ✅ Volume control with visual slider
- ✅ Fullscreen mode with automatic landscape rotation
- ✅ Screen wake lock during playback
- ✅ Responsive UI that adapts to screen size and orientation

### User Interface
- **Modern Material Design 3** UI with Jetpack Compose
- **Responsive Controls**: Adapt to portrait and landscape orientations
- **Auto-hide Controls**: Controls fade out during playback in landscape mode
- **System UI Integration**: Proper handling of navigation bars and status bars
- **Loading Indicators**: Visual feedback during stream connection
- **Touch Gestures**: Tap to show/hide controls

## Testing the Application

### Prerequisites
- Android device or emulator running Android 7.0 (API 24) or higher
- For emulator testing: Use `10.0.2.2` to access localhost on your development machine
- For real device testing: Use your computer's local network IP address

### Testing with StreamDecoder-Server

This application is designed to work with the [StreamDecoder-Server](https://github.com/BlagoyVelinov/StreamDecoder-Server), which provides HLS stream transcoding from various sources.

#### Current Testing Method (Manual Configuration)

Since the application currently requires manual URL configuration, follow these steps:

1. **Start the StreamDecoder-Server**
   ```bash
   # Follow the setup instructions from the StreamDecoder-Server repository
   # The server typically runs on http://localhost:8082
   ```

2. **Make a Request to the Server**
   
   Use an API client (Postman, curl, etc.) or your agent to request a stream:
   ```bash
   # Example request to start streaming
   POST http://localhost:8082/api/stream/start
   {
     "source": "rtsp://your-camera-url",
     "streamId": "unique-stream-id"
   }
   ```

3. **Get the HLS URL from Response**
   
   The server will respond with an HLS URL, typically in this format:
   ```
   http://localhost:8082/streams/{stream-id}/index.m3u8
   ```

4. **Add the URL to VideoRepository**
   
   Open `app/src/main/java/com/example/video_player_example/data/VideoItem.kt` and add a new `VideoItem`:
   
   ```kotlin
   VideoItem(
       id = "5",
       title = "My Custom Stream",
       url = "http://10.0.2.2:8082/streams/your-stream-id/index.m3u8", // For emulator
       // url = "http://192.168.x.X:8082/streams/your-stream-id/index.m3u8", // For real device
       description = "Stream from StreamDecoder-Server",
       duration = "Live",
       useVlc = false // Use ExoPlayer for HLS streams
   )
   ```

5. **Run the Application**
   - Build and run the app on your device/emulator
   - Select your newly added stream from the video list
   - The player will connect and start playback

#### Testing URLs Already Configured

The application comes with pre-configured test streams:

**For Android Emulator:**
```kotlin
VideoItem(
    id = "3",
    title = "RTSP emulator (My Server)",
    url = "http://10.0.2.2:8082/streams/d9518a3c-f1aa-41a8-8956-803d84c16e32/index.m3u8",
    useVlc = false
)
```

**For Real Android Device:**
```kotlin
VideoItem(
    id = "4",
    title = "RTSP real device (My Server)",
    url = "http://192.168.x.x:8082/streams/d9518a3c-f1aa-41a8-8956-803d84c16e32/index.m3u8",
    useVlc = false
)
```

> **Note**: Replace `192.168.x.x` with your computer's actual local IP address.

### Testing with Public RTSP Streams

The app includes a public RTSP stream for immediate testing:

```kotlin
VideoItem(
    id = "1",
    title = "RTSP Stream (VLC Player)",
    url = "rtsp://dev.gradotech.eu:8554/stream",
    useVlc = true
)
```

### Comparing VLC vs ExoPlayer

The application allows you to test the same stream with both players:

1. Add two `VideoItem` entries with the same URL
2. Set `useVlc = true` for VLC player
3. Set `useVlc = false` for ExoPlayer
4. Compare performance, startup time, and stability

## Project Structure

```
app/src/main/java/com/example/video_player_example/
├── data/
│   └── VideoItem.kt                  # Video data models and repository
├── ui/
│   ├── main/
│   │   └── MainScreen.kt             # Video list screen
│   ├── screens/
│   │   ├── ExoPlayerScreen.kt        # ExoPlayer UI screen
│   │   └── VlcPlayerScreen.kt        # VLC player UI screen
│   ├── viewmodels/
│   │   ├── ExoPlayerViewModel.kt     # ExoPlayer logic and state
│   │   └── VlcPlayerViewModel.kt     # VLC player logic and state
│   ├── utils/
│   │   ├── FullscreenHelper.kt       # Fullscreen mode utilities
│   │   └── PlayerDimensions.kt       # Player sizing utilities
│   └── theme/
│       ├── Color.kt                  # Material Design colors
│       ├── Theme.kt                  # App theme configuration
│       └── Type.kt                   # Typography definitions
└── MainActivity.kt                   # App entry point and navigation
```

## Configuration

### Adding New Streams

Edit `VideoItem.kt` to add new streams:

```kotlin
VideoItem(
    id = "unique-id",
    title = "Stream Title",
    url = "rtsp://... or http://... or https://...",
    description = "Stream description",
    duration = "Live", // or actual duration
    useVlc = false // true for VLC, false for ExoPlayer
)
```

### Network Configuration

**For Emulator:**
- Use `10.0.2.2` to access `localhost` on your development machine
- Example: `http://10.0.2.2:8082/streams/...`

**For Real Device:**
- Use your computer's local network IP address
- Find your IP: `ipconfig` (Windows) or `ifconfig` (Mac/Linux)
- Example: `http://192.168.x.x:8082/streams/...`
- Ensure device and computer are on the same network

## Technical Details

### Dependencies
- **Jetpack Compose**: Modern declarative UI framework
- **Material Design 3**: Latest Material Design components
- **LibVLC**: VLC media player library for Android
- **ExoPlayer (Media3)**: Google's media player framework
- **Kotlin Coroutines**: Asynchronous programming

### Player Selection Logic
The app uses the `useVlc` flag to determine which player to use:
- `useVlc = true` → VLC Player (best for RTSP and diverse codecs)
- `useVlc = false` → ExoPlayer (best for HLS and HTTP streams)

### Network Resilience
- **VLC**: 3-attempt reconnection with exponential backoff (2s, 4s, 6s)
- **ExoPlayer**: 3-attempt retry for RTSP, automatic stagnant stream detection

## Future Enhancements

- [ ] Dynamic stream URL input (no need to edit code)
- [ ] Direct integration with StreamDecoder-Server API
- [ ] Stream history and favorites
- [ ] Playback speed control
- [ ] Audio track selection
- [ ] Subtitle support
- [ ] Picture-in-Picture mode
- [ ] Chromecast support

## Known Limitations

- **Manual URL Configuration**: Currently requires editing `VideoItem.kt` to add new streams
- **VLC Pause on Live Streams**: Live RTSP streams cannot be paused (they restart when resumed)
- **HLS with VLC**: ExoPlayer is recommended for HLS streams (better compatibility)

## Troubleshooting

### Stream Won't Play
1. Check network connectivity
2. Verify the stream URL is correct
3. For local streams, ensure correct IP address (10.0.2.2 for emulator)
4. Try switching between VLC and ExoPlayer
5. Check server logs for errors

### App Crashes on Startup
1. Ensure all dependencies are properly installed
2. Clean and rebuild the project
3. Check Android version compatibility (API 24+)

### Controls Not Visible
1. Tap the screen to show controls
2. Controls auto-hide after 3 seconds in landscape mode
3. Controls always visible in portrait mode

## License

This project is open source and available for educational and commercial use.

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

## Contact

For questions or support, please open an issue in the repository.
