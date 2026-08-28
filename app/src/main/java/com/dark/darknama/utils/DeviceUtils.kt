package com.dark.darknama.utils

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.util.DisplayMetrics
import android.view.WindowManager

object DeviceUtils {
    /**
     * Check if the device is a TV
     */
    fun isTv(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }
    
    /**
     * Check if the device is a tablet
     */
    fun isTablet(context: Context): Boolean {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val width = display.width
        val height = display.height
        val density = context.resources.displayMetrics.density
        val dpHeight = height / density
        val dpWidth = width / density
        val smallestWidth = Math.min(dpWidth, dpHeight)
        return smallestWidth >= 600
    }
    
    /**
     * Get the number of grid columns based on screen size and device type
     */
    fun getGridColumns(resources: Resources): Int {
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        val isTv = resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION
        
        return when {
            isTv -> 5 // More columns for TV screens
            screenWidthDp >= 600 -> 4 // Tablets
            else -> 2 // Phones
        }
    }
}