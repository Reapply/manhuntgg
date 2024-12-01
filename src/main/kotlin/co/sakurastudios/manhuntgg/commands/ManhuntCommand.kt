package co.sakurastudios.manhuntgg.commands

import co.sakurastudios.manhuntgg.engine.GameEngine
import co.sakurastudios.manhuntgg.utils.Messages
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class ManhuntCommand(private val gameEngine: GameEngine) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            Messages.sendError(sender, "Only players can use this command!")
            return true
        }

        if (!sender.hasPermission("manhunt.admin")) {
            Messages.sendError(sender, "You don't have permission to use this command!")
            return true
        }

        when (args.getOrNull(0)?.lowercase()) {
            "start" -> handleStart(sender)
            "stop" -> handleStop(sender)
            else -> Messages.sendInfo(sender, "Usage: /manhunt <start|stop>")
        }

        return true
    }

    private fun handleStart(player: Player) {
        if (gameEngine.startGame()) {
            Messages.sendSuccess(player, "Game started successfully!")
        } else {
            Messages.sendError(player, "Failed to start game. Is one already running?")
        }
    }

    private fun handleStop(player: Player) {
        gameEngine.forceStop()
        Messages.sendSuccess(player, "Game stopped successfully!")
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        return when {
            args.size <= 1 -> listOf("start", "stop").filter { it.startsWith(args.getOrNull(0) ?: "") }
            else -> emptyList()
        }
    }
}