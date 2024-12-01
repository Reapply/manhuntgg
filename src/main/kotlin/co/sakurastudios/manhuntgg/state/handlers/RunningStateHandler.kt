package co.sakurastudios.manhuntgg.state.handlers

import co.sakurastudios.manhuntgg.config.GameConfig
import co.sakurastudios.manhuntgg.managers.CompassManager
import co.sakurastudios.manhuntgg.managers.WorldManager
import co.sakurastudios.manhuntgg.utils.Messages
import org.bukkit.plugin.java.JavaPlugin

class RunningStateHandler(
    plugin: JavaPlugin,
    config: GameConfig
) : BaseStateHandler(plugin, config) {
    private var isFinalBorderReached = false

    override fun onEnter() {
        isActive.set(true)
        Messages.broadcastInfo("The game is now running! Hunters, track down the Runner!")
        startBorderShrinking()
    }

    private fun startBorderShrinking() {
        WorldManager.startBorderShrinking(
            plugin,
            config.initialBorderSize,
            config.finalBorderSize,
            config.borderShrinkTime
        ) { progress ->
            when {
                progress >= 1.0 -> {
                    isFinalBorderReached = true
                    Messages.broadcastInfo("Border has reached its final size!")
                }
                progress >= 0.75 -> Messages.broadcastInfo("Border is at 75%!")
                progress >= 0.50 -> Messages.broadcastInfo("Border is halfway!")
                progress >= 0.25 -> Messages.broadcastInfo("Border has started shrinking!")
            }
        }
    }

    override fun onExit() {
        WorldManager.stopBorderShrinking()
        CompassManager.stopTracking()
        isActive.set(false)
    }

    override fun update() {
    }

    override fun cleanup() {
        super.cleanup()
        WorldManager.stopBorderShrinking()
    }
}
