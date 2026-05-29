package org.tiwut.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "launcher_settings")
data class LauncherSettings(
    @PrimaryKey val id: Int = 0,
    val blurRadius: Float = 15f,
    val transparency: Float = 0.85f,
    val widthPercent: Float = 0.85f,
    val heightPercent: Float = 0.65f,
    val saturation: Float = 1.0f,
    val tintColor: Int = 0xFFFFFFFF.toInt(),
    val windowX: Int = 0,
    val windowY: Int = 0,
    val cornerRadius: Float = 24f,
    val strokeWidth: Float = 1.2f,
    val borderColor: Int = 0x40FFFFFF.toInt(),
    val showSearchIcon: Boolean = true,
    val gridColumns: Int = 4,
    val iconSize: Float = 48f,
    val showLabels: Boolean = true,
    val labelColor: Int = 0xFFFFFFFF.toInt(),
    val labelFontSize: Float = 11f,
    val enableHapticFeedback: Boolean = true,
    val animationSpeedMultiplier: Float = 1.0f,
    val headerTitleText: String = "PORTAL",
    val enableAutoCollapse: Boolean = false,
    val puckColor: Int = 0xFF0284C7.toInt(),
    val puckOpacity: Float = 0.6f,
    val showLaunchToast: Boolean = true,
    val folderColumns: Int = 3,
    val folderCardSize: String = "Medium",
    val folderColor: Int = 0xFF0F172A.toInt(),
    val folderCornerRadius: Float = 16f,
    val showAccessibilityPuck: Boolean = false,
    val puckSize: Float = 56f
)

@Entity(tableName = "launcher_items")
data class LauncherItem(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val parentFolderId: String?,
    val position: Int,
    val packageName: String?,
    val className: String?
)
