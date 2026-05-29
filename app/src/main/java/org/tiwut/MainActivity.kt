package org.tiwut

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import org.tiwut.service.FloatingWindowService
import org.tiwut.ui.launcher.LauncherViewModel
import org.tiwut.ui.launcher.OverlayLauncherContent
import org.tiwut.ui.theme.TiwutPortalTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: LauncherViewModel
    private var hasOverlayPermissionState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(
            this,
            LauncherViewModel.provideFactory(applicationContext)
        )[LauncherViewModel::class.java]

        setContent {
            TiwutPortalTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    MainActivityDashboard(
                        viewModel = viewModel,
                        hasOverlayPermission = hasOverlayPermissionState.value,
                        onGrantPermissionClicked = { requestOverlayPermission() },
                        onToggleFloatingWindow = { toggleFloatingService() },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasOverlayPermissionState.value = Settings.canDrawOverlays(this)
    }

    private fun requestOverlayPermission() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            startActivity(intent)
        }
    }

    private fun toggleFloatingService() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Enable Draw Over Other Apps permission first!", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, FloatingWindowService::class.java)
        if (FloatingWindowService.isRunning) {
            stopService(intent)
            Toast.makeText(this, "Floating overlay stopped.", Toast.LENGTH_SHORT).show()
        } else {
            startService(intent)
            Toast.makeText(this, "Floating overlay started on screen!", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun MainActivityDashboard(
    viewModel: LauncherViewModel,
    hasOverlayPermission: Boolean,
    onGrantPermissionClicked: () -> Unit,
    onToggleFloatingWindow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var isServiceRunning by remember { mutableStateOf(FloatingWindowService.isRunning) }
    var activeTab by remember { mutableIntStateOf(0) }

    val settings by viewModel.settings.collectAsState()
    val items by viewModel.allItems.collectAsState()
    val systemApps by viewModel.systemApps.collectAsState()

    var appSearchQuery by remember { mutableStateOf("") }
    var newFolderName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            isServiceRunning = FloatingWindowService.isRunning
            kotlinx.coroutines.delay(1000)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary,
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        )
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Layers,
                            contentDescription = "App logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "PORTAL OVERLAY",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = 1.5.sp
                )
                Text(
                    "Desktop Customizer Control Center",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (isServiceRunning) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isServiceRunning) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isServiceRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )
                    Column {
                        Text(
                            text = if (isServiceRunning) "Overlay Active" else "Overlay Inactive",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (isServiceRunning) "Floating overlay window is active" else "Stopped — launch via Quick Settings trigger",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Button(
                    onClick = onToggleFloatingWindow,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isServiceRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        contentColor = if (isServiceRunning) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("action_toggle_overlay_main")
                ) {
                    Text(
                        text = if (isServiceRunning) "Stop Window" else "Start Window",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (!hasOverlayPermission) {
            Card(
                modifier = Modifier.fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = "Alert",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Permission Required",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Please allow 'Display over other apps' to use the floating home portal launcher overlay.",
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                    Button(
                        onClick = onGrantPermissionClicked,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Grant", fontSize = 11.sp)
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Dynamic Live Preview (Interactive Window Canvas)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Settings Sync Real-Time",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Vibrant desktop-like mesh gradient wallpaper
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFEC4899), // Hot Pink
                                    Color(0xFF8B5CF6), // Royal Purple
                                    Color(0xFF3B82F6), // Indigo/Blue
                                    Color(0xFF06B6D4)  // Cyan
                                )
                            )
                        )
                )

                OverlayLauncherContent(
                    viewModel = viewModel,
                    onCloseRequested = {
                        Toast.makeText(context, "Closing request preview", Toast.LENGTH_SHORT).show()
                    },
                    isPreviewMode = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        TabRow(
            selectedTabIndex = activeTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        ) {
            val tabs = listOf(
                Pair("Aesthetics", Icons.Rounded.Style),
                Pair("Overlay Apps", Icons.Rounded.Apps),
                Pair("Folders & Position", Icons.Rounded.FolderCopy),
                Pair("About", Icons.Rounded.Info)
            )
            tabs.forEachIndexed { index, (title, icon) ->
                Tab(
                    selected = activeTab == index,
                    onClick = { activeTab = index },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }

        when (activeTab) {
            0 -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Header Branding & Search", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            
                            OutlinedTextField(
                                value = settings.headerTitleText,
                                onValueChange = { viewModel.updateHeaderTitleText(it) },
                                label = { Text("Branded Header Label Text", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                                placeholder = { Text("e.g. PORTAL", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Show Overlay Filtering Search Bar", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text("Display customizable input for quick app filter searching", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 9.sp)
                                }
                                Switch(
                                    checked = settings.showSearchIcon,
                                    onCheckedChange = { viewModel.updateShowSearchIcon(it) }
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Overlay Transparency & Radius", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Window Corner Radius", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                                    Text("${settings.cornerRadius.toInt()} dp", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                }
                                Slider(
                                    value = settings.cornerRadius,
                                    onValueChange = { viewModel.updateCornerRadius(it) },
                                    valueRange = 0f..48f,
                                    modifier = Modifier.testTag("corner_radius_slider")
                                )
                            }

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Glass Opacity", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                                    Text("${(settings.transparency * 100).toInt()}%", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                }
                                Slider(
                                    value = settings.transparency,
                                    onValueChange = { viewModel.updateTransparency(it) },
                                    valueRange = 0.05f..0.98f,
                                )
                            }

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Border Stroke Width", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                                    Text("${String.format("%.1f", settings.strokeWidth)} dp", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                }
                                Slider(
                                    value = settings.strokeWidth,
                                    onValueChange = { viewModel.updateStrokeWidth(it) },
                                    valueRange = 0.5f..5.0f,
                                )
                            }

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                            Text("Theme Accent Color Override", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                viewModel.themePresets.forEach { colorInt ->
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(colorInt))
                                            .border(
                                                width = if (settings.tintColor == colorInt) 2.dp else 0.dp,
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = CircleShape
                                            )
                                            .clickable { viewModel.updateTintColor(colorInt) }
                                    )
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Layout Columns & Icon Sizing", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Layout Grid Columns", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                                    Text("${settings.gridColumns} Columns", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                }
                                Slider(
                                    value = settings.gridColumns.toFloat(),
                                    onValueChange = { viewModel.updateGridColumns(it.toInt()) },
                                    valueRange = 3f..6f,
                                    steps = 2
                                )
                            }

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("App Icon Size", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                                    Text("${settings.iconSize.toInt()} dp", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                }
                                Slider(
                                    value = settings.iconSize,
                                    onValueChange = { viewModel.updateIconSize(it) },
                                    valueRange = 32f..64f,
                                )
                            }

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Show App Labels", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text("Toggle application launcher text captions", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 9.sp)
                                }
                                Switch(
                                    checked = settings.showLabels,
                                    onCheckedChange = { viewModel.updateShowLabels(it) }
                                )
                            }

                            if (settings.showLabels) {
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("App Caption Label Font Size", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                                        Text("${settings.labelFontSize.toInt()} sp", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                    }
                                    Slider(
                                        value = settings.labelFontSize,
                                        onValueChange = { viewModel.updateLabelFontSize(it) },
                                        valueRange = 9f..16f,
                                    )
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Folder Styling & Layout", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                            Column {
                                Text("Folder Window Size Preset", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("Small", "Medium", "Large").forEach { size ->
                                        val isSelected = settings.folderCardSize == size
                                        Button(
                                            onClick = { viewModel.updateFolderCardSize(size) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            Text(size, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Folder Grid Columns", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                                    Text("${settings.folderColumns} Cols", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                }
                                Slider(
                                    value = settings.folderColumns.toFloat(),
                                    onValueChange = { viewModel.updateFolderColumns(it.toInt()) },
                                    valueRange = 3f..6f,
                                    steps = 2
                                )
                            }

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Folder Corner Radius", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                                    Text("${settings.folderCornerRadius.toInt()} dp", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                }
                                Slider(
                                    value = settings.folderCornerRadius,
                                    onValueChange = { viewModel.updateFolderCornerRadius(it) },
                                    valueRange = 0f..32f,
                                )
                            }

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                            Column {
                                Text("Folder Theme Background", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        0xFF0F172A.toInt(),
                                        0xFF1E1B4B.toInt(),
                                        0xFF022C22.toInt(),
                                        0xFF3F1616.toInt(),
                                        0xFF1C1917.toInt(),
                                    ).forEach { colorInt ->
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color(colorInt))
                                                .border(
                                                    width = if (settings.folderColor == colorInt) 2.dp else 0.dp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = CircleShape
                                                )
                                                .clickable { viewModel.updateFolderColor(colorInt) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Haptic Feedback & Portal Scaling", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Enable Haptic Feedback", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                                    Text("Execute system haptics feedback on item operations", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 9.sp)
                                }
                                Switch(
                                    checked = settings.enableHapticFeedback,
                                    onCheckedChange = { viewModel.updateEnableHapticFeedback(it) }
                                )
                            }

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Show Launching Toast", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                                    Text("Display launching toast upon container application clicks", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 9.sp)
                                }
                                Switch(
                                    checked = settings.showLaunchToast,
                                    onCheckedChange = { viewModel.updateShowLaunchToast(it) }
                                )
                            }

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Portal Width (Screen %)", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                                    Text("${(settings.widthPercent * 100).toInt()}% screen", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                }
                                Slider(
                                    value = settings.widthPercent,
                                    onValueChange = { viewModel.updateWidthPercent(it) },
                                    valueRange = 0.4f..1.0f,
                                )
                            }

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Portal Height (Screen %)", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                                    Text("${(settings.heightPercent * 100).toInt()}% screen", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                }
                                Slider(
                                    value = settings.heightPercent,
                                    onValueChange = { viewModel.updateHeightPercent(it) },
                                    valueRange = 0.3f..1.0f,
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Accessibility & Trigger Puck", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Show Floating Accessibility Puck", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                                    Text("Always-on-top trigger button to open portal workspace instantly", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 9.sp)
                                }
                                Switch(
                                    checked = settings.showAccessibilityPuck,
                                    onCheckedChange = { viewModel.updateShowAccessibilityPuck(it) }
                                )
                            }

                            if (settings.showAccessibilityPuck) {
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Puck Size (Ease of Click)", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                                        Text("${settings.puckSize.toInt()} dp", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                    }
                                    Slider(
                                        value = settings.puckSize,
                                        onValueChange = { viewModel.updatePuckSize(it) },
                                        valueRange = 40f..96f,
                                    )
                                }

                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Puck Opacity", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                                        Text("${(settings.puckOpacity * 100).toInt()}%", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                    }
                                    Slider(
                                        value = settings.puckOpacity,
                                        onValueChange = { viewModel.updatePuckOpacity(it) },
                                        valueRange = 0.1f..1.0f,
                                    )
                                }

                                Text("Puck Theme Color", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    viewModel.themePresets.forEach { colorInt ->
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color(colorInt))
                                                .border(
                                                    width = if (settings.puckColor == colorInt) 2.dp else 0.dp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = CircleShape
                                                )
                                                .clickable { viewModel.updatePuckColor(colorInt) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.resetSettings() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset All Design Cosmetics Parameters", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            1 -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Workspace Desktop Apps Checklist",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Text(
                        "Manage customized app layouts below. Overlay is empty with no apps selected by default. Check packages to insert, uncheck to instantly clean remove!",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    OutlinedTextField(
                        value = appSearchQuery,
                        onValueChange = { appSearchQuery = it },
                        placeholder = { Text("Filter system apps list...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                        trailingIcon = {
                            if (appSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { appSearchQuery = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    val filteredApps = remember(systemApps, appSearchQuery) {
                        if (appSearchQuery.isBlank()) {
                            systemApps
                        } else {
                            systemApps.filter { it.label.contains(appSearchQuery, ignoreCase = true) || it.packageName.contains(appSearchQuery, ignoreCase = true) }
                        }
                    }

                    if (systemApps.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Syncing system installation registry...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 11.sp)
                            }
                        }
                    } else if (filteredApps.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No matching system application records found.", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 12.sp)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        ) {
                            androidx.compose.foundation.lazy.LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(8.dp)
                            ) {
                                items(filteredApps, key = { it.packageName }) { appInfo ->
                                    val isAdded = items.any { it.packageName == appInfo.packageName }
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                width = 1.dp,
                                                color = if (isAdded) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isAdded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                                                    MainActivityAppIcon(packageName = appInfo.packageName, modifier = Modifier.size(32.dp))
                                                }

                                                Column {
                                                    Text(appInfo.label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text(appInfo.packageName, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 9.sp, maxLines = 1)
                                                }
                                            }

                                            Button(
                                                onClick = {
                                                    viewModel.toggleAppInOverlay(appInfo.packageName, appInfo.label, appInfo.className, !isAdded)
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isAdded) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                                    contentColor = if (isAdded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                                ),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isAdded) Icons.Rounded.Remove else Icons.Rounded.Add,
                                                        contentDescription = null,
                                                        tint = if (isAdded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Text(
                                                        text = if (isAdded) "Remove" else "Add",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            2 -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Overall Coordinates Placements", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Directly align positions of the overlay launcher desktop with dynamic presets coordinates", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 10.sp)

                            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Window X Coordinate", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                                    Text("${settings.windowX} px", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Slider(
                                        value = settings.windowX.toFloat(),
                                        onValueChange = { viewModel.updateWindowLocation(it.toInt(), settings.windowY) },
                                        valueRange = 0f..2000f,
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Window Y Coordinate", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                                    Text("${settings.windowY} px", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Slider(
                                        value = settings.windowY.toFloat(),
                                        onValueChange = { viewModel.updateWindowLocation(settings.windowX, it.toInt()) },
                                        valueRange = 0f..2000f,
                                    )
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                            Text("Quick Screen Snaps Preset Locations", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { viewModel.updateWindowLocation(20, 40) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), contentColor = MaterialTheme.colorScheme.onSurface),
                                    modifier = Modifier.weight(1f).height(32.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Top-Left", fontSize = 10.sp)
                                }
                                Button(
                                    onClick = { viewModel.updateWindowLocation(600, 40) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), contentColor = MaterialTheme.colorScheme.onSurface),
                                    modifier = Modifier.weight(1f).height(32.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Top-Right", fontSize = 10.sp)
                                }
                                Button(
                                    onClick = { viewModel.updateWindowLocation(300, 450) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), contentColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.weight(1f).height(32.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Screen Center", fontSize = 10.sp)
                                }
                                Button(
                                    onClick = { viewModel.updateWindowLocation(20, 950) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), contentColor = MaterialTheme.colorScheme.onSurface),
                                    modifier = Modifier.weight(1f).height(32.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Bottom-Left", fontSize = 10.sp)
                                }
                                Button(
                                    onClick = { viewModel.updateWindowLocation(600, 950) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), contentColor = MaterialTheme.colorScheme.onSurface),
                                    modifier = Modifier.weight(1f).height(32.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Bottom-Right", fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Organizational Container Drawers", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newFolderName,
                                    onValueChange = { newFolderName = it },
                                    placeholder = { Text("Insert Folder Title Name...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 11.sp) },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp),
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )

                                Button(
                                    onClick = {
                                        if (newFolderName.isNotBlank()) {
                                            viewModel.createEmptyFolder(newFolderName)
                                            newFolderName = ""
                                            Toast.makeText(context, "Folder created inside overlay launcher workspace!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Create", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            val folderItems = remember(items) { items.filter { it.type == "FOLDER" } }

                            if (folderItems.isEmpty()) {
                                Text("No organizational folders exist on workspace yet. Type a name above to create one instantly!", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                            } else {
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                                Text("Active Workspace Folders Info & Items Management", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                
                                folderItems.forEach { folder ->
                                    val childrenAppsInFolder = remember(items) { items.filter { it.parentFolderId == folder.id } }
                                    
                                    Card(
                                        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                    Text(folder.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text("(${childrenAppsInFolder.size} apps inside)", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp)
                                                }

                                                IconButton(
                                                    onClick = {
                                                        childrenAppsInFolder.forEach { appItem ->
                                                            viewModel.moveItemToRoot(appItem.id)
                                                        }
                                                        viewModel.deleteItem(folder)
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete Folder", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                                }
                                            }

                                            if (childrenAppsInFolder.isEmpty()) {
                                                Text("This organizational folder is currently empty.", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 10.sp)
                                            } else {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    childrenAppsInFolder.forEach { appItem ->
                                                        Row(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Text(appItem.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp)
                                                            IconButton(
                                                                onClick = { viewModel.moveItemToRoot(appItem.id) },
                                                                modifier = Modifier.size(14.dp)
                                                            ) {
                                                                Icon(Icons.Rounded.Cancel, contentDescription = "Remove app", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(10.dp))
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 2.dp))

                                            val sourceRootApps = remember(items) { items.filter { it.type == "APP" && it.parentFolderId == null } }
                                            if (sourceRootApps.isEmpty()) {
                                                Text("No available active apps to move into this folder drawer. Add apps under the Apps tab first!", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 10.sp)
                                            } else {
                                                Text("Add active app items into this folder drawer custom container:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    sourceRootApps.forEach { rootApp ->
                                                        Button(
                                                            onClick = {
                                                                viewModel.moveItemToFolder(rootApp.id, folder.id)
                                                            },
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                                contentColor = MaterialTheme.colorScheme.primary
                                                            ),
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                            modifier = Modifier.height(22.dp),
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Icon(Icons.Rounded.ArrowDownward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(10.dp))
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                            Text(rootApp.name, fontSize = 8.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            3 -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "Developer: Tiwut",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Creator of Portal Overlay Launcher",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/tiwut")))
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("GitHub", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://tiwut.org/")))
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Rounded.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Website", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "MIT License",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Copyright (c) 2026 Tiwut\n\nPermission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files...",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                            TextButton(
                                onClick = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/tiwut/PORTAL-OVERLAY---Android-APP")))
                                }
                            ) {
                                Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Read full LICENSE & README on GitHub", fontSize = 11.sp)
                            }
                        }
                    }

                    Text(
                        "Version 1.2.0 (Stable Branch)",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun MainActivityAppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            android.widget.ImageView(ctx).apply {
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                minimumWidth = 44
                minimumHeight = 44
            }
        },
        modifier = modifier.size(36.dp),
        update = { imageView ->
            try {
                val icon = context.packageManager.getApplicationIcon(packageName)
                imageView.setImageDrawable(icon)
            } catch (e: Exception) {
                imageView.setImageResource(android.R.drawable.sym_def_app_icon)
            }
        }
    )
}
