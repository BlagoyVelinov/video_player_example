package com.example.video_player_example.ui.utils

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Helper object for managing fullscreen mode and screen orientation in video players.
 */
object FullscreenHelper {

    fun toggleFullscreenWithRotation(
        context: Context,
        enable: Boolean,
        isCurrentlyPortrait: Boolean
    ) {
        val activity = context as? Activity ?: return

        if (enable) {
            if (isCurrentlyPortrait) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, !enable)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        
        if (enable) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
