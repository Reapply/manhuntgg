package co.sakurastudios.manhuntgg.commands

import co.sakurastudios.manhuntgg.managers.GameManager
import co.sakurastudios.manhuntgg.managers.TeamManager
import co.sakurastudios.manhuntgg.utils.MsgUtils
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class ShoutCommand(
    private val teamManager: TeamManager,
    private val gameManager: GameManager
) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            MsgUtils.sendError(sender, "Only players can use this command.")
            return true
        }

        if (!gameManager.isGameActive()) {
            MsgUtils.sendError(sender, "The game is not currently running.")
            return true
        }

        if (!teamManager.isHunter(sender)) {
            MsgUtils.sendError(sender, "Only Hunters can use /shout.")
            return true
        }

        if (args.isEmpty()) {
            MsgUtils.sendError(sender, "Usage: /shout <message>")
            return true
        }

        val message = args.joinToString(" ")
        val shoutMessage = Component.text("[SHOUT] ${sender.name}: $message").color(NamedTextColor.RED)

        sender.server.onlinePlayers.forEach { it.sendMessage(shoutMessage) }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> = mutableListOf()
}