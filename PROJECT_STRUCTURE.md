# Project Structure Documentation

## Overview
This document describes the refactored code organization for better readability, maintainability, and separation of concerns.

## New Folder Structure

```
app/src/main/java/com/example/video_player_example/
├── data/
│   └── VideoItem.kt                    # Data models and repository
├── ui/
│   ├── screens/                        # 📱 UI Screens (Composables)
│   │   ├── VlcPlayerScreen.kt         # VLC player UI
│   │   └── ExoPlayerScreen.kt         # ExoPlayer UI
│   ├── viewmodels/                     # 🧠 Business Logic
│   │   ├── VlcPlayerViewModel.kt      # VLC player logic & state
│   │   └── ExoPlayerViewModel.kt      # ExoPlayer logic & state
│   ├── utils/                          # 🛠️ Utility Functions
│   │   ├── FullscreenHelper.kt        # Fullscreen & rotation logic
│   │   └── PlayerDimensions.kt        # Responsive dimension calculations
│   ├── main/
│   │   └── MainScreen.kt              # Video list screen
│   └── theme/
│       └── ...                         # Material Design theme
└── MainActivity.kt                     # App entry point

## Deprecated (Old Structure)
├── ui/player/
│   ├── vlc_player/                     # ❌ Old location
│   │   ├── VlcPlayerScreen.kt         # ❌ Moved to ui/screens/
│   │   └── VlcPlayerViewModel.kt      # ❌ Moved to ui/viewmodels/
│   └── exo_player/                     # ❌ Old location
│       ├── RtspPlayerScreen.kt        # ❌ Moved to ui/screens/
│       └── PlayerViewModel.kt         # ❌ Moved to ui/viewmodels/
```

## Architecture Layers

### 1. **Screens Layer** (`ui/screens/`)
**Purpose**: Pure UI components (Composables) that display content and handle user interactions.

**Files**:
- `VlcPlayerScreen.kt` - VLC player UI with controls
- `ExoPlayerScreen.kt` - ExoPlayer UI with controls

**Responsibilities**:
- Render UI components
- Handle user input (button clicks, gestures)
- Display player controls and overlays
- Manage UI state (controls visibility, loading indicators)
- Delegate business logic to ViewModels

**Key Features**:
- Responsive layouts that adapt to screen size/orientation
- Material Design 3 components
- Auto-hiding controls in landscape mode
- Loading indicators and error states

---

### 2. **ViewModels Layer** (`ui/viewmodels/`)
**Purpose**: Business logic, state management, and player lifecycle management.

**Files**:
- `VlcPlayerViewModel.kt` - VLC player logic
- `ExoPlayerViewModel.kt` - ExoPlayer logic

**Responsibilities**:
- Initialize and configure media players
- Manage playback state (playing, paused, stopped)
- Handle media loading and buffering
- Implement error handling and recovery
- Manage player lifecycle (creation, disposal)
- Expose state to UI via Compose State

**Key Features**:
- **VLC**: LibVLC configuration, network resilience, codec support
- **ExoPlayer**: RTSP retry logic, stagnant stream detection, HLS support

---

### 3. **Utils Layer** (`ui/utils/`)
**Purpose**: Reusable utility functions and helpers shared across screens.

**Files**:
- `FullscreenHelper.kt` - Fullscreen mode and rotation management
- `PlayerDimensions.kt` - Responsive dimension calculations

**Responsibilities**:
- **FullscreenHelper**: 
  - Toggle fullscreen mode
  - Manage system UI visibility
  - Handle automatic screen rotation
  
- **PlayerDimensions**:
  - Calculate responsive padding values
  - Adapt control sizes to screen dimensions
  - Support both portrait and landscape orientations

---

## Benefits of New Structure

### ✅ **Separation of Concerns**
- **UI** (Screens) is separated from **Logic** (ViewModels)
- **Shared utilities** are extracted for reuse
- Each layer has a single, clear responsibility

### ✅ **Improved Readability**
- Screens are focused on UI composition
- ViewModels contain only business logic
- Utility functions are easy to locate

### ✅ **Better Maintainability**
- Changes to UI don't affect business logic
- Player logic can be modified independently
- Shared utilities prevent code duplication

### ✅ **Easier Testing**
- ViewModels can be unit tested without UI
- UI can be tested with mock ViewModels
- Utilities can be tested independently

### ✅ **Scalability**
- Easy to add new player types
- Simple to extend functionality
- Clear patterns for new features

---

## Migration Guide

### For Developers

**Old Import**:
```kotlin
import com.example.video_player_example.ui.player.vlc_player.VlcPlayerScreen
import com.example.video_player_example.ui.player.vlc_player.VlcPlayerViewModel
import com.example.video_player_example.ui.player.exo_player.RtspPlayerScreen
import com.example.video_player_example.ui.player.exo_player.PlayerViewModel
```

**New Import**:
```kotlin
// Screens
import com.example.video_player_example.ui.screens.VlcPlayerScreen
import com.example.video_player_example.ui.screens.ExoPlayerScreen

// ViewModels
import com.example.video_player_example.ui.viewmodels.VlcPlayerViewModel
import com.example.video_player_example.ui.viewmodels.ExoPlayerViewModel

// Utilities
import com.example.video_player_example.ui.utils.FullscreenHelper
import com.example.video_player_example.ui.utils.PlayerDimensions
```

