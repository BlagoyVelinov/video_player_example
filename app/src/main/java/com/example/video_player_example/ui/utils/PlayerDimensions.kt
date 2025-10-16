package com.example.video_player_example.ui.utils

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Data class containing responsive dimensions for player controls.
 * All dimensions adapt based on screen size and orientation.
 */
data class PlayerDimensions(
    val playButtonBottomPadding: Dp,
    val playButtonStartPadding: Dp,
    val playButtonEndPadding: Dp,

    val controlBarBottomPadding: Dp,
    val controlBarStartPadding: Dp,
    val controlBarHorizontalPadding: Dp,
    val controlBarVerticalPadding: Dp,
    val controlBarWidthFraction: Float,

    val volumeSliderWidth: Dp,
    val spacerWidth: Dp,

    val backButtonTopPadding: Dp,
    val backButtonStartPadding: Dp
) {
    companion object {

        fun calculate(
            screenWidth: Dp,
            screenHeight: Dp,
            isPortrait: Boolean
        ): PlayerDimensions {
            return PlayerDimensions(
                playButtonBottomPadding = if (isPortrait) screenHeight * 0.0127f else screenHeight * 0.025f,
                playButtonStartPadding = if (isPortrait) screenWidth * 0.08f else screenWidth * 0.12f,
                playButtonEndPadding = if (isPortrait) 0.dp else screenWidth * 0.04f,

                controlBarBottomPadding = if (isPortrait) screenHeight * 0.002f else 0.dp,
                controlBarStartPadding = if (isPortrait) screenWidth * 0.12f else screenWidth * 0.08f,
                controlBarHorizontalPadding = screenWidth * 0.03f,
                controlBarVerticalPadding = if (isPortrait) screenHeight * 0.01f else screenHeight * 0.025f,
                controlBarWidthFraction = if (isPortrait) 0.9f else 0.8f,

                volumeSliderWidth = if (isPortrait) screenWidth * 0.28f else screenWidth * 0.15f,
                spacerWidth = screenWidth * 0.02f,

                backButtonTopPadding = if (isPortrait) screenHeight * 0.01f else screenHeight * 0.05f,
                backButtonStartPadding = if (isPortrait) screenWidth * 0.03f else screenWidth * 0.08f
            )
        }
    }
}
