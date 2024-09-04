package co.sakurastudios.manhuntgg.managers

import co.sakurastudios.manhuntgg.utils.MsgUtils
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.atomic.AtomicBoolean

class GameManager(
    private val plugin: JavaPlugin,
    private val lobbyManager: LobbyManager,
    val worldManager: WorldManager,
    private val teamManager: TeamManager,
    private val compassManager: CompassManager,
    private val victoryManager: VictoryManager
) {
    private val isGameActiveOrStarting = AtomicBoolean(false)

    fun isGameActive() = isGameActiveOrStarting.get()

    fun start(): Boolean {
        if (!isGameActiveOrStarting.compareAndSet(false, true)) {
            MsgUtils.broadcastError("A Manhunt game is already active or starting!")
            return false
        }

        return try {
            setupLobbyAndScheduleStart()
        } catch (e: Exception) {
            handleError(e, "Error during Manhunt game start")
            false
        }
    }

    private fun setupLobbyAndScheduleStart(): Boolean {
        if (!lobbyManager.setupLobby()) {
            MsgUtils.broadcastError("Failed to set up the lobby. Aborting game start.")
            cleanup()
            return false
        }

        scheduleGameStart()
        return true
    }

    private fun scheduleGameStart() {
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            try {
                startGame()
            } catch (e: Exception) {
                handleError(e, "Error during Manhunt world setup")
            }
        }, 20 * 60) // 60 seconds delay
    }

    private fun startGame() {
        if (setupManhuntWorld() && setupTeams()) {
            compassManager.giveCompassToHunters()
            compassManager.startCompassTracking()
            victoryManager.initialize()
            MsgUtils.broadcastManhunt("Manhunt has started! Good luck to all players!")
        }
    }

    private fun setupManhuntWorld(): Boolean {
        if (!worldManager.setupManhuntWorld()) {
            MsgUtils.broadcastError("Failed to set up the Manhunt world. Aborting game start.")
            cleanup()
            return false
        }
        return true
    }

    private fun setupTeams(): Boolean {
        if (!teamManager.setupTeams()) {
            MsgUtils.broadcastError("Failed to set up teams. Aborting game start.")
            cleanup()
            return false
        }
        return true
    }

    fun stop() {
        if (!isGameActiveOrStarting.get()) {
            MsgUtils.broadcastError("Manhunt is not active!")
            return
        }

        try {
            cleanup()
            MsgUtils.broadcastManhunt("Manhunt has stopped! Thanks for playing!")
        } catch (e: Exception) {
            handleError(e, "Error during Manhunt game stop")
        }
    }

    private fun cleanup() {
        worldManager.cleanupManhuntWorld()
        lobbyManager.cleanupLobby()
        compassManager.stopCompassTracking()
        isGameActiveOrStarting.set(false)
        teamManager.resetTeams()
    }

    private fun handleError(e: Exception, message: String) {
        plugin.logger.severe("$message: ${e.message}")
        e.printStackTrace()
        cleanup()
        MsgUtils.broadcastError("Failed to manage Manhunt due to an error. Check console for details.")
    }
}