**Function Name Changes**:
- `RtspPlayerScreen()` → `ExoPlayerScreen()` (renamed for clarity)
- `PlayerViewModel` → `ExoPlayerViewModel` (renamed for clarity)

---

## File Descriptions

### Screens

#### `VlcPlayerScreen.kt`
- **Purpose**: VLC player UI
- **Features**: Play/pause, volume control, fullscreen, back button
- **Dependencies**: `VlcPlayerViewModel`, `FullscreenHelper`, `PlayerDimensions`
- **Lines**: ~350 lines (UI only)

#### `ExoPlayerScreen.kt`
- **Purpose**: ExoPlayer UI
- **Features**: Play/pause, volume control, fullscreen, back button, buffering indicators
- **Dependencies**: `ExoPlayerViewModel`, `FullscreenHelper`, `PlayerDimensions`
- **Lines**: ~450 lines (UI + retry logic)

### ViewModels

#### `VlcPlayerViewModel.kt`
- **Purpose**: VLC player business logic
- **Key Methods**:
  - `playMedia(url)` - Start playback
  - `pausePlayback()` - Pause video
  - `resumePlayback()` - Resume video
  - `togglePlayPause()` - Toggle play/pause
  - `stopPlayback()` - Stop and cleanup
  - `setVolume(volume)` - Adjust volume
- **Configuration**: VLC options for network resilience, codec support
- **Lines**: ~230 lines

#### `ExoPlayerViewModel.kt`
- **Purpose**: ExoPlayer business logic
- **Key Methods**:
  - `ensurePlaying(url)` - Ensure stream is playing
  - `preloadStream(url)` - Preload stream
  - `stopPlayback()` - Stop playback
  - `reconnectRtsp(url)` - Reconnect RTSP stream
- **Configuration**: ExoPlayer buffer settings, RTSP configuration
- **Lines**: ~125 lines

### Utilities

#### `FullscreenHelper.kt`
- **Purpose**: Manage fullscreen mode and screen rotation
- **Key Method**: `toggleFullscreenWithRotation(context, enable, isPortrait)`
- **Features**:
  - Automatic landscape rotation when entering fullscreen
  - System UI visibility management
  - Transient system bars behavior
- **Lines**: ~50 lines

#### `PlayerDimensions.kt`
- **Purpose**: Calculate responsive dimensions for player controls
- **Key Method**: `PlayerDimensions.calculate(screenWidth, screenHeight, isPortrait)`
- **Returns**: Data class with all responsive dimension values
- **Features**:
  - Adapts to screen size
  - Different values for portrait/landscape
  - Percentage-based calculations
- **Lines**: ~70 lines

---

## Code Organization Principles

### 1. **Single Responsibility**
Each file has one clear purpose:
- Screens → UI rendering
- ViewModels → Business logic
- Utils → Shared functionality

### 2. **DRY (Don't Repeat Yourself)**
- Responsive dimension logic extracted to `PlayerDimensions`
- Fullscreen logic extracted to `FullscreenHelper`
- Both players use the same utilities

### 3. **Clear Dependencies**
- Screens depend on ViewModels and Utils
- ViewModels are independent (no UI dependencies)
- Utils are pure functions (no state)

### 4. **Consistent Naming**
- Screens end with `Screen`
- ViewModels end with `ViewModel`
- Helpers end with `Helper`
- Data classes are descriptive

---

## Future Enhancements

### Potential New Folders

#### `ui/components/`
Extract reusable UI components:
- `PlayerControls.kt` - Shared control bar component
- `VolumeSlider.kt` - Reusable volume slider
- `LoadingIndicator.kt` - Loading state component

#### `domain/`
Business logic layer (if app grows):
- `usecases/` - Use cases for video playback
- `repositories/` - Data access layer

#### `di/`
Dependency injection (if using Hilt/Koin):
- `AppModule.kt` - App-level dependencies
- `PlayerModule.kt` - Player-specific dependencies

---

## Best Practices

### When Adding New Features

1. **New Screen**: Add to `ui/screens/`
2. **New Business Logic**: Add to `ui/viewmodels/`
3. **Shared Utility**: Add to `ui/utils/`
4. **Data Model**: Add to `data/`

### When Modifying Existing Code

1. **UI Changes**: Modify screen files only
2. **Logic Changes**: Modify ViewModel files only
3. **Shared Behavior**: Modify utility files

### Code Review Checklist

- [ ] Is the file in the correct folder?
- [ ] Does it follow single responsibility principle?
- [ ] Are dependencies clear and minimal?
- [ ] Is naming consistent with conventions?
- [ ] Is documentation/KDoc present?

---

## Summary

The refactored structure provides:
- **Clear separation** between UI and logic
- **Reusable utilities** for common functionality
- **Better organization** for easier navigation
- **Improved maintainability** for future development
- **Consistent patterns** across the codebase

This structure scales well as the app grows and makes it easier for new developers to understand the codebase.
