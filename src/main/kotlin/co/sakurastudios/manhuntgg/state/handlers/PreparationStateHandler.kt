package co.sakurastudios.manhuntgg.state.handlers

import co.sakurastudios.manhuntgg.config.GameConfig
import co.sakurastudios.manhuntgg.managers.CompassManager
import co.sakurastudios.manhuntgg.managers.TeamManager
import co.sakurastudios.manhuntgg.managers.WorldManager
import co.sakurastudios.manhuntgg.utils.Messages
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

class PreparationStateHandler(
    plugin: JavaPlugin,
    config: GameConfig
) : BaseStateHandler(plugin, config) {
    private var gameWorld: World? = null
    private var preparationTask: BukkitTask? = null

    override fun onEnter() {
        isActive.set(true)
        initializeGameWorld()
        setupTeams()
        startPreparationPhase()
    }

    private fun initializeGameWorld() {
        try {
            gameWorld = WorldManager.createGameWorld(plugin).also { world ->
                WorldManager.setupWorldBorder(world, config.initialBorderSize)
            }
        } catch (e: Exception) {
            plugin.logger.severe("Failed to create game world: ${e.message}")
            Messages.broadcastError("Failed to create game world!")
        }
    }

    private fun setupTeams() {
        TeamManager.initialize()
        if (TeamManager.assignTeams(Bukkit.getOnlinePlayers())) {
            Messages.broadcastInfo("Teams have been assigned! Prepare yourselves!")
            Messages.broadcastInfo("Runners have ${config.preparationDuration.inWholeSeconds} seconds head start!")
        } else {
            Messages.broadcastError("Not enough players to start the game!")
        }
    }

    private fun startPreparationPhase() {
        preparationTask = plugin.server.scheduler.runTaskLater(plugin, Runnable {
            if (isActive.get()) {
                Messages.broadcastInfo("The hunt begins! Good luck!")
                CompassManager.startTracking(plugin)
            }
        }, config.preparationDuration.inWholeSeconds * 20L)
    }

    override fun onExit() {
        preparationTask?.cancel()
        preparationTask = null
        isActive.set(false)
    }

    override fun update() {

    }

    override fun cleanup() {
        super.cleanup()
        preparationTask?.cancel()
        preparationTask = null
    }
}
