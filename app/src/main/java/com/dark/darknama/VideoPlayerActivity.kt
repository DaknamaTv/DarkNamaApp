package com.dark.darknama

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Typeface
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.FrameLayout
import kotlin.math.abs
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.dark.darknama.data.model.SubtitleSettings
import com.dark.darknama.data.model.VideoPlayerSettings
import com.dark.darknama.data.model.FontSettings
import com.dark.darknama.data.model.WatchedEpisode
import com.dark.darknama.utils.StorageUtils
import com.dark.darknama.ui.theme.FontManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Applies the user's subtitle settings (size + colours + font) to the PlayerView.
 *
 * IMPORTANT: media3's SubtitleView honours the *embedded* styles/sizes that come
 * with the subtitle track by default, which silently overrides any size we set.
 * `setApplyEmbeddedStyles(false)` / `setApplyEmbeddedFontSizes(false)` must be
 * disabled first, otherwise the "text size" setting appears to do nothing.
 */
fun PlayerView.applySubtitleSettings(settings: SubtitleSettings, typeface: Typeface? = null) {
    val sv = subtitleView ?: return

    // 1) Stop the track's own styling from overriding ours.
    sv.setApplyEmbeddedStyles(false)
    sv.setApplyEmbeddedFontSizes(false)

    // 2) Size - convert sp to px against the current display metrics.
    val pixels = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        settings.textSize,
        context.resources.displayMetrics
    )
    sv.setFixedTextSize(TypedValue.COMPLEX_UNIT_PX, pixels)

    // 3) Colours + typeface.
    // borderColor doubles as the subtitle background/outline colour:
    //  - fully transparent -> no background, draw a black outline for legibility
    //  - otherwise         -> use it as the background box behind the text
    val isTransparentBackground = android.graphics.Color.alpha(settings.borderColor) == 0
    val style = CaptionStyleCompat(
        settings.textColor,
        if (isTransparentBackground) android.graphics.Color.TRANSPARENT else settings.borderColor,
        android.graphics.Color.TRANSPARENT, // window colour
        if (isTransparentBackground) CaptionStyleCompat.EDGE_TYPE_OUTLINE else CaptionStyleCompat.EDGE_TYPE_NONE,
        android.graphics.Color.BLACK,
        typeface
    )
    sv.setStyle(style)
}

// Kept for backwards compatibility with existing call sites.
fun PlayerView.setSubtitleTextSize(spSize: Float) {
    val pixels = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        spSize,
        context.resources.displayMetrics
    )
    subtitleView?.apply {
        setApplyEmbeddedStyles(false)
        setApplyEmbeddedFontSizes(false)
        setFixedTextSize(TypedValue.COMPLEX_UNIT_PX, pixels)
    }
}

class VideoPlayerActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.dark.darknama.utils.LanguageUtils.wrapContext(newBase))
    }

    companion object {
        const val EXTRA_VIDEO_URL = "video_url"
        const val EXTRA_SERIES_ID = "series_id"
        const val EXTRA_SEASON_ID = "season_id"
        const val EXTRA_EPISODE_ID = "episode_id"
        const val EXTRA_USER_AGENT = "user_agent"
        const val EXTRA_REFERER = "referer"
        const val REQUEST_WRITE_SETTINGS = 1001
        
        fun start(context: Context, videoUrl: String) {
            val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_URL, videoUrl)
            }
            context.startActivity(intent)
        }
        
        /**
         * Starts the player for a live TV stream, optionally with the custom
         * HTTP User-Agent / Referer headers required by some IPTV streams.
         */
        fun startLiveTv(context: Context, videoUrl: String, userAgent: String? = null, referer: String? = null) {
            val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_URL, videoUrl)
                if (!userAgent.isNullOrBlank()) putExtra(EXTRA_USER_AGENT, userAgent)
                if (!referer.isNullOrBlank()) putExtra(EXTRA_REFERER, referer)
            }
            context.startActivity(intent)
        }
        
        fun startWithEpisodeInfo(context: Context, videoUrl: String, seriesId: Int, seasonId: Int, episodeId: Int) {
            val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_URL, videoUrl)
                putExtra(EXTRA_SERIES_ID, seriesId)
                putExtra(EXTRA_SEASON_ID, seasonId)
                putExtra(EXTRA_EPISODE_ID, episodeId)
            }
            context.startActivity(intent)
        }
    }
    
    private var exoPlayer: ExoPlayer? = null
    private var videoUrl: String? = null
    private var seriesId: Int? = null
    private var seasonId: Int? = null
    private var episodeId: Int? = null
    private var streamUserAgent: String? = null
    private var streamReferer: String? = null
    private var playerInitialized = false
    private var isActivityResumed = false
    private var hasMarkedAsWatched = false
    
    // Compose-observable states shared with the composable so that
    // an Android TV remote can show controls / open the settings dialog.
    private val showControlsState = mutableStateOf(true)
    private val showSettingsDialogState = mutableStateOf(false)
    private var remoteSeekTimeSeconds = 10
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set fullscreen landscape mode
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        // Enable immersive full-screen mode
        enableFullScreenMode()
        
        // Keep screen on while in video player
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Load the user-configured seek time so the TV remote uses the same value
        remoteSeekTimeSeconds = try {
            StorageUtils.loadVideoPlayerSettings(this).seekTimeSeconds
        } catch (e: Exception) {
            10
        }
        
        videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL)
        seriesId = intent.getIntExtra(EXTRA_SERIES_ID, -1).takeIf { it != -1 }
        seasonId = intent.getIntExtra(EXTRA_SEASON_ID, -1).takeIf { it != -1 }
        episodeId = intent.getIntExtra(EXTRA_EPISODE_ID, -1).takeIf { it != -1 }
        streamUserAgent = intent.getStringExtra(EXTRA_USER_AGENT)
        streamReferer = intent.getStringExtra(EXTRA_REFERER)
        
        if (videoUrl != null) {
            setContent {
                VideoPlayerScreen(
                    videoUrl = videoUrl!!, 
                    seriesId = seriesId,
                    seasonId = seasonId,
                    episodeId = episodeId,
                    userAgent = streamUserAgent,
                    referer = streamReferer,
                    externalShowControls = showControlsState,
                    externalShowSettingsDialog = showSettingsDialogState,
                    onBack = this::finish
                ) { player ->
                    exoPlayer = player
                    playerInitialized = true
                }
            }
        } else {
            finish()
        }
    }
    
    // Handle TV remote control key events
    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        try {
            // While the settings dialog is open, let the dialog window handle keys
            if (showSettingsDialogState.value) {
                return super.onKeyDown(keyCode, event)
            }
            exoPlayer?.let { player ->
                when (keyCode) {
                    android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                    android.view.KeyEvent.KEYCODE_DPAD_CENTER -> {
                        player.playWhenReady = !player.playWhenReady
                        showControlsState.value = true
                        return true
                    }
                    android.view.KeyEvent.KEYCODE_MEDIA_PLAY -> {
                        player.playWhenReady = true
                        return true
                    }
                    android.view.KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        player.playWhenReady = false
                        return true
                    }
                    android.view.KeyEvent.KEYCODE_DPAD_LEFT,
                    android.view.KeyEvent.KEYCODE_MEDIA_REWIND -> {
                        val seekMs = remoteSeekTimeSeconds * 1000L
                        val newPosition = (player.currentPosition - seekMs).coerceAtLeast(0L)
                        player.seekTo(newPosition)
                        showControlsState.value = true
                        return true
                    }
                    android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
                    android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                        val seekMs = remoteSeekTimeSeconds * 1000L
                        val newPosition = (player.currentPosition + seekMs).coerceAtMost(player.duration)
                        player.seekTo(newPosition)
                        showControlsState.value = true
                        return true
                    }
                    android.view.KeyEvent.KEYCODE_DPAD_UP,
                    android.view.KeyEvent.KEYCODE_MENU -> {
                        // Open the player settings dialog (subtitles / audio / speed / style)
                        showControlsState.value = true
                        showSettingsDialogState.value = true
                        return true
                    }
                    android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                        // Toggle on-screen controls with the remote
                        showControlsState.value = !showControlsState.value
                        return true
                    }
                    android.view.KeyEvent.KEYCODE_BACK -> {
                        if (showControlsState.value) {
                            showControlsState.value = false
                        } else {
                            finish()
                        }
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore key event errors
        }
        return super.onKeyDown(keyCode, event)
    }
    
    private fun enableFullScreenMode() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // For Android 11 and above
                window.insetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // For Android 4.4 to Android 10
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
            } else {
                // For even older versions
                @Suppress("DEPRECATION")
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
        } catch (e: Exception) {
            // Fallback to basic fullscreen if there's an issue
            try {
                @Suppress("DEPRECATION")
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
            } catch (e2: Exception) {
                // Ignore fullscreen errors
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            exoPlayer?.release()
        } catch (e: Exception) {
            // Ignore any exceptions during release
        }
        exoPlayer = null
        playerInitialized = false
        
        // Remove keep screen on flag to conserve battery
        try {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } catch (e: Exception) {
            // Ignore flag clear errors
        }
    }
    
    override fun onResume() {
        super.onResume()
        isActivityResumed = true
        // Re-enable full-screen mode when resuming
        try {
            enableFullScreenMode()
        } catch (e: Exception) {
            // Ignore fullscreen errors
        }
        
        // Player will remain paused until user manually starts it
        try {
            if (playerInitialized && exoPlayer != null) {
                // Keep player paused - let user manually start playback
                exoPlayer?.playWhenReady = false
            }
        } catch (e: Exception) {
            // Ignore player state errors
        }
    }
    
    override fun onPause() {
        super.onPause()
        isActivityResumed = false
        
        // Stop player completely when activity pauses (app switch or screen off)
        try {
            if (playerInitialized && exoPlayer != null) {
                exoPlayer?.playWhenReady = false
                // Note: currentPosition is managed in the Composable scope
                // Player will remain paused until user manually starts it
            }
        } catch (e: Exception) {
            // Ignore player stop errors
        }
    }
}

