package co.sakurastudios.manhuntgg.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object Messages {
    private fun textColor(hex: String): TextColor {
        return TextColor.fromHexString(hex) ?: TextColor.color(255, 255, 255) // Fallback to white if invalid
    }

    fun broadcastInfo(message: String) {
        val formattedMessage = Component.text("[INFO] ", textColor("#00FFFF"))
            .append(Component.text(message, textColor("#FFFFFF")))
        Bukkit.getServer().sendMessage(formattedMessage)
    }

    fun broadcastError(message: String) {
        val formattedMessage = Component.text("[ERROR] ", textColor("#FF5555"))
            .append(Component.text(message, textColor("#FFFFFF")))
        Bukkit.getServer().sendMessage(formattedMessage)
    }

    fun broadcastShout(sender: CommandSender, message: String) {
        val formattedMessage = Component.text("[SHOUT] ", textColor("#00FFFF"))
            .append(Component.text("${sender.name}: ", textColor("#FFFFFF")))
            .append(Component.text(message, textColor("#FFFFFF")))
        Bukkit.getServer().sendMessage(formattedMessage)
    }

    fun broadcastManhunt(message: String) {
        val formattedMessage = Component.text("[MANHUNT] ", textColor("#FFD700"))
            .append(Component.text(message, textColor("#FFFFFF")))
        Bukkit.getServer().sendMessage(formattedMessage)
    }

    fun sendInfo(sender: CommandSender, message: String) {
        val formattedMessage = Component.text("[INFO] ", textColor("#00FFFF"))
            .append(Component.text(message, textColor("#FFFFFF")))
        sendToSender(sender, formattedMessage)
    }

    fun sendError(sender: CommandSender, message: String) {
        val formattedMessage = Component.text("[ERROR] ", textColor("#FF5555"))
            .append(Component.text(message, textColor("#FFFFFF")))
        sendToSender(sender, formattedMessage)
    }

    fun sendSuccess(sender: CommandSender, message: String) {
        val formattedMessage = Component.text("[SUCCESS] ", textColor("#55FF55"))
            .append(Component.text(message, textColor("#FFFFFF")))
        sendToSender(sender, formattedMessage)
    }

    private fun sendToSender(sender: CommandSender, message: Component) {
        if (sender is Player) {
            sender.sendMessage(message)
        } else {
            val plainText = PlainTextComponentSerializer.plainText().serialize(message)
            sender.sendMessage(plainText)
        }
    }
}
