package org.tiwut.service

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

class QuickSettingsTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(
                this,
                "Permission required: Please enable 'Draw over other apps' in the main app.",
                Toast.LENGTH_LONG
            ).show()

            try {
                val intent = Intent(this, Class.forName("org.tiwut.MainActivity")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startActivityAndCollapse(intent)
                } else {
                    @Suppress("DEPRECATION")
                    startActivityAndCollapse(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }

        val intent = Intent(this, FloatingWindowService::class.java)
        if (FloatingWindowService.isRunning) {
            stopService(intent)
        } else {
            startService(intent)
        }

        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isActive = FloatingWindowService.isRunning

        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Portal Overlay"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isActive) "Active" else "Inactive"
        }
        
        tile.updateTile()
    }
}
