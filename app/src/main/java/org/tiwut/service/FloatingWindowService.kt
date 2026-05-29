package org.tiwut.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.ContextThemeWrapper
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import org.tiwut.data.entity.LauncherSettings
import org.tiwut.ui.launcher.LauncherViewModel
import org.tiwut.ui.launcher.OverlayLauncherContent
import org.tiwut.ui.theme.TiwutPortalTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FloatingWindowService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = store

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    
    private val isMinimized = MutableStateFlow(false)

    private val viewModel: LauncherViewModel by lazy {
        ViewModelProvider(this, LauncherViewModel.provideFactory(applicationContext))[LauncherViewModel::class.java]
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        setupFloatingWindow()
    }

    private fun setupFloatingWindow() {
        val wm = windowManager ?: return

        val contextThemeWrapper = ContextThemeWrapper(this, org.tiwut.R.style.Theme_TiwutPortal)
        val view = ComposeView(contextThemeWrapper).apply {
            setViewTreeLifecycleOwner(this@FloatingWindowService)
            setViewTreeViewModelStoreOwner(this@FloatingWindowService)
            setViewTreeSavedStateRegistryOwner(this@FloatingWindowService)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setContent {
                val minimized by isMinimized.collectAsState()
                val settings by viewModel.settings.collectAsState()

                TiwutPortalTheme {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (minimized && settings.showAccessibilityPuck) {
                            Box(
                                modifier = Modifier
                                    .size(settings.puckSize.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color(settings.puckColor).copy(alpha = settings.puckOpacity),
                                                Color(settings.puckColor).copy(alpha = settings.puckOpacity * 0.6f)
                                            )
                                        )
                                    )
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                shiftWindowPosition(IntOffset(dragAmount.x.toInt(), dragAmount.y.toInt()))
                                            },
                                            onDragEnd = { saveFinalWindowPosition() }
                                        )
                                    }
                                    .clickable { isMinimized.value = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Layers,
                                    contentDescription = "Open Portal",
                                    tint = Color.White,
                                    modifier = Modifier.size((settings.puckSize * 0.5f).dp)
                                )
                            }
                        } else {
                            OverlayLauncherContent(
                                viewModel = viewModel,
                                onCloseRequested = { 
                                    if (settings.showAccessibilityPuck) {
                                        isMinimized.value = true
                                    } else {
                                        stopSelf()
                                    }
                                },
                                isPreviewMode = false,
                                onDragStarted = { },
                                onDragged = { dragOffset ->
                                    shiftWindowPosition(dragOffset)
                                },
                                onDragEnded = {
                                    saveFinalWindowPosition()
                                },
                                onRenameFocusRequested = { focusable ->
                                    setWindowFocusable(focusable)
                                }
                            )
                        }
                    }
                }
            }
        }
        
        composeView = view

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        lifecycleScope.launch {
            combine(viewModel.settings, isMinimized) { settings, minimized ->
                settings to minimized
            }.collectLatest { (settings, minimized) ->
                val params = if (minimized && settings.showAccessibilityPuck) {
                    getTriggerLayoutParams(settings)
                } else {
                    getOverlayLayoutParams(settings, screenWidth, screenHeight)
                }
                
                if (view.parent == null) {
                    try {
                        wm.addView(view, params)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    try {
                        wm.updateViewLayout(view, params)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun getTriggerLayoutParams(settings: LauncherSettings): WindowManager.LayoutParams {
        val size = (settings.puckSize * resources.displayMetrics.density).toInt()
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH

        return WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = settings.windowX
            y = settings.windowY
        }
    }

    private fun getOverlayLayoutParams(
        settings: LauncherSettings,
        screenWidth: Int,
        screenHeight: Int
    ): WindowManager.LayoutParams {
        val computedWidth = (screenWidth * settings.widthPercent).toInt()
        val computedHeight = (screenHeight * settings.heightPercent).toInt()

        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                0x00000080

        return WindowManager.LayoutParams(
            computedWidth,
            computedHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = settings.windowX.coerceAtLeast(0).coerceAtMost(screenWidth - 200)
            y = settings.windowY.coerceAtLeast(0).coerceAtMost(screenHeight - 200)
        }
    }

    private fun shiftWindowPosition(dragOffset: IntOffset) {
        val view = composeView ?: return
        val wm = windowManager ?: return
        val params = view.layoutParams as WindowManager.LayoutParams
        
        params.x += dragOffset.x
        params.y += dragOffset.y
        
        try {
            wm.updateViewLayout(view, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveFinalWindowPosition() {
        val view = composeView ?: return
        val params = view.layoutParams as WindowManager.LayoutParams
        viewModel.saveWindowPosition(params.x, params.y)
    }

    private fun setWindowFocusable(focusable: Boolean) {
        val view = composeView ?: return
        val wm = windowManager ?: return
        val params = view.layoutParams as WindowManager.LayoutParams
        
        if (focusable) {
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        
        try {
            wm.updateViewLayout(view, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        val wm = windowManager
        val view = composeView
        if (wm != null && view != null && view.parent != null) {
            try {
                wm.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        
        isRunning = false
        super.onDestroy()
    }

    companion object {
        @Volatile
        var isRunning = false
    }
}
