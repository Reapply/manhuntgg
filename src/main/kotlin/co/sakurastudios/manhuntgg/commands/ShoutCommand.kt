package co.sakurastudios.manhuntgg.commands

import co.sakurastudios.manhuntgg.engine.GameEngine
import co.sakurastudios.manhuntgg.managers.TeamManager
import co.sakurastudios.manhuntgg.utils.Messages
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ShoutCommand(private val gameEngine: GameEngine) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            Messages.sendError(sender, "Only players can use this command!")
            return true
        }

        if (!gameEngine.isActive()) {
            Messages.sendError(sender, "No game is currently running!")
            return true
        }

        if (!TeamManager.isHunter(sender)) {
            Messages.sendError(sender, "Only hunters can use the shout command!")
            return true
        }

        if (args.isEmpty()) {
            Messages.sendError(sender, "Usage: /shout <message>")
            return true
        }

        val message = args.joinToString(" ")
        Messages.broadcastShout(sender, message)
        return true
    }
}