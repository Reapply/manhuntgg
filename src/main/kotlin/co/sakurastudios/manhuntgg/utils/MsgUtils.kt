package co.sakurastudios.manhuntgg.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object MsgUtils {

    private val miniMessage = MiniMessage.miniMessage()

    // Predefined message prefixes using MiniMessage for styling
    private const val MANHUNT_PREFIX = "<gold><bold>MANHUNT!</bold></gold> "
    private const val ERROR_PREFIX = "<red><bold>ERROR!</bold></red> "
    private const val SUCCESS_PREFIX = "<green><bold>SUCCESS!</bold></green> "
    private const val INFO_PREFIX = "<aqua><bold>INFO!</bold></aqua> "

    // Create a message with a specific prefix and color
    private fun createMessage(prefix: String, message: String): Component {
        return miniMessage.deserialize("$prefix$message")
    }

    // Send styled messages to players
    fun sendManhunt(player: Player, message: String) {
        player.sendMessage(createMessage(MANHUNT_PREFIX, message))
    }

    fun sendError(player: Player, message: String) {
        player.sendMessage(createMessage(ERROR_PREFIX, message))
    }

    fun sendSuccess(player: Player, message: String) {
        player.sendMessage(createMessage(SUCCESS_PREFIX, message))
    }

    fun sendInfo(player: Player, message: String) {
        player.sendMessage(createMessage(INFO_PREFIX, message))
    }

    // Send styled messages to CommandSender (e.g., players, console)
    fun sendManhunt(sender: CommandSender, message: String) {
        sender.sendMessage(createMessage(MANHUNT_PREFIX, message))
    }

    fun sendError(sender: CommandSender, message: String) {
        sender.sendMessage(createMessage(ERROR_PREFIX, message))
    }

    fun sendSuccess(sender: CommandSender, message: String) {
        sender.sendMessage(createMessage(SUCCESS_PREFIX, message))
    }

    fun sendInfo(sender: CommandSender, message: String) {
        sender.sendMessage(createMessage(INFO_PREFIX, message))
    }

    // Broadcast styled messages to all players
    fun broadcastManhunt(message: String) {
        Bukkit.getServer().sendMessage(createMessage(MANHUNT_PREFIX, message))
    }

    fun broadcastError(message: String) {
        Bukkit.getServer().sendMessage(createMessage(ERROR_PREFIX, message))
    }

    fun broadcastSuccess(message: String) {
        Bukkit.getServer().sendMessage(createMessage(SUCCESS_PREFIX, message))
    }

    fun broadcastInfo(message: String) {
        Bukkit.getServer().sendMessage(createMessage(INFO_PREFIX, message))
    }
}
