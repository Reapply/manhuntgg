package co.sakurastudios.manhuntgg.listeners

import co.sakurastudios.manhuntgg.events.GameStateChangeEvent
import co.sakurastudios.manhuntgg.managers.GameScoreboardManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class ScoreboardListener(private val scoreboardManager: GameScoreboardManager) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        // Update the player's scoreboard
        scoreboardManager.updatePlayerScoreboard(event.player.uniqueId)
    }

    @EventHandler
    fun onStateChange(event: GameStateChangeEvent) {
        // Restart scoreboard updates when the game state changes
        scoreboardManager.startUpdating()
    }
}
