package co.sakurastudios.manhuntgg.services

import co.sakurastudios.manhuntgg.managers.GameManager
import co.sakurastudios.manhuntgg.utils.MsgUtils
import org.bukkit.entity.Player

class GameService(private val gameManager: GameManager) {

    fun startGame(player: Player) {
        if (gameManager.isGameActive()) {
            MsgUtils.sendError(player, "Manhunt is already active.")
            return
        }

        val success = gameManager.start()
        val message = if (success) "Manhunt started." else "Failed to start Manhunt. Check console for details."
        val sendMessage = if (success) MsgUtils::sendSuccess else MsgUtils::sendError
        sendMessage(player, message)
    }

    fun stopGame(player: Player) {
        if (!gameManager.isGameActive()) {
            MsgUtils.sendError(player, "No active Manhunt game to stop.")
            return
        }

        gameManager.stop()
        MsgUtils.sendSuccess(player, "Manhunt stopped.")
    }

    fun stopGameOnShutdown() {
        if (gameManager.isGameActive()) {
            gameManager.stop()
        }
    }
}