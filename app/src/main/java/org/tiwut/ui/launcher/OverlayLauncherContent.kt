package org.tiwut.ui.launcher
import android.widget.ImageView
import android.content.Context
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.tiwut.data.entity.LauncherItem
import org.tiwut.data.entity.LauncherSettings
import kotlin.math.roundToInt
fun Modifier.glassOverlay(
    opacity: Float,
    tintColorInt: Int,
    cornerRadiusDp: Float,
    themeSurfaceColor: Color,
    themeOutlineColor: Color
): Modifier = this.drawWithCache {
    val baseTintColor = Color(tintColorInt)
    val tintColor = if (tintColorInt == 0xFFFFFFFF.toInt()) themeSurfaceColor else baseTintColor
    val cornerRadiusPx = cornerRadiusDp.dp.toPx()
    onDrawWithContent {
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    tintColor.copy(alpha = opacity),
                    tintColor.copy(alpha = (opacity * 0.7f).coerceIn(0f, 1f))
                ),
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height)
            ),
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx)
        )
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.15f),
                    Color.White.copy(alpha = 0.0f)
                ),
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.5f)
            ),
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx)
        )
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.25f),
                    themeOutlineColor.copy(alpha = 0.35f),
                    themeOutlineColor.copy(alpha = 0.1f)
                ),
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height)
            ),
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadiusPx, cornerRadiusPx),
            style = Stroke(width = 1.2.dp.toPx())
        )
        drawContent()
    }
}
@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OverlayLauncherContent(
    viewModel: LauncherViewModel,
    onCloseRequested: () -> Unit,
    modifier: Modifier = Modifier,
    isPreviewMode: Boolean = false,
    onDragStarted: () -> Unit = {},
    onDragged: (offset: IntOffset) -> Unit = {},
    onDragEnded: () -> Unit = {},
    onRenameFocusRequested: (Boolean) -> Unit = {}
) {
    val items by viewModel.allItems.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val activeFolderId by viewModel.activeFolderId.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    var showSettingsPanel by remember { mutableStateOf(false) }
    var filterQuery by remember { mutableStateOf("") }
    var folderRenameId by remember { mutableStateOf<String?>(null) }
    var currentRenameText by remember { mutableStateOf("") }
    var isDraggingItemRoot by remember { mutableStateOf(false) }
    var draggedItemId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(FloatOffset(0f, 0f)) }
    var hoveredItemId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val density = LocalDensity.current
    val hapticFeedback = androidx.compose.ui.platform.LocalHapticFeedback.current
    val rootItems = remember(items) {
        items.filter { it.parentFolderId == null }.sortedBy { it.position }
    }
    val filteredRootItems = remember(rootItems, filterQuery) {
        if (filterQuery.isBlank()) {
            rootItems
        } else {
            rootItems.filter { it.name.contains(filterQuery, ignoreCase = true) }
        }
    }
    val activeFolder = remember(activeFolderId, items) {
        items.find { it.id == activeFolderId && it.type == "FOLDER" }
    }
    val folderApps = remember(activeFolderId, items) {
        if (activeFolderId != null) {
            items.filter { it.parentFolderId == activeFolderId }.sortedBy { it.position }
        } else {
            emptyList()
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = {
                    if (activeFolderId != null) {
                        viewModel.closeFolder()
                    } else if (showSettingsPanel) {
                        showSettingsPanel = false
                    }
                }
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(settings.widthPercent)
                .fillMaxHeight(settings.heightPercent)
                .border(
                    width = settings.strokeWidth.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(settings.cornerRadius.dp)
                )
                .testTag("floating_launcher_card")
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = { }
                ),
            shape = RoundedCornerShape(settings.cornerRadius.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(settings.cornerRadius.dp))
                        .glassOverlay(
                            opacity = settings.transparency,
                            tintColorInt = settings.tintColor,
                            cornerRadiusDp = settings.cornerRadius,
                            themeSurfaceColor = MaterialTheme.colorScheme.surface,
                            themeOutlineColor = MaterialTheme.colorScheme.outline
                        )
                )
                Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            if (!isPreviewMode) {
                                detectDragGestures(
                                    onDragStart = { onDragStarted() },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        onDragged(
                                            IntOffset(
                                                dragAmount.x.roundToInt(),
                                                dragAmount.y.roundToInt()
                                            )
                                        )
                                    },
                                    onDragEnd = { onDragEnded() },
                                    onDragCancel = { onDragEnded() }
                                )
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DashboardCustomize,
                            contentDescription = "launcher_icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isScanning) "Syncing..." else settings.headerTitleText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = onCloseRequested,
                            modifier = Modifier.size(32.dp).testTag("close_window_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close window",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (settings.showSearchIcon) {
                        androidx.compose.material3.OutlinedTextField(
                            value = filterQuery,
                            onValueChange = { filterQuery = it },
                            placeholder = { Text("Filter apps...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(16.dp)) },
                            trailingIcon = {
                                if (filterQuery.isNotEmpty()) {
                                    IconButton(onClick = { filterQuery = "" }, modifier = Modifier.size(20.dp)) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(14.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                focusedLabelColor = Color.Transparent,
                                unfocusedLabelColor = Color.Transparent
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (items.isEmpty() && isScanning) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Scanning for launchable apps...",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    fontSize = 13.sp
                                )
                            }
                        } else if (filteredRootItems.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = "Empty",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    if (filterQuery.isNotEmpty()) "No matching apps found in launcher" else "Launcher is empty.\nGo to App Settings to add folders and apps!",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                        val isTrashHovered = hoveredItemId == "TRASH_ZONE"
                        androidx.compose.animation.AnimatedVisibility(
                            visible = draggedItemId != null,
                            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .align(Alignment.TopCenter)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (isTrashHovered) Color.Red.copy(alpha = 0.25f) else Color.Red.copy(alpha = 0.08f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isTrashHovered) Color.Red else Color.Red.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                                    )
                                    .pointerInput(draggedItemId) {
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Trash icon",
                                        tint = if (isTrashHovered) Color.White else Color.Red,
                                        modifier = Modifier.size(24.dp).scale(if (isTrashHovered) 1.2f else 1f)
                                    )
                                    Text(
                                        text = if (isTrashHovered) "Release to Delete / Remove" else "Drag here to Remove",
                                        color = if (isTrashHovered) Color.White else Color.Red,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(settings.gridColumns),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(top = if (draggedItemId != null) 72.dp else 16.dp, bottom = 16.dp)
                        ) {
                            items(filteredRootItems, key = { it.id }) { item ->
                                val isHovered = hoveredItemId == item.id
                                val isDragged = draggedItemId == item.id
                                val densityValue = androidx.compose.ui.platform.LocalDensity.current.density
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                        .graphicsLayer {
                                            scaleX = if (isHovered) 1.15f else if (isDragged) 0.85f else 1.0f
                                            scaleY = if (isHovered) 1.15f else if (isDragged) 0.85f else 1.0f
                                        }
                                        .clip(RoundedCornerShape((settings.cornerRadius * 0.5f).dp))
                                        .background(
                                            if (isHovered) Color.White.copy(alpha = 0.12f) else Color.Transparent
                                        )
                                        .pointerInput(item.id) {
                                            detectDragGestures(
                                                onDragStart = { offset ->
                                                    draggedItemId = item.id
                                                    isDraggingItemRoot = true
                                                    dragOffset = FloatOffset(0f, 0f)
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffset = FloatOffset(
                                                        dragOffset.x + dragAmount.x,
                                                        dragOffset.y + dragAmount.y
                                                     )
                                                     val screenYOffset = dragOffset.y / densityValue
                                                     if (screenYOffset < -50f) {
                                                         hoveredItemId = "TRASH_ZONE"
                                                     } else {
                                                         hoveredItemId = null
                                                     }
                                                },
                                                onDragEnd = {
                                                    val localDragged = draggedItemId
                                                    val localHovered = hoveredItemId
                                                    if (localDragged != null) {
                                                        if (localHovered == "TRASH_ZONE") {
                                                            viewModel.deleteItem(item)
                                                        } else if (localHovered != null && localHovered != localDragged) {
                                                            viewModel.mergeItems(localDragged, localHovered)
                                                        }
                                                    }
                                                    draggedItemId = null
                                                    hoveredItemId = null
                                                    dragOffset = FloatOffset(0f, 0f)
                                                },
                                                onDragCancel = {
                                                    draggedItemId = null
                                                    hoveredItemId = null
                                                    dragOffset = FloatOffset(0f, 0f)
                                                }
                                            )
                                        }
                                        .combinedClickable(
                                            onClick = {
                                                if (settings.enableHapticFeedback) {
                                                    hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                }
                                                if (item.type == "FOLDER") {
                                                    viewModel.openFolder(item.id)
                                                } else {
                                                    if (settings.showLaunchToast) {
                                                        android.widget.Toast.makeText(context, "Opening ${item.name}...", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                    viewModel.launchApp(context, item, onFinish = onCloseRequested)
                                                }
                                            },
                                            onLongClick = {
                                                if (settings.enableHapticFeedback) {
                                                    hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                }
                                                if (item.type != "FOLDER") {
                                                    viewModel.createFolderWithSingleItem("New Folder", item)
                                                }
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(4.dp)
                                    ) {
                                        if (item.type == "FOLDER") {
                                            val nestedApps = items.filter { it.parentFolderId == item.id }
                                            val nestedPackages = nestedApps.mapNotNull { it.packageName }
                                            FolderIcon(
                                                childPackages = nestedPackages, saturation = settings.saturation,
                                                modifier = Modifier.size(settings.iconSize.dp)
                                            )
                                        } else if (item.packageName != null) {
                                            AppIcon(
                                                packageName = item.packageName, saturation = settings.saturation,
                                                modifier = Modifier.size(settings.iconSize.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        if (settings.showLabels) {
                                            Text(
                                                text = item.name,
                                                fontSize = settings.labelFontSize.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (settings.labelColor == 0xFFFFFFFF.toInt()) MaterialTheme.colorScheme.onSurface else Color(settings.labelColor),
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = activeFolder != null,
                        enter = scaleIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
                        exit = scaleOut() + fadeOut(),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    ) {
                        activeFolder?.let { folder ->
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                val cardModifier = when (settings.folderCardSize) {
                                    "Small" -> Modifier.fillMaxWidth(0.72f).fillMaxHeight(0.68f)
                                    "Large" -> Modifier.fillMaxSize().padding(12.dp)
                                    else -> Modifier.fillMaxWidth(0.88f).fillMaxHeight(0.82f)
                                }
                                Card(
                                    modifier = cardModifier
                                        .clip(RoundedCornerShape(settings.folderCornerRadius.dp))
                                        .border(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(settings.folderCornerRadius.dp)),
                                    shape = RoundedCornerShape(settings.folderCornerRadius.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                                    )
                                ) {
                                    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (folderRenameId == folder.id) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                TextField(
                                                    value = currentRenameText,
                                                    onValueChange = { currentRenameText = it },
                                                    colors = TextFieldDefaults.colors(
                                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                                        unfocusedIndicatorColor = Color.Transparent
                                                    ),
                                                    modifier = Modifier.weight(1f).testTag("folder_rename_input"),
                                                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                                                )
                                                IconButton(
                                                    onClick = {
                                                        viewModel.renameFolder(folder.id, currentRenameText)
                                                        folderRenameId = null
                                                        onRenameFocusRequested(false)
                                                    },
                                                    modifier = Modifier.testTag("save_rename_button")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Save Rename",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        } else {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = folder.name,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 15.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                IconButton(
                                                    onClick = {
                                                        folderRenameId = folder.id
                                                        currentRenameText = folder.name
                                                        onRenameFocusRequested(true)
                                                    },
                                                    modifier = Modifier.size(28.dp).testTag("edit_folder_name_button")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Rename folder",
                                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                onRenameFocusRequested(false)
                                                viewModel.closeFolder()
                                            },
                                            modifier = Modifier.size(32.dp).testTag("close_folder_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ArrowBack,
                                                contentDescription = "Back from folder",
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    if (folderApps.isEmpty()) {
                                        Box(
                                            modifier = Modifier.weight(1f).fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "Tap & hold apps to manage them, or merge other apps inside this container.",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(16.dp)
                                            )
                                        }
                                    } else {
                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(settings.folderColumns),
                                            modifier = Modifier.weight(1f).fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            items(folderApps, key = { it.id }) { app ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(64.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .combinedClickable(
                                                            onClick = {
                                                                 viewModel.launchApp(context, app, onFinish = onCloseRequested)
                                                            },
                                                            onLongClick = {
                                                                viewModel.moveItemToRoot(app.id)
                                                            }
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        if (app.packageName != null) {
                                                            AppIcon(
                                                                packageName = app.packageName, saturation = settings.saturation,
                                                                modifier = Modifier.size(40.dp)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(3.dp))
                                                        Text(
                                                            text = app.name,
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            textAlign = TextAlign.Center,
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = {
                                                viewModel.ungroupFolder(folder)
                                                viewModel.closeFolder()
                                            },
                                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Folder", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Ungroup Folder", fontSize = 12.sp)
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
    }
}
class FloatOffset(val x: Float, val y: Float)
@Composable
fun Modifier.verticalScrollStateOrSimple(): Modifier {
    return this.verticalScroll(androidx.compose.foundation.rememberScrollState())
}
@Composable
fun AppIcon(packageName: String, saturation: Float, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                minimumWidth = 44
                minimumHeight = 44
            }
        },
        modifier = modifier.size(44.dp),
        update = { imageView ->
            try {
                val icon = context.packageManager.getApplicationIcon(packageName)
                imageView.setImageDrawable(icon)
                if (saturation != 1.0f) {
                    val matrix = android.graphics.ColorMatrix().apply {
                        setSaturation(saturation)
                    }
                    imageView.colorFilter = android.graphics.ColorMatrixColorFilter(matrix)
                } else {
                    imageView.clearColorFilter()
                }
            } catch (e: Exception) {
                imageView.setImageResource(android.R.drawable.sym_def_app_icon)
                imageView.clearColorFilter()
            }
        }
    )
}
@Composable
fun FolderIcon(childPackages: List<String>, saturation: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.2.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (childPackages.isEmpty()) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = "Empty Folder",
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        } else {
            val displayPackages = childPackages.take(4)
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceAround
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    if (displayPackages.size > 0) {
                        AppIcon(packageName = displayPackages[0], saturation = saturation, modifier = Modifier.size(16.dp))
                    }
                    if (displayPackages.size > 1) {
                        AppIcon(packageName = displayPackages[1], saturation = saturation, modifier = Modifier.size(16.dp))
                    } else if (displayPackages.size > 0) {
                        Spacer(modifier = Modifier.size(16.dp))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    if (displayPackages.size > 2) {
                        AppIcon(packageName = displayPackages[2], saturation = saturation, modifier = Modifier.size(16.dp))
                    } else {
                        Spacer(modifier = Modifier.size(16.dp))
                    }
                    if (displayPackages.size > 3) {
                        AppIcon(packageName = displayPackages[3], saturation = saturation, modifier = Modifier.size(16.dp))
                    } else {
                        Spacer(modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
