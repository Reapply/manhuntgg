package co.sakurastudios.manhuntgg.state

import co.sakurastudios.manhuntgg.config.GameConfig
import co.sakurastudios.manhuntgg.managers.CompassManager
import co.sakurastudios.manhuntgg.managers.TeamManager
import co.sakurastudios.manhuntgg.managers.WorldManager
import co.sakurastudios.manhuntgg.utils.Messages
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.World
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Base state handler with common functionality
 */
abstract class BaseStateHandler(
    protected val plugin: JavaPlugin,
    protected val config: GameConfig
) : GameStateHandler {
    protected var isActive = AtomicBoolean(false)

    override fun cleanup() {
        isActive.set(false)
    }
}

/**
 * Handles the lobby phase where players wait for game start
 */
class LobbyStateHandler(
    plugin: JavaPlugin,
    config: GameConfig
) : BaseStateHandler(plugin, config) {
    private var countdownTask: BukkitTask? = null
    private var countdownSeconds: Int = config.lobbyDuration.inWholeSeconds.toInt()
    private var lobbyWorld: World? = null

    override fun onEnter() {
        isActive.set(true)
        initializeLobbyWorld()
        startCountdown()
    }

    private fun initializeLobbyWorld() {
        val world = WorldManager.createLobbyWorld(plugin)
        if (world != null) {
            lobbyWorld = world
            Bukkit.getOnlinePlayers().forEach { player ->
                player.apply {
                    gameMode = GameMode.ADVENTURE
                    inventory.clear()
                    teleport(world.spawnLocation.clone().add(0.0, 1.0, 0.0))
                    sendTitle("§bManhunt", "§7Waiting for players...", 10, 70, 20)
                }
            }
        } else {
            Messages.broadcastError("Failed to create lobby world!")
            isActive.set(false)
        }
    }

    private fun startCountdown() {
        countdownTask?.cancel()
        countdownTask = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            if (!isActive.get()) {
                countdownTask?.cancel()
                return@Runnable
            }

            when (countdownSeconds) {
                60, 30, 15, 10, 5, 4, 3, 2, 1 -> {
                    Messages.broadcastInfo("Game starts in $countdownSeconds seconds!")
                    Bukkit.getOnlinePlayers().forEach { player ->
                        player.playSound(player.location, "block.note_block.pling", 1f, 1f)
                    }
                }
                0 -> {
                    countdownTask?.cancel()
                    if (Bukkit.getOnlinePlayers().size >= config.minPlayers) {
                        Messages.broadcastInfo("Game starting!")
                    } else {
                        Messages.broadcastError("Not enough players! Resetting countdown...")
                        countdownSeconds = config.lobbyDuration.inWholeSeconds.toInt()
                        startCountdown()
                    }
                }
            }
            countdownSeconds--
        }, 0L, 20L)
    }

    override fun onExit() {
        countdownTask?.cancel()
        countdownTask = null
    }

    override fun update() {
        // Update player count display if needed
    }

    override fun cleanup() {
        super.cleanup()
        WorldManager.cleanupLobbyWorld()
        countdownTask?.cancel()
        countdownTask = null
    }
}
/**
 * Handles the preparation phase where teams are assigned
 */
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
    }

    override fun update() {
        // Update preparation phase status
    }

    override fun cleanup() {
        super.cleanup()
        preparationTask?.cancel()
        preparationTask = null
    }
}

/**
 * Handles the main game phase
 */
class RunningStateHandler(
    plugin: JavaPlugin,
    config: GameConfig
) : BaseStateHandler(plugin, config) {
    private var isFinalBorderReached = false
    private var borderUpdateTask: BukkitTask? = null

    override fun onEnter() {
        isActive.set(true)
        Messages.broadcastInfo("The game is now running! Hunters, track down the Runner!")
        startBorderShrinking()
    }

    private fun startBorderShrinking() {
        WorldManager.startBorderShrinking(
            plugin,
            config.initialBorderSize,
            config.finalBorderSize,
            config.borderShrinkTime
        ) { progress ->
            when {
                progress >= 1.0 -> {
                    isFinalBorderReached = true
                    Messages.broadcastInfo("§c§lBorder has reached its final size!")
                }
                progress >= 0.75 -> Messages.broadcastInfo("§e§lBorder is at 75%!")
                progress >= 0.50 -> Messages.broadcastInfo("§e§lBorder is halfway!")
                progress >= 0.25 -> Messages.broadcastInfo("§e§lBorder has started shrinking!")
            }
        }
    }

    override fun onExit() {
        WorldManager.stopBorderShrinking()
        CompassManager.stopTracking()
        borderUpdateTask?.cancel()
    }

    override fun update() {
        // Update game status and check win conditions
    }

    override fun cleanup() {
        super.cleanup()
        WorldManager.stopBorderShrinking()
        borderUpdateTask?.cancel()
    }
}

/**
 * Handles the game ending phase
 */
class EndingStateHandler(
    plugin: JavaPlugin,
    config: GameConfig
) : BaseStateHandler(plugin, config) {
    override fun onEnter() {
        isActive.set(true)
        handleGameEnd()
    }

    private fun handleGameEnd() {
        Bukkit.getOnlinePlayers().forEach { player ->
            player.gameMode = GameMode.SPECTATOR
            player.sendTitle("§6Game Over!", "§7Thanks for playing!", 10, 70, 20)
        }
        Messages.broadcastInfo("Game Over! Thanks for playing!")
    }

    override fun onExit() {
        TeamManager.reset()
    }

    override fun update() {
        // Update end game statistics if needed
    }

    override fun cleanup() {
        super.cleanup()
        WorldManager.cleanupGameWorld()
    }
}