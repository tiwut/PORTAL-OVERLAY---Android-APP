package org.tiwut.ui.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import org.tiwut.data.database.AppDatabase
import org.tiwut.data.entity.LauncherItem
import org.tiwut.data.entity.LauncherSettings
import org.tiwut.data.repository.LauncherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class SystemAppInfo(
    val packageName: String,
    val className: String,
    val label: String
)

class LauncherViewModel(
    private val repository: LauncherRepository,
    private val packageManager: PackageManager
) : ViewModel() {

    val themePresets = listOf(
        0xFF1A1F2C.toInt(),
        0xFF0D1721.toInt(),
        0xFF121212.toInt(),
        0xFF2D122D.toInt(),
        0xFF0A221C.toInt(),
        0xFF3E1F1F.toInt(),
        0xFFF5F6FA.toInt()
    )

    val settings: StateFlow<LauncherSettings> = repository.launcherSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LauncherSettings()
        )

    val allItems: StateFlow<List<LauncherItem>> = repository.launcherItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _activeFolderId = MutableStateFlow<String?>(null)
    val activeFolderId: StateFlow<String?> = _activeFolderId.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _systemApps = MutableStateFlow<List<SystemAppInfo>>(emptyList())
    val systemApps: StateFlow<List<SystemAppInfo>> = _systemApps.asStateFlow()

    fun loadSystemApps() {
        viewModelScope.launch {
            try {
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = packageManager.queryIntentActivities(intent, 0)
                val list = resolveInfos.map {
                    SystemAppInfo(
                        packageName = it.activityInfo.packageName,
                        className = it.activityInfo.name,
                        label = it.loadLabel(packageManager).toString()
                    )
                }.sortedBy { it.label }
                _systemApps.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    init {
        loadSystemApps()
        viewModelScope.launch {
            repository.ensureSettings()
        }
    }

    fun syncInstalledApps() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = packageManager.queryIntentActivities(intent, 0)
                val currentSystemPkgNames = resolveInfos.map { it.activityInfo.packageName }.toSet()

                val currentDbProducts = repository.getItems()

                // Prune any apps in the DB that are no longer installed on the system
                currentDbProducts.forEach { dbItem ->
                    if (dbItem.type == "APP" && dbItem.packageName != null) {
                        if (!currentSystemPkgNames.contains(dbItem.packageName)) {
                            repository.deleteItem(dbItem)
                        }
                    }
                }

                // Refresh system app list registry for the settings dashboard checklist
                loadSystemApps()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun updateBlurRadius(radius: Float) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(blurRadius = radius))
        }
    }

    fun updateTransparency(opacity: Float) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(transparency = opacity))
        }
    }

    fun updateWidthPercent(width: Float) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(widthPercent = width))
        }
    }

    fun updateHeightPercent(height: Float) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(heightPercent = height))
        }
    }

    fun updateSaturation(sat: Float) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(saturation = sat))
        }
    }

    fun updateTintColor(colorInt: Int) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(tintColor = colorInt))
        }
    }

    fun updateCornerRadius(radius: Float) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(cornerRadius = radius))
        }
    }

    fun updateStrokeWidth(width: Float) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(strokeWidth = width))
        }
    }

    fun updateBorderColor(color: Int) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(borderColor = color))
        }
    }

    fun updateShowSearchIcon(show: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(showSearchIcon = show))
        }
    }

    fun updateGridColumns(cols: Int) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(gridColumns = cols))
        }
    }

    fun updateIconSize(size: Float) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(iconSize = size))
        }
    }

    fun updateShowLabels(show: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(showLabels = show))
        }
    }

    fun updateLabelColor(color: Int) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(labelColor = color))
        }
    }

    fun updateLabelFontSize(size: Float) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(labelFontSize = size))
        }
    }

    fun updateEnableHapticFeedback(enable: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(enableHapticFeedback = enable))
        }
    }

    fun updateAnimationSpeedMultiplier(speed: Float) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(animationSpeedMultiplier = speed))
        }
    }

    fun updateHeaderTitleText(text: String) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(headerTitleText = text))
        }
    }

    fun updateEnableAutoCollapse(enable: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(enableAutoCollapse = enable))
        }
    }

    fun updatePuckColor(color: Int) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(puckColor = color))
        }
    }

    fun updatePuckOpacity(opacity: Float) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(puckOpacity = opacity))
        }
    }

    fun updateShowLaunchToast(show: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(showLaunchToast = show))
        }
    }

    fun updateFolderColumns(cols: Int) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(folderColumns = cols))
        }
    }

    fun updateFolderCardSize(size: String) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(folderCardSize = size))
        }
    }

    fun updateFolderColor(colorVal: Int) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(folderColor = colorVal))
        }
    }

    fun updateFolderCornerRadius(radius: Float) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(folderCornerRadius = radius))
        }
    }

    fun updateShowAccessibilityPuck(show: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(showAccessibilityPuck = show))
        }
    }

    fun updatePuckSize(size: Float) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(puckSize = size))
        }
    }

    fun toggleAppInOverlay(packageName: String, label: String, className: String, enabled: Boolean) {
        viewModelScope.launch {
            val currentItems = repository.getItems()
            val existing = currentItems.find { it.id == packageName }
            if (enabled) {
                if (existing == null) {
                    val maxPos = currentItems.maxByOrNull { it.position }?.position ?: -1
                    repository.insertItem(
                        LauncherItem(
                            id = packageName,
                            name = label,
                            type = "APP",
                            parentFolderId = null,
                            position = maxPos + 1,
                            packageName = packageName,
                            className = className
                        )
                    )
                }
            } else {
                if (existing != null) {
                    repository.deleteItem(existing)
                }
            }
        }
    }

    fun updateWindowLocation(x: Int, y: Int) {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(current.copy(windowX = x, windowY = y))
        }
    }

    fun saveWindowPosition(x: Int, y: Int) {
        viewModelScope.launch {
            repository.updateWindowPosition(x, y)
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            val current = settings.value
            repository.updateSettings(LauncherSettings(
                id = 0,
                windowX = current.windowX,
                windowY = current.windowY
            ))
        }
    }

    fun openFolder(folderId: String) {
        _activeFolderId.value = folderId
    }

    fun closeFolder() {
        _activeFolderId.value = null
    }

    fun createFolder(folderName: String, firstItem: LauncherItem, secondItem: LauncherItem) {
        viewModelScope.launch {
            val folderId = UUID.randomUUID().toString()
            val finalFolderName = folderName.ifEmpty { "New Folder" }

            val items = repository.getItems()
            val maxPos = items.filter { it.parentFolderId == null }.maxByOrNull { it.position }?.position ?: -1

            val folderEntity = LauncherItem(
                id = folderId,
                name = finalFolderName,
                type = "FOLDER",
                parentFolderId = null,
                position = maxPos + 1,
                packageName = null,
                className = null
            )

            val updatedFirst = firstItem.copy(parentFolderId = folderId, position = 0)
            val updatedSecond = secondItem.copy(parentFolderId = folderId, position = 1)

            repository.insertItem(folderEntity)
            repository.insertItem(updatedFirst)
            repository.insertItem(updatedSecond)
        }
    }

    fun createEmptyFolder(name: String) {
        viewModelScope.launch {
            val folderId = UUID.randomUUID().toString()
            val items = repository.getItems()
            val maxPos = items.filter { it.parentFolderId == null }.maxByOrNull { it.position }?.position ?: -1

            val folderEntity = LauncherItem(
                id = folderId,
                name = name.ifEmpty { "Folder" },
                type = "FOLDER",
                parentFolderId = null,
                position = maxPos + 1,
                packageName = null,
                className = null
            )
            repository.insertItem(folderEntity)
        }
    }

    fun createFolderWithSingleItem(folderName: String, item: LauncherItem) {
        viewModelScope.launch {
            val folderId = UUID.randomUUID().toString()
            val finalFolderName = folderName.ifEmpty { "New Folder" }

            val items = repository.getItems()
            val maxPos = items.filter { it.parentFolderId == null }.maxByOrNull { it.position }?.position ?: -1

            val folderEntity = LauncherItem(
                id = folderId,
                name = finalFolderName,
                type = "FOLDER",
                parentFolderId = null,
                position = maxPos + 1,
                packageName = null,
                className = null
            )

            val updatedItem = item.copy(parentFolderId = folderId, position = 0)

            repository.insertItem(folderEntity)
            repository.insertItem(updatedItem)
        }
    }

    fun ungroupFolder(folder: LauncherItem) {
        viewModelScope.launch {
            val items = repository.getItems()
            val folderChildren = items.filter { it.parentFolderId == folder.id }
            val rootItems = items.filter { it.parentFolderId == null }
            var maxPos = rootItems.maxByOrNull { it.position }?.position ?: -1

            folderChildren.forEach { child ->
                maxPos++
                repository.insertItem(child.copy(parentFolderId = null, position = maxPos))
            }
            repository.deleteItem(folder)
        }
    }

    fun renameFolder(folderId: String, newName: String) {
        viewModelScope.launch {
            val items = repository.getItems()
            val folder = items.find { it.id == folderId && it.type == "FOLDER" }
            if (folder != null) {
                repository.insertItem(folder.copy(name = newName))
            }
        }
    }

    fun moveItemToFolder(itemId: String, folderId: String) {
        viewModelScope.launch {
            val items = repository.getItems()
            val item = items.find { it.id == itemId }
            if (item != null) {
                val folderItems = items.filter { it.parentFolderId == folderId }
                val newPosition = folderItems.maxByOrNull { it.position }?.position?.plus(1) ?: 0
                val updated = item.copy(parentFolderId = folderId, position = newPosition)
                repository.insertItem(updated)
            }
        }
    }

    fun moveItemToRoot(itemId: String) {
        viewModelScope.launch {
            val items = repository.getItems()
            val item = items.find { it.id == itemId }
            if (item != null) {
                val rootItems = items.filter { it.parentFolderId == null }
                val newPosition = rootItems.maxByOrNull { it.position }?.position?.plus(1) ?: 0
                val updated = item.copy(parentFolderId = null, position = newPosition)
                repository.insertItem(updated)
            }
        }
    }

    fun deleteItem(item: LauncherItem) {
        viewModelScope.launch {
            if (item.type == "FOLDER") {
                val folderChildren = repository.getItems().filter { it.parentFolderId == item.id }
                folderChildren.forEach { child ->
                    repository.deleteItem(child)
                }
            }
            repository.deleteItem(item)
        }
    }

    fun swapItems(draggedId: String, hoverId: String) {
        if (draggedId == hoverId) return
        viewModelScope.launch {
            val items = repository.getItems()
            val dragged = items.find { it.id == draggedId }
            val hover = items.find { it.id == hoverId }

            if (dragged != null && hover != null) {
                val tempPos = dragged.position
                repository.insertItem(dragged.copy(position = hover.position))
                repository.insertItem(hover.copy(position = tempPos))
            }
        }
    }

    fun mergeItems(draggedId: String, targetId: String) {
        if (draggedId == targetId) return
        viewModelScope.launch {
            val items = repository.getItems()
            val dragged = items.find { it.id == draggedId }
            val target = items.find { it.id == targetId }

            if (dragged != null && target != null) {
                if (target.type == "FOLDER") {
                    moveItemToFolder(dragged.id, target.id)
                } else if (target.type == "APP" && dragged.type == "APP") {
                    createFolder(
                        folderName = "Merge Drawer",
                        firstItem = target,
                        secondItem = dragged
                    )
                }
            }
        }
    }

    fun launchApp(context: Context, item: LauncherItem, onFinish: () -> Unit = {}) {
        if (item.packageName == null) return
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(item.packageName)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                onFinish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getDatabase(context)
                    val repository = LauncherRepository(db.launcherDao())
                    return LauncherViewModel(repository, context.packageManager) as T
                }
            }
        }
    }
}
