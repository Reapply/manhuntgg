package co.sakurastudios.manhuntgg.listeners

import co.sakurastudios.manhuntgg.managers.GameManager
import co.sakurastudios.manhuntgg.managers.TeamManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

class ChatListener(
    private val teamManager: TeamManager,
    private val gameManager: GameManager
) : Listener {

    @EventHandler
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        if (!gameManager.isGameActive()) {
            return // If the game is not running, let chat proceed normally
        }

        val player = event.player
        if (teamManager.isHunter(player)) {
            event.isCancelled = true
            val hunterMessage = Component.text("[TEAM] ${player.name}: ${event.message}")
                .color(NamedTextColor.RED)

            event.recipients
                .filterIsInstance<Player>()
                .filter { teamManager.isHunter(it) }
                .forEach { it.sendMessage(hunterMessage) }
        }
    }
}