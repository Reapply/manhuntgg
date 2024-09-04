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

    fun isGameActive(): Boolean = isGameActiveOrStarting.get()

    fun start(): Boolean {
        if (!isGameActiveOrStarting.compareAndSet(false, true)) {
            MsgUtils.broadcastError("A Manhunt game is already active or starting!")
            return false
        }

        return try {
            if (!setupLobby()) return false

            scheduleGameStart()
            true
        } catch (e: Exception) {
            handleStartError(e)
            false
        }
    }

    private fun setupLobby(): Boolean {
        if (!lobbyManager.setupLobby()) {
            MsgUtils.broadcastError("Failed to set up the lobby. Aborting game start.")
            cleanup()
            return false
        }
        return true
    }

    private fun scheduleGameStart() {
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            try {
                if (setupManhuntWorld() && setupTeams()) {
                    compassManager.giveCompassToHunters()
                    compassManager.startCompassTracking()
                    victoryManager.initialize()
                    MsgUtils.broadcastManhunt("Manhunt has started! Good luck to all players!")
                }
            } catch (e: Exception) {
                handleStartError(e, "Error during Manhunt world setup")
            }
        }, 20 * 60) // Delay start to allow lobby time, e.g., 60 seconds
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

    private fun handleStartError(e: Exception, message: String = "Error during Manhunt game start") {
        plugin.logger.severe("$message: ${e.message}")
        e.printStackTrace()
        cleanup()
        MsgUtils.broadcastError("Failed to start Manhunt due to an error. Check console for details.")
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
            plugin.logger.severe("Error during Manhunt game stop: ${e.message}")
            e.printStackTrace()
            MsgUtils.broadcastError("An error occurred while stopping Manhunt. Check console for details.")
        }
    }

    private fun cleanup() {
        worldManager.cleanupManhuntWorld()
        lobbyManager.cleanupLobby()
        compassManager.stopCompassTracking()
        isGameActiveOrStarting.set(false)
        teamManager.resetTeams()
    }
}