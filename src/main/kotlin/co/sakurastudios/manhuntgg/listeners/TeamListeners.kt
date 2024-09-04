package co.sakurastudios.manhuntgg.listeners

import co.sakurastudios.manhuntgg.managers.GameManager
import co.sakurastudios.manhuntgg.managers.TeamManager
import co.sakurastudios.manhuntgg.utils.MsgUtils
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin

class TeamListeners(
    private val teamManager: TeamManager,
    private val gameManager: GameManager,
    private val plugin: JavaPlugin
) : Listener {

    @EventHandler
    fun onPlayerDamage(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        val victim = event.entity

        if (damager is Player && victim is Player && teamManager.areTeammates(damager, victim)) {
            event.isCancelled = true
            MsgUtils.sendError(damager, "You can't damage your fellow Hunters!")
        }
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        if (player.world == Bukkit.getWorld("manhunt_world")) {
            when {
                teamManager.isHunter(player) -> {
                    val runner = teamManager.getRunner()
                    player.teleport(runner.location.add(0.0, 1.0, 0.0))
                    MsgUtils.broadcastError("${player.name} (Hunter) has died")
                }
                teamManager.isRunner(player) -> {
                    player.gameMode = GameMode.SPECTATOR
                    MsgUtils.broadcastError("${player.name} (Runner) has died! The Hunters win!")
                    gameManager.stop()
                }
            }
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (gameManager.isGameActive()) {
            teamManager.reassignPlayerToTeam(player)
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        when {
            teamManager.isHunter(player) -> {
                teamManager.removeHunter(player)
                MsgUtils.broadcastError("${player.name} (Hunter) has left the game.")
            }
            teamManager.isRunner(player) -> {
                MsgUtils.broadcastError("${player.name} (Runner) has left the game. The game will end.")
                gameManager.stop()
            }
        }
    }

    fun registerListeners() {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }
}