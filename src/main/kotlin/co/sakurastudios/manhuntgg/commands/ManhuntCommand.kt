package co.sakurastudios.manhuntgg.commands

import co.sakurastudios.manhuntgg.services.GameService
import co.sakurastudios.manhuntgg.utils.MsgUtils
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ManhuntCommand(private val gameService: GameService) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            MsgUtils.sendError(sender, "Only players can use this command.")
            return false
        }

        if (args.isEmpty()) {
            MsgUtils.sendInfo(sender, "Usage: /manhunt <start|stop>")
            return false
        }

        when (args[0].lowercase()) {
            "start" -> gameService.startGame(sender)
            "stop" -> gameService.stopGame(sender)
            else -> MsgUtils.sendInfo(sender, "Usage: /manhunt <start|stop>")
        }

        return true
    }
}
