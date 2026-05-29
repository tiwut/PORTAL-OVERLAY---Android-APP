package org.tiwut.data.dao

import androidx.room.*
import org.tiwut.data.entity.LauncherItem
import org.tiwut.data.entity.LauncherSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface LauncherDao {
    @Query("SELECT * FROM launcher_settings WHERE id = 0")
    fun getSettingsFlow(): Flow<LauncherSettings?>

    @Query("SELECT * FROM launcher_settings WHERE id = 0")
    suspend fun getSettings(): LauncherSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: LauncherSettings)

    @Query("SELECT * FROM launcher_items ORDER BY position ASC")
    fun getAllItemsFlow(): Flow<List<LauncherItem>>

    @Query("SELECT * FROM launcher_items ORDER BY position ASC")
    suspend fun getAllItems(): List<LauncherItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: LauncherItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<LauncherItem>)

    @Delete
    suspend fun deleteItem(item: LauncherItem)

    @Query("DELETE FROM launcher_items WHERE id = :id")
    suspend fun deleteItemById(id: String)

    @Query("DELETE FROM launcher_items WHERE parentFolderId = :folderId")
    suspend fun deleteItemsInFolder(folderId: String)

    @Query("DELETE FROM launcher_items")
    suspend fun clearAllItems()
}
