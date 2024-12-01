package co.sakurastudios.manhuntgg.listeners

import co.sakurastudios.manhuntgg.engine.GameEngine
import co.sakurastudios.manhuntgg.managers.TeamManager
import co.sakurastudios.manhuntgg.managers.WorldManager
import co.sakurastudios.manhuntgg.utils.Messages
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PlayerListener(private val gameEngine: GameEngine) : Listener {
    private val deathLocations = ConcurrentHashMap<UUID, Location>()
    private val respawnTasks = ConcurrentHashMap<UUID, Int>()

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!gameEngine.isActive()) return

        gameEngine.getPlugin().server.scheduler.runTaskLater(gameEngine.getPlugin(), Runnable {
            val player = event.player
            when {
                TeamManager.isRunner(player) -> {
                    Messages.sendInfo(player, "Welcome back, Runner!")
                    TeamManager.reapplyRunnerEffects(player)
                }
                TeamManager.isHunter(player) -> {
                    Messages.sendInfo(player, "Welcome back, Hunter!")
                    TeamManager.reapplyHunterEffects(player)
                }
                else -> {
                    player.gameMode = GameMode.SPECTATOR
                    Messages.sendInfo(player, "You've joined as a spectator.")
                }
            }
        }, 20L)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (!gameEngine.isActive()) return

        val player = event.player
        when {
            TeamManager.isRunner(player) -> {
                Messages.broadcastManhunt("The Runner has disconnected! Game paused for 5 minutes.")
                gameEngine.initiateRunnerDisconnectTimer(player)
            }
            TeamManager.isHunter(player) -> {
                Messages.broadcastManhunt("Hunter ${player.name} has disconnected!")
                respawnTasks.remove(player.uniqueId)?.let {
                    gameEngine.getPlugin().server.scheduler.cancelTask(it)
                }
            }
        }
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        if (!gameEngine.isActive()) return

        val player = event.entity
        deathLocations[player.uniqueId] = player.location.clone()
        event.deathMessage(null)

        when {
            TeamManager.isRunner(player) -> {
                Messages.broadcastManhunt("The Runner has been eliminated! Hunters win!")
                gameEngine.handleHunterVictory("Runner eliminated")
            }
            TeamManager.isHunter(player) -> handleHunterDeath(player)
        }
    }

    private fun handleHunterDeath(player: Player) {
        if (gameEngine.isFinalBorderReached()) {
            Messages.broadcastManhunt("${player.name} has been eliminated!")
            TeamManager.eliminateHunter(player)
            checkForRunnerVictory()
        } else {
            Messages.broadcastManhunt("${player.name} will respawn in 5 seconds!")
            scheduleHunterRespawn(player)
        }
    }

    private fun scheduleHunterRespawn(player: Player) {
        gameEngine.getPlugin().server.scheduler.runTaskLater(
            gameEngine.getPlugin(),
            Runnable {
                if (!player.isOnline || !gameEngine.isActive()) return@Runnable

                val runner = TeamManager.getRunner()
                if (runner != null && runner.isOnline) {
                    val respawnLocation = WorldManager.findSafeSpawnLocation(
                        runner.location,
                        gameEngine.getConfig().hunterRespawnRadius
                    )
                    player.spigot().respawn()
                    player.teleport(respawnLocation)
                    TeamManager.reapplyHunterEffects(player)
                    Messages.sendSuccess(player, "You have respawned!")
                }
            },
            100L // 5 seconds
        )
    }

    private fun checkForRunnerVictory() {
        if (TeamManager.getActiveHunters().isEmpty()) {
            Messages.broadcastManhunt("All Hunters have been eliminated! Runner wins!")
            gameEngine.handleRunnerVictory("All hunters eliminated")
        }
    }

    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        if (!gameEngine.isActive()) return

        val deathLoc = deathLocations[event.player.uniqueId]
        if (deathLoc != null) {
            event.respawnLocation = deathLoc
            deathLocations.remove(event.player.uniqueId)
        }
    }
}