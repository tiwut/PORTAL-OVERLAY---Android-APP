package org.tiwut.data.repository

import org.tiwut.data.dao.LauncherDao
import org.tiwut.data.entity.LauncherItem
import org.tiwut.data.entity.LauncherSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LauncherRepository(private val launcherDao: LauncherDao) {

    val launcherSettings: Flow<LauncherSettings> = launcherDao.getSettingsFlow().map {
        it ?: LauncherSettings()
    }

    suspend fun getSettings(): LauncherSettings {
        return launcherDao.getSettings() ?: LauncherSettings()
    }

    suspend fun ensureSettings() {
        if (launcherDao.getSettings() == null) {
            launcherDao.insertSettings(LauncherSettings())
        }
    }

    suspend fun updateSettings(settings: LauncherSettings) {
        launcherDao.insertSettings(settings)
    }

    val launcherItems: Flow<List<LauncherItem>> = launcherDao.getAllItemsFlow()

    suspend fun getItems(): List<LauncherItem> {
        return launcherDao.getAllItems()
    }

    suspend fun insertItem(item: LauncherItem) {
        launcherDao.insertItem(item)
    }

    suspend fun insertItems(items: List<LauncherItem>) {
        launcherDao.insertItems(items)
    }

    suspend fun deleteItem(item: LauncherItem) {
        launcherDao.deleteItem(item)
        if (item.type == "FOLDER") {
            launcherDao.deleteItemsInFolder(item.id)
        }
    }

    suspend fun deleteItemById(id: String) {
        launcherDao.deleteItemById(id)
    }

    suspend fun clearAllItems() {
        launcherDao.clearAllItems()
    }

    suspend fun updateWindowPosition(x: Int, y: Int) {
        val current = getSettings()
        updateSettings(current.copy(windowX = x, windowY = y))
    }
}
