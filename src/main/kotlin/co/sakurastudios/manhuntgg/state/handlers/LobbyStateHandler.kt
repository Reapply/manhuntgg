package co.sakurastudios.manhuntgg.state.handlers

import co.sakurastudios.manhuntgg.config.GameConfig
import co.sakurastudios.manhuntgg.managers.WorldManager
import co.sakurastudios.manhuntgg.utils.Messages
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.World
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

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
                    showTitle("Manhunt", "Waiting for players...", "#00FFFF", "#AAAAAA")
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
        isActive.set(false)
    }

    override fun update() {
    }


    override fun cleanup() {
        super.cleanup()
        WorldManager.cleanupLobbyWorld()
        countdownTask?.cancel()
        countdownTask = null
    }
}