@Composable
fun VideoPlayerScreen(
    videoUrl: String,
    seriesId: Int?,
    seasonId: Int?,
    episodeId: Int?,
    userAgent: String? = null,
    referer: String? = null,
    externalShowControls: androidx.compose.runtime.MutableState<Boolean>? = null,
    externalShowSettingsDialog: androidx.compose.runtime.MutableState<Boolean>? = null,
    onBack: () -> Unit,
    onPlayerReady: (ExoPlayer) -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    val showControlsHolder = externalShowControls ?: remember { mutableStateOf(true) }
    var showControls by showControlsHolder
    var isSeeking by remember { mutableStateOf(false) }
    var playerError by remember { mutableStateOf<String?>(null) }
    var isRetrying by remember { mutableStateOf(false) }
    var showForwardIndicator by remember { mutableStateOf(false) }
    var showRewindIndicator by remember { mutableStateOf(false) }
    var wasPlayingBeforeSeek by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var showSpeedDropdown by remember { mutableStateOf(false) }
    var playerInitialized by remember { mutableStateOf(false) }
    var hasMarkedAsWatched by remember { mutableStateOf(false) }
    
    // Track selection state
    val showTrackSelectionDialogHolder = externalShowSettingsDialog ?: remember { mutableStateOf(false) }
    var showTrackSelectionDialog by showTrackSelectionDialogHolder
    var currentTracks by remember { mutableStateOf(Tracks.EMPTY) }
    var trackSelector by remember { mutableStateOf<DefaultTrackSelector?>(null) }
    
    // Predefined playback speed options
    val speedOptions = remember {
        listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f, 3.5f)
    }
    
    // Load font settings
    val fontSettings = remember(context) {
        try {
            StorageUtils.loadFontSettings(context)
        } catch (e: Exception) {
            com.dark.darknama.data.model.FontSettings.DEFAULT
        }
    }
    
    // Load custom font typeface
    val customTypeface = remember(fontSettings.fontType) {
        try {
            when (fontSettings.fontType) {
                com.dark.darknama.data.model.FontType.DEFAULT -> null
                com.dark.darknama.data.model.FontType.VAZIRMATN -> {
                    try {
                        // Load the Vazirmatn font from assets
                        Typeface.createFromAsset(context.assets, "font/vazirmatn_regular.ttf")
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    // Load video player settings (without affecting playback speed)
    val videoPlayerSettings = remember(context) {
        try {
            StorageUtils.loadVideoPlayerSettings(context)
        } catch (e: Exception) {
            com.dark.darknama.data.model.VideoPlayerSettings.DEFAULT
        }
    }
    
    // Load subtitle settings (mutable so they can be changed from the in-player settings dialog)
    var subtitleSettings by remember(context) {
        mutableStateOf(
            try {
                StorageUtils.loadSubtitleSettings(context)
            } catch (e: Exception) {
                SubtitleSettings.getDefaultSettings(context)
            }
        )
    }
    
    // Update and persist subtitle settings so the change is shared with the Settings screen
    fun updateSubtitleSettings(newSettings: SubtitleSettings) {
        subtitleSettings = newSettings
        try {
            StorageUtils.saveSubtitleSettings(context, newSettings)
        } catch (e: Exception) {
            // Ignore storage errors
        }
    }
    
    val exoPlayer = remember(context) {
        try {
            // Create track selector for track selection
            val selector = DefaultTrackSelector(context)
            trackSelector = selector
            
            // Build a media source factory with custom HTTP headers when needed
            // (many IPTV live streams require a specific User-Agent and/or Referer)
            val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(30000)
                .setReadTimeoutMs(30000)
            if (!userAgent.isNullOrBlank()) {
                httpDataSourceFactory.setUserAgent(userAgent)
            }
            if (!referer.isNullOrBlank()) {
                httpDataSourceFactory.setDefaultRequestProperties(mapOf("Referer" to referer))
            }
            val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)
            val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
            
            ExoPlayer.Builder(context)
                .setTrackSelector(selector)
                .setMediaSourceFactory(mediaSourceFactory)
                .build().apply {
                    try {
                        setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
                        prepare()
                        // If we're retrying, seek to the current position
                        if (isRetrying && currentPosition > 0) {
                            seekTo(currentPosition)
                        }
                        playWhenReady = isPlaying // Start with current play state
                        // Set initial playback speed
                        setPlaybackSpeed(playbackSpeed)
                    } catch (e: Exception) {
                        // Don't show error, just mark as retrying
                        isRetrying = true
                    }
                }
        } catch (e: Exception) {
            // Don't show error, just mark as retrying
            isRetrying = true
            null
        }
    }
    
    // Listen to track changes
    val trackListener = remember(exoPlayer) {
        object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                currentTracks = tracks
            }
        }
    }
    
    LaunchedEffect(exoPlayer) {
        if (exoPlayer == null) return@LaunchedEffect
        
        try {
            exoPlayer.addListener(trackListener)
        } catch (e: Exception) {
            // Ignore listener errors
        }
    }
    
    DisposableEffect(exoPlayer) {
        onDispose {
            try {
                exoPlayer?.removeListener(trackListener)
            } catch (e: Exception) {
                // Ignore listener removal errors
            }
        }
    }
    
    // Notify activity of player reference
    LaunchedEffect(Unit) {
        try {
            exoPlayer?.let { onPlayerReady(it) }
            playerInitialized = true
        } catch (e: Exception) {
            // Ignore callback errors
        }
    }
    
    // Update player state and mark episode as watched
    LaunchedEffect(isPlaying, exoPlayer) {
        try {
            exoPlayer?.playWhenReady = isPlaying
            
            // Mark episode as watched when playback starts (only once)
            if (isPlaying && !hasMarkedAsWatched && seriesId != null && seasonId != null && episodeId != null) {
                try {
                    val watchedEpisode = WatchedEpisode(
                        seriesId = seriesId!!,
                        seasonId = seasonId!!,
                        episodeId = episodeId!!
                    )
                    StorageUtils.saveWatchedEpisode(context, watchedEpisode)
                    hasMarkedAsWatched = true
                } catch (e: Exception) {
                    // Ignore storage errors
                }
            }
        } catch (e: Exception) {
            // Ignore player state errors
        }
    }
    
    // Update playback speed when it changes
    LaunchedEffect(playbackSpeed, exoPlayer) {
        try {
            exoPlayer?.setPlaybackSpeed(playbackSpeed)
        } catch (e: Exception) {
            // Ignore playback speed errors
        }
    }
    
    // Listen to player events and handle cleanup
    val playerListener = remember(exoPlayer) {
        object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                // Only update isPlaying if we're not currently seeking
                if (!isSeeking) {
                    isPlaying = playing
                }
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                try {
                    if (playbackState == Player.STATE_READY) {
                        duration = exoPlayer?.duration ?: 0L
                        
                        // After the player is ready (especially after a retry), 
                        // ensure the playWhenReady state is consistent with our UI state
                        if (exoPlayer != null && !isRetrying) {
                            exoPlayer?.playWhenReady = isPlaying
                        }
                    } else if (playbackState == Player.STATE_ENDED) {
                        // Video ended, pause the player
                        isPlaying = false
                    }
                } catch (e: Exception) {
                    // Ignore duration errors
                }
            }
            
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                try {
                    if (!isSeeking) {
                        currentPosition = exoPlayer?.currentPosition ?: 0L
                    }
                } catch (e: Exception) {
                    // Ignore position errors
                }
            }
            
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                // Don't show error message, just mark as retrying
                isRetrying = true
                playerError = error.message
                
                // Store current position before retrying
                val retryPosition = currentPosition
                val wasPlaying = isPlaying // Store whether it was playing before the error
                
                // Attempt to retry after a delay
                CoroutineScope(Dispatchers.Main).launch {
                    delay(3000) // Wait 3 seconds before retrying
                    try {
                        exoPlayer?.let { player ->
                            // Retry loading the media
                            player.setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
                            player.prepare()
                            // Seek to the stored position after preparing
                            player.seekTo(retryPosition)
                            
                            // Resume playback if it was playing before the error
                            player.playWhenReady = wasPlaying
                            
                            // Update the UI state to match the player state
                            isPlaying = wasPlaying
                            isRetrying = false
                            playerError = null
                        }
                    } catch (e: Exception) {
                        // If retry fails, keep isRetrying true
                    }
                }
            }
        }
    }
    
    LaunchedEffect(exoPlayer) {
        if (exoPlayer == null) return@LaunchedEffect
        
        try {
            exoPlayer.addListener(playerListener)
        } catch (e: Exception) {
            // Ignore listener errors
        }
    }
    
    DisposableEffect(exoPlayer) {
        onDispose {
            try {
                exoPlayer?.removeListener(playerListener)
            } catch (e: Exception) {
                // Ignore listener removal errors
            }
        }
    }
    
    // Periodically update the current position for real-time progress tracking
    LaunchedEffect(exoPlayer, isPlaying) {
        if (exoPlayer == null) return@LaunchedEffect
        
        try {
            while (true) {
                delay(100) // Update every 100ms for smooth progress tracking
                if (isPlaying && !isSeeking) {
                    try {
                        exoPlayer?.let { player ->
                            if (player.isPlaying) {
                                currentPosition = player.currentPosition
                                duration = player.duration
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore position/duration errors
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore coroutine errors
        }
    }
    
    // Hide controls after a delay
    LaunchedEffect(showControls, isPlaying) {
        try {
            if (showControls && isPlaying) {
                delay(3000) // Hide controls after 3 seconds
                showControls = false
            }
        } catch (e: Exception) {
            // Ignore delay errors
        }
    }
    
    // Hide forward/rewind indicators after a delay
    LaunchedEffect(showForwardIndicator) {
        try {
            if (showForwardIndicator) {
                delay(500) // Hide after 500ms
                showForwardIndicator = false
            }
        } catch (e: Exception) {
            // Ignore delay errors
        }
    }
    
    LaunchedEffect(showRewindIndicator) {
        try {
            if (showRewindIndicator) {
                delay(500) // Hide after 500ms
                showRewindIndicator = false
            }
        } catch (e: Exception) {
            // Ignore delay errors
        }
    }
    
    // Clean up player
    DisposableEffect(exoPlayer) {
        onDispose {
            try {
                exoPlayer?.release()
            } catch (e: Exception) {
                // Ignore release errors
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                try {
                    detectTapGestures(
                        onDoubleTap = { offset -> 
                            // Calculate if the tap is on the left or right side
                            val screenWidth = size.width
                            val tapX = offset.x
                            
                            // Store the playing state before seeking
                            wasPlayingBeforeSeek = isPlaying
                            isSeeking = true
                            
                            if (tapX < screenWidth / 2) {
                                // Left side - rewind specified seconds
                                try {
                                    exoPlayer?.let { player ->
                                        val seekTimeMs = videoPlayerSettings.seekTimeSeconds * 1000L
                                        val newPosition = (player.currentPosition - seekTimeMs).coerceAtLeast(0L)
                                        player.seekTo(newPosition)
                                        currentPosition = newPosition
                                        showRewindIndicator = true
                                        // Keep the player playing during seeking if it was playing before
                                        if (wasPlayingBeforeSeek) {
                                            player.playWhenReady = true
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Ignore seek errors
                                }
                            } else {
                                // Right side - forward specified seconds
                                try {
                                    exoPlayer?.let { player ->
                                        val seekTimeMs = videoPlayerSettings.seekTimeSeconds * 1000L
                                        val newPosition = (player.currentPosition + seekTimeMs).coerceAtMost(player.duration)
                                        player.seekTo(newPosition)
                                        currentPosition = newPosition
                                        showForwardIndicator = true
                                        // Keep the player playing during seeking if it was playing before
                                        if (wasPlayingBeforeSeek) {
                                            player.playWhenReady = true
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Ignore seek errors
                                }
                            }
                            
                            // Reset seeking state after a short delay using a coroutine scope
                            CoroutineScope(Dispatchers.Main).launch {
                                try {
                                    delay(500) // Reset after 500ms
                                    isSeeking = false
                                    // Restore the playing state after seeking is finished
                                    try {
                                        exoPlayer?.playWhenReady = wasPlayingBeforeSeek
                                        // Update isPlaying state to match the player's actual state
                                        isPlaying = wasPlayingBeforeSeek
                                    } catch (e: Exception) {
                                        // Ignore errors
                                    }
                                } catch (e: Exception) {
                                    // Ignore delay errors
                                }
                            }
                        },
                        onTap = {
                            showControls = !showControls
                            // Reset the auto-hide timer when controls are shown
                            if (showControls && isPlaying) {
                                // The LaunchedEffect above will handle the auto-hide
                            }
                        }
                    )
                } catch (e: Exception) {
                    // Ignore gesture detection errors
                }
            }
    ) {
        // Check if player is initialized
        if (exoPlayer == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.initializing_player),
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }
            return@Box
        }
        
        // Video player
        AndroidView(
            factory = { ctx ->
                try {
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false // We're using our own controls
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        // Make the player view fill the entire screen
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        
                        // Apply subtitle settings (size + colours + font) to the player view
                        applySubtitleSettings(subtitleSettings, customTypeface)
                    }
                } catch (e: Exception) {
                    // Return a simple view if PlayerView fails to initialize
                    View(ctx).apply {
                        setBackgroundColor(android.graphics.Color.BLACK)
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { playerView ->
                try {
                    // Re-apply subtitle styling whenever the settings change so
                    // changes made in the in-player dialog are visible instantly.
                    if (playerView is PlayerView) {
                        playerView.applySubtitleSettings(subtitleSettings, customTypeface)
                    }
                } catch (e: Exception) {
                    // Ignore update errors
                }
            }
        )
        
        // Rewind indicator
        if (showRewindIndicator) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Rewind ${videoPlayerSettings.seekTimeSeconds} seconds",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "${videoPlayerSettings.seekTimeSeconds}s",
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        
        // Forward indicator
        if (showForwardIndicator) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward,
                        contentDescription = "Forward ${videoPlayerSettings.seekTimeSeconds} seconds",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "${videoPlayerSettings.seekTimeSeconds}s",
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        
        // Custom controls overlay
        if (showControls) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Top bar with back button and settings
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.7f),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    
                    // Settings button in top right corner
                    IconButton(
                        onClick = { showTrackSelectionDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.7f),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                            .align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                }
                
                // Middle play/pause button
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.7f),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                // Bottom controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(16.dp)
                ) {
                    // Progress slider with retry animation
                    if (isRetrying) {
                        // Show animated progress bar when retrying
                        androidx.compose.material3.LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    } else {
                        Slider(
                            value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                            onValueChange = { progress ->
                                // Store the playing state before seeking
                                if (!isSeeking) {
                                    wasPlayingBeforeSeek = isPlaying
                                }
                                isSeeking = true
                                val newPosition = (progress * duration).toLong()
                                try {
                                    exoPlayer?.seekTo(newPosition)
                                    currentPosition = newPosition
                                    // Keep the player playing during seeking if it was playing before
                                    if (wasPlayingBeforeSeek) {
                                        exoPlayer?.playWhenReady = true
                                    }
                                } catch (e: Exception) {
                                    // Ignore seek errors
                                }
                            },
                            onValueChangeFinished = {
                                isSeeking = false
                                // Restore the playing state after seeking is finished
                                try {
                                    exoPlayer?.playWhenReady = wasPlayingBeforeSeek
                                    // Update isPlaying state to match the player's actual state
                                    isPlaying = wasPlayingBeforeSeek
                                } catch (e: Exception) {
                                    // Ignore errors
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Time and controls row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontManager.loadFontFamily(context, fontSettings.fontType)
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Retry button when there's an error
                        if (isRetrying) {
                            Text(
                                text = androidx.compose.ui.res.stringResource(R.string.retrying),
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .clickable { 
                                        // Manual retry
                                        try {
                                            exoPlayer?.let { player ->
                                                // Store current position and playback state before retrying
                                                val retryPosition = currentPosition
                                                val wasPlaying = isPlaying // Store whether it was playing before the retry
                                                player.setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
                                                player.prepare()
                                                // Seek to the stored position after preparing
                                                player.seekTo(retryPosition)
                                                // Resume playback if it was playing before the retry
                                                player.playWhenReady = wasPlaying
                                                // Update the UI state to match the player state
                                                isPlaying = wasPlaying
                                                isRetrying = false
                                                playerError = null
                                            }
                                        } catch (e: Exception) {
                                            // If manual retry fails, keep isRetrying true
                                        }
                                    }
                                    .padding(horizontal = 8.dp)
                            )
                        }
                        
                        // Video speed controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            // Tapping the speed chip opens a small popup dialog
                            // listing every speed option (a DropdownMenu did not
                            // show reliably above the full-screen player, and was
                            // unreachable with a TV remote).
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { showSpeedDropdown = true }
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Playback speed",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )

                                Text(
                                    text = String.format("%.2fx", playbackSpeed),
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    fontFamily = FontManager.loadFontFamily(context, fontSettings.fontType)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            // Normal speed button
                            Text(
                                text = androidx.compose.ui.res.stringResource(R.string.normal),
                                color = if (playbackSpeed == 1.0f) MaterialTheme.colorScheme.primary else Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (playbackSpeed == 1.0f) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier
                                    .clickable { playbackSpeed = 1.0f }
                                    .padding(4.dp),
                                fontFamily = FontManager.loadFontFamily(context, fontSettings.fontType)
                            )
                        }
                        
                        Text(
                            text = formatTime(duration),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontManager.loadFontFamily(context, fontSettings.fontType)
                        )
                    }
                }
            }
        }
        
        // Quick playback-speed popup opened from the bottom control bar
        if (showSpeedDropdown) {
            OptionPickerDialog(
                title = androidx.compose.ui.res.stringResource(R.string.playback_speed),
                options = speedOptions.map { speed ->
                    PickerOption(
                        label = String.format("%.2fx", speed),
                        isSelected = speed == playbackSpeed,
                        onSelect = { playbackSpeed = speed }
                    )
                },
                onDismiss = { showSpeedDropdown = false }
            )
        }
        
        // Player settings dialog (tracks + subtitle style + speed)
        if (showTrackSelectionDialog) {
            TrackSelectionDialog(
                tracks = currentTracks,
                trackSelector = trackSelector,
                subtitleSettings = subtitleSettings,
                onSubtitleSettingsChanged = { updateSubtitleSettings(it) },
                playbackSpeed = playbackSpeed,
                speedOptions = speedOptions,
                onPlaybackSpeedChanged = { playbackSpeed = it },
                onDismiss = { showTrackSelectionDialog = false }
            )
        }
    }
}

/** A single row inside [OptionPickerDialog]. */
data class PickerOption(
    val label: String,
    val isSelected: Boolean,
    val onSelect: () -> Unit
)

/**
 * A small popup dialog listing selectable options.
 *
 * Replaces the `DropdownMenu`s that were used for playback speed /
 * audio track / subtitle track: those did not render reliably on top of the
 * full-screen player and could not be focused with an Android TV remote.
 */
@Composable
fun OptionPickerDialog(
    title: String,
    options: List<PickerOption>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                options.forEach { option ->
                    OptionPickerRow(
                        label = option.label,
                        isSelected = option.isSelected,
                        onClick = {
                            option.onSelect()
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(androidx.compose.ui.res.stringResource(R.string.close))
            }
        }
    )
}

@Composable
private fun OptionPickerRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    isFocused -> MaterialTheme.colorScheme.surfaceVariant
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected || isFocused)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Player settings dialog: audio track, subtitle track, playback speed and
 * subtitle styling.
 *
 * Each selectable list opens as its own small popup ([OptionPickerDialog])
 * instead of an inline DropdownMenu, so the options actually appear over the
 * full-screen player and can be reached with an Android TV remote.
 */
@Composable
fun TrackSelectionDialog(
    tracks: Tracks,
    trackSelector: DefaultTrackSelector?,
    subtitleSettings: SubtitleSettings = SubtitleSettings.DEFAULT,
    onSubtitleSettingsChanged: (SubtitleSettings) -> Unit = {},
    playbackSpeed: Float = 1.0f,
    speedOptions: List<Float> = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f),
    onPlaybackSpeedChanged: (Float) -> Unit = {},
    onDismiss: () -> Unit
) {
    val noneLabel = androidx.compose.ui.res.stringResource(R.string.none)

    val audioTrackGroups = remember(tracks) {
        tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
    }

    val textTrackGroups = remember(tracks) {
        tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
    }

    // Which sub-popup is currently open
    var showAudioPicker by remember { mutableStateOf(false) }
    var showSubtitlePicker by remember { mutableStateOf(false) }
    var showSpeedPicker by remember { mutableStateOf(false) }

    /** Human readable name for a track format. */
    fun trackName(format: androidx.media3.common.Format, fallback: String): String {
        val label = format.label
        if (!label.isNullOrBlank()) return label
        val language = format.language
        if (!language.isNullOrBlank()) {
            return try {
                val locale = java.util.Locale(language)
                locale.displayLanguage.ifBlank { language }
            } catch (e: Exception) {
                language
            }
        }
        return fallback
    }

    val currentAudioSelection = remember(audioTrackGroups) {
        audioTrackGroups.firstOrNull { it.isSelected }?.let { group ->
            (0 until group.length).firstOrNull { group.isTrackSelected(it) }?.let { index ->
                trackName(group.getTrackFormat(index), "Track ${index + 1}")
            }
        } ?: noneLabel
    }

    val currentSubtitleSelection = remember(textTrackGroups) {
        textTrackGroups.firstOrNull { it.isSelected }?.let { group ->
            (0 until group.length).firstOrNull { group.isTrackSelected(it) }?.let { index ->
                trackName(group.getTrackFormat(index), "Subtitle ${index + 1}")
            }
        } ?: noneLabel
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.player_settings_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // ---------- Audio track ----------
                if (audioTrackGroups.isNotEmpty()) {
                    PlayerSettingRow(
                        title = androidx.compose.ui.res.stringResource(R.string.audio_tracks),
                        value = currentAudioSelection,
                        onClick = { showAudioPicker = true }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ---------- Subtitle track ----------
                if (textTrackGroups.isNotEmpty()) {
                    PlayerSettingRow(
                        title = androidx.compose.ui.res.stringResource(R.string.subtitles),
                        value = currentSubtitleSelection,
                        onClick = { showSubtitlePicker = true }
                    )
                } else {
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.no_subtitles_available),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ---------- Playback speed ----------
                PlayerSettingRow(
                    title = androidx.compose.ui.res.stringResource(R.string.playback_speed),
                    value = String.format("%.2fx", playbackSpeed),
                    onClick = { showSpeedPicker = true }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ---------- Subtitle styling ----------
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.subtitle_settings),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // Subtitle text size, adjusted with +/- so a TV remote works
                Text(
                    text = androidx.compose.ui.res.stringResource(
                        R.string.subtitle_text_size,
                        subtitleSettings.textSize.toInt()
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SubtitleSizeButton(
                        symbol = "−",
                        enabled = subtitleSettings.textSize > 10f,
                        onClick = {
                            onSubtitleSettingsChanged(
                                subtitleSettings.copy(
                                    textSize = (subtitleSettings.textSize - 1f).coerceIn(10f, 60f)
                                )
                            )
                        }
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${subtitleSettings.textSize.toInt()} sp",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    SubtitleSizeButton(
                        symbol = "+",
                        enabled = subtitleSettings.textSize < 60f,
                        onClick = {
                            onSubtitleSettingsChanged(
                                subtitleSettings.copy(
                                    textSize = (subtitleSettings.textSize + 1f).coerceIn(10f, 60f)
                                )
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Subtitle text colour
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.subtitle_text_color),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        Color.Yellow, Color.White, Color.Black,
                        Color.Red, Color.Blue, Color.Green, Color.Cyan
                    ).forEach { color ->
                        val argb = color.toArgb()
                        SubtitleColorSwatch(
                            color = color,
                            isSelected = subtitleSettings.textColor == argb,
                            onClick = {
                                onSubtitleSettingsChanged(subtitleSettings.copy(textColor = argb))
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Subtitle background / outline colour
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.subtitle_background_color),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val glass = Color.Black.copy(alpha = 0.5f)
                    listOf(
                        Color.Transparent, glass, Color.Black,
                        Color.White, Color.Red, Color.Blue
                    ).forEach { color ->
                        val argb = color.toArgb()
                        SubtitleColorSwatch(
                            color = color,
                            isSelected = subtitleSettings.borderColor == argb,
                            onClick = {
                                onSubtitleSettingsChanged(subtitleSettings.copy(borderColor = argb))
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live preview so the chosen size/colours are visible instantly
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF101010))
                        .padding(vertical = 18.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.subtitle_preview_text),
                        color = Color(subtitleSettings.textColor),
                        fontSize = subtitleSettings.textSize.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(subtitleSettings.borderColor))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    androidx.compose.ui.res.stringResource(R.string.close),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )

    // ---------- Sub-popups ----------
    if (showAudioPicker) {
        val options = mutableListOf<PickerOption>()
        options += PickerOption(
            label = noneLabel,
            isSelected = audioTrackGroups.none { it.isSelected },
            onSelect = {
                trackSelector?.setParameters(
                    trackSelector.buildUponParameters()
                        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                )
            }
        )
        audioTrackGroups.forEachIndexed { groupIndex, trackGroup ->
            for (i in 0 until trackGroup.length) {
                val format = trackGroup.getTrackFormat(i)
                options += PickerOption(
                    label = trackName(format, "Track ${groupIndex + 1}.${i + 1}"),
                    isSelected = trackGroup.isTrackSelected(i),
                    onSelect = {
                        trackSelector?.setParameters(
                            trackSelector.buildUponParameters()
                                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                                .setOverrideForType(
                                    TrackSelectionOverride(trackGroup.mediaTrackGroup, i)
                                )
                        )
                    }
                )
            }
        }
        OptionPickerDialog(
            title = androidx.compose.ui.res.stringResource(R.string.audio_tracks),
            options = options,
            onDismiss = { showAudioPicker = false }
        )
    }

    if (showSubtitlePicker) {
        val options = mutableListOf<PickerOption>()
        options += PickerOption(
            label = noneLabel,
            isSelected = textTrackGroups.none { it.isSelected },
            onSelect = {
                trackSelector?.setParameters(
                    trackSelector.buildUponParameters()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                )
            }
        )
        textTrackGroups.forEachIndexed { groupIndex, trackGroup ->
            for (i in 0 until trackGroup.length) {
                val format = trackGroup.getTrackFormat(i)
                options += PickerOption(
                    label = trackName(format, "Subtitle ${groupIndex + 1}.${i + 1}"),
                    isSelected = trackGroup.isTrackSelected(i),
                    onSelect = {
                        trackSelector?.setParameters(
                            trackSelector.buildUponParameters()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                .setOverrideForType(
                                    TrackSelectionOverride(trackGroup.mediaTrackGroup, i)
                                )
                        )
                    }
                )
            }
        }
        OptionPickerDialog(
            title = androidx.compose.ui.res.stringResource(R.string.subtitles),
            options = options,
            onDismiss = { showSubtitlePicker = false }
        )
    }

    if (showSpeedPicker) {
        OptionPickerDialog(
            title = androidx.compose.ui.res.stringResource(R.string.playback_speed),
            options = speedOptions.map { speed ->
                PickerOption(
                    label = String.format("%.2fx", speed),
                    isSelected = speed == playbackSpeed,
                    onSelect = { onPlaybackSpeedChanged(speed) }
                )
            },
            onDismiss = { showSpeedPicker = false }
        )
    }
}

/**
 * A "title / current value / chevron" row inside the player settings dialog.
 * Tapping it (or pressing OK on a remote) opens the matching option popup.
 */
@Composable
private fun PlayerSettingRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = if (isFocused) 2.dp else 0.dp,
                    color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isFocused)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Focusable +/- button for the in-player subtitle size stepper. */
@Composable
private fun SubtitleSizeButton(
    symbol: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(
                if (enabled)
                    MaterialTheme.colorScheme.primary.copy(alpha = if (isFocused) 0.9f else 0.25f)
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = androidx.compose.foundation.shape.CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                isFocused -> MaterialTheme.colorScheme.onPrimary
                else -> MaterialTheme.colorScheme.primary
            }
        )
    }
}

/** Focusable colour swatch used by the in-player subtitle style section. */
@Composable
private fun SubtitleColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .size(34.dp)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .clip(RoundedCornerShape(6.dp))
            .background(if (color == Color.Transparent) Color.Gray.copy(alpha = 0.35f) else color)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = if (color == Color.White || color == Color.Yellow || color == Color.Transparent)
                    Color.Black
                else
                    Color.White,
                modifier = Modifier.size(16.dp)
            )
        } else if (color == Color.Transparent) {
            Text(text = "—", color = Color.White.copy(alpha = 0.8f))
        }
    }
}

fun formatTime(milliseconds: Long): String {
    val seconds = (milliseconds / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, remainingMinutes, remainingSeconds)
    } else {
        String.format("%02d:%02d", remainingMinutes, remainingSeconds)
    }
}