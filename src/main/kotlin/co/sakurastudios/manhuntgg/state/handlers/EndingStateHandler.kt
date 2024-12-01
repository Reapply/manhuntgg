package co.sakurastudios.manhuntgg.state.handlers

import co.sakurastudios.manhuntgg.config.GameConfig
import co.sakurastudios.manhuntgg.managers.TeamManager
import co.sakurastudios.manhuntgg.managers.WorldManager
import co.sakurastudios.manhuntgg.utils.Messages
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.plugin.java.JavaPlugin

class EndingStateHandler(
    plugin: JavaPlugin,
    config: GameConfig
) : BaseStateHandler(plugin, config) {
    override fun onEnter() {
        isActive.set(true)
        handleGameEnd()
    }

    private fun handleGameEnd() {
        Bukkit.getOnlinePlayers().forEach { player ->
            player.gameMode = GameMode.SPECTATOR
            player.showTitle("Game Over!", "Thanks for playing!", "#FFD700", "#AAAAAA")
        }
        Messages.broadcastInfo("Game Over! Thanks for playing!")
    }

    override fun onExit() {
        isActive.set(false)
    }

    override fun update() {
    }

    override fun cleanup() {
        super.cleanup()
        WorldManager.cleanupGameWorld()
        TeamManager.reset()
    }
}
