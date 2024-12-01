package co.sakurastudios.manhuntgg.engine

import co.sakurastudios.manhuntgg.config.GameConfig
import co.sakurastudios.manhuntgg.events.GameStateChangeEvent
import co.sakurastudios.manhuntgg.managers.CompassManager
import co.sakurastudios.manhuntgg.managers.GameScoreboardManager

import co.sakurastudios.manhuntgg.managers.TeamManager
import co.sakurastudios.manhuntgg.managers.WorldManager
import co.sakurastudios.manhuntgg.state.*
import co.sakurastudios.manhuntgg.utils.Messages
import kotlinx.coroutines.*
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.seconds

class GameEngine(private val plugin: JavaPlugin) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val state = AtomicReference<GameState>(GameState.IDLE)
    private val config = GameConfig.load(plugin)
    private val stateHandlers = mutableMapOf<GameState, GameStateHandler>()
    private val scoreboardManager = GameScoreboardManager(this)
    private var runnerDisconnectTask: Int = -1
    private var isFinalBorderReached = false

    init {
        initializeStateHandlers()
        startGameLoop()
        scoreboardManager.startUpdating() // Start scoreboard in IDLE state
    }

    fun getPlugin(): JavaPlugin = plugin
    fun getCurrentState(): GameState = state.get()
    fun isActive(): Boolean = state.get() != GameState.IDLE
    fun getConfig(): GameConfig = config
    fun isFinalBorderReached(): Boolean = isFinalBorderReached

    private fun initializeStateHandlers() {
        stateHandlers[GameState.LOBBY] = LobbyStateHandler(plugin, config)
        stateHandlers[GameState.PREPARATION] = PreparationStateHandler(plugin, config)
        stateHandlers[GameState.RUNNING] = RunningStateHandler(plugin, config)
        stateHandlers[GameState.ENDING] = EndingStateHandler(plugin, config)
    }

    private fun startGameLoop() {
        scope.launch {
            while (isActive) {
                try {
                    getCurrentState().let { currentState ->
                        stateHandlers[currentState]?.update()
                    }
                    delay(50) // 20 TPS
                } catch (e: Exception) {
                    plugin.logger.severe("Game loop error: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    fun startGame(): Boolean {
        // Check if a game is already running
        if (isActive()) {
            Messages.sendError(plugin.server.consoleSender, "A game is already running!")
            return false
        }

        // Check player count
        val players = plugin.server.onlinePlayers
        if (players.size < config.minPlayers) {
            Messages.broadcastError("Not enough players! Need at least ${config.minPlayers} players to start.")
            return false
        }

        if (players.size > config.maxPlayers) {
            Messages.broadcastError("Too many players! Maximum is ${config.maxPlayers} players.")
            return false
        }

        return try {
            // Always transition from IDLE to LOBBY first
            if (state.compareAndSet(GameState.IDLE, GameState.LOBBY)) {
                object : BukkitRunnable() {
                    override fun run() {
                        try {
                            stateHandlers[GameState.LOBBY]?.onEnter()
                            plugin.server.pluginManager.callEvent(GameStateChangeEvent(GameState.IDLE, GameState.LOBBY))
                        } catch (e: Exception) {
                            plugin.logger.severe("Failed to start game: ${e.message}")
                            forceStop()
                        }
                    }
                }.runTask(plugin)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            plugin.logger.severe("Failed to start game: ${e.message}")
            forceStop()
            false
        }
    }

    fun handleRunnerVictory(reason: String) {
        Messages.broadcastManhunt("Runner Victory: $reason")
        scheduleGameEnd(5.seconds)
    }

    fun handleHunterVictory(reason: String) {
        Messages.broadcastManhunt("Hunters Victory: $reason")
        scheduleGameEnd(5.seconds)
    }

    private fun scheduleGameEnd(delay: kotlin.time.Duration) {
        plugin.server.scheduler.scheduleSyncDelayedTask(plugin, {
            transitionToState(GameState.ENDING)
        }, delay.inWholeSeconds * 20L)
    }

    private fun transitionToState(newState: GameState) {
        val oldState = state.get()
        if (!oldState.canTransitionTo(newState)) {
            throw IllegalStateException("Invalid state transition: $oldState -> $newState")
        }

        if (state.compareAndSet(oldState, newState)) {
            object : BukkitRunnable() {
                override fun run() {
                    try {
                        stateHandlers[oldState]?.onExit()
                        plugin.server.pluginManager.callEvent(GameStateChangeEvent(oldState, newState))
                        stateHandlers[newState]?.onEnter()

                        // Update scoreboard for new state
                        scoreboardManager.startUpdating()

                        when (newState) {
                            GameState.PREPARATION -> scheduleNextState(GameState.RUNNING, config.preparationDuration)
                            GameState.RUNNING -> startBorderShrinking()
                            GameState.ENDING -> scheduleNextState(GameState.IDLE, config.endingDuration)
                            else -> {} // No scheduled transition
                        }
                    } catch (e: Exception) {
                        plugin.logger.severe("State transition failed: ${e.message}")
                        forceStop()
                    }
                }
            }.runTask(plugin)
        }
    }

    private fun scheduleNextState(nextState: GameState, delay: kotlin.time.Duration) {
        plugin.server.scheduler.scheduleSyncDelayedTask(plugin, {
            transitionToState(nextState)
        }, delay.inWholeSeconds * 20L)
    }

    fun initiateRunnerDisconnectTimer(runner: Player) {
        cancelRunnerDisconnectTimer()

        runnerDisconnectTask = plugin.server.scheduler.scheduleSyncDelayedTask(plugin, {
            if (!isActive()) return@scheduleSyncDelayedTask

            if (!runner.isOnline) {
                Messages.broadcastError("Runner did not reconnect in time! Hunters win!")
                handleHunterVictory("Runner disconnect timeout")
            }
        }, 20L * 300) // 5 minutes
    }

    private fun cancelRunnerDisconnectTimer() {
        if (runnerDisconnectTask != -1) {
            plugin.server.scheduler.cancelTask(runnerDisconnectTask)
            runnerDisconnectTask = -1
        }
    }

    fun forceStop() {
        val currentState = state.get()
        object : BukkitRunnable() {
            override fun run() {
                cancelRunnerDisconnectTimer()
                stateHandlers[currentState]?.onExit()
                state.set(GameState.IDLE)
                plugin.server.pluginManager.callEvent(GameStateChangeEvent(currentState, GameState.IDLE))
                cleanup()
            }
        }.runTask(plugin)
    }

    fun shutdown() {
        scope.cancel()
        forceStop()
    }

    private fun cleanup() {
        WorldManager.stopBorderShrinking()
        CompassManager.stopTracking()
        scoreboardManager.stopUpdating()
        stateHandlers.values.forEach { it.cleanup() }

        plugin.server.onlinePlayers.forEach { player ->
            player.gameMode = GameMode.SURVIVAL
            player.inventory.clear()
            TeamManager.resetPlayerState(player)
        }
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
                    Messages.broadcastInfo("Border has reached its final size!")
                }
                progress >= 0.75 -> Messages.broadcastInfo("Border is at 75%!")
                progress >= 0.50 -> Messages.broadcastInfo("Border is halfway!")
                progress >= 0.25 -> Messages.broadcastInfo("Border has started shrinking!")
            }
        }
    }
}