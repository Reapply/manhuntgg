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
        val player = event.player

        // Check if the game is active
        if (!gameManager.isGameActive()) {
            return // If the game is not running, do nothing and let chat proceed normally
        }

        // Check if the player is a hunter
        if (teamManager.isHunter(player)) {
            // Cancel the default message sending
            event.isCancelled = true

            // Prepare a message visible only to other hunters
            val hunterMessage = Component.text("[TEAM] ${player.name}: ${event.message}").color(NamedTextColor.RED)

            // Send the message only to hunters
            event.recipients.forEach {
                if (it is Player && teamManager.isHunter(it)) {
                    it.sendMessage(hunterMessage)
                }
            }
        }
    }
}
