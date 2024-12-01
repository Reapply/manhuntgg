package co.sakurastudios.manhuntgg.utils

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender

object Messages {
    fun broadcastInfo(message: String) {
        val formattedMessage = "${ChatColor.AQUA}[INFO] ${ChatColor.WHITE}$message"
        Bukkit.broadcastMessage(formattedMessage)
    }

    fun broadcastError(message: String) {
        val formattedMessage = "${ChatColor.RED}[ERROR] ${ChatColor.WHITE}$message"
        Bukkit.broadcastMessage(formattedMessage)
    }

    fun broadcastShout(sender: CommandSender, message: String) {
        val formattedMessage = "${ChatColor.AQUA}[SHOUT] ${ChatColor.WHITE}${sender.name}: $message"
        Bukkit.broadcastMessage(formattedMessage)
    }

    fun broadcastManhunt(message: String) {
        val formattedMessage = "${ChatColor.GOLD}[MANHUNT] ${ChatColor.WHITE}$message"
        Bukkit.broadcastMessage(formattedMessage)
    }

    fun sendInfo(sender: CommandSender, message: String) {
        sender.sendMessage("${ChatColor.AQUA}[INFO] ${ChatColor.WHITE}$message")
    }

    fun sendError(sender: CommandSender, message: String) {
        sender.sendMessage("${ChatColor.RED}[ERROR] ${ChatColor.WHITE}$message")
    }

    fun sendSuccess(sender: CommandSender, message: String) {
        sender.sendMessage("${ChatColor.GREEN}[SUCCESS] ${ChatColor.WHITE}$message")
    }
}