package co.sakurastudios.manhuntgg.listeners

import co.sakurastudios.manhuntgg.engine.GameEngine
import co.sakurastudios.manhuntgg.managers.TeamManager
import co.sakurastudios.manhuntgg.managers.WorldManager
import co.sakurastudios.manhuntgg.state.GameState
import co.sakurastudios.manhuntgg.utils.Messages
import org.bukkit.entity.EnderDragon
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerPortalEvent
import org.bukkit.event.world.WorldLoadEvent

class GameEventListener(private val gameEngine: GameEngine) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onDragonDeath(event: EntityDeathEvent) {
        if (gameEngine.getCurrentState() != GameState.RUNNING) return
        if (event.entity !is EnderDragon) return

        val killer = event.entity.killer
        if (killer != null && TeamManager.isRunner(killer)) {
            gameEngine.handleRunnerVictory("Runner defeated the Dragon!")
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPortalUse(event: PlayerPortalEvent) {
        if (!gameEngine.isActive()) return

        // Validate portal usage is within world border
        val destination = event.to ?: return
        if (!WorldManager.isLocationSafe(destination)) {
            event.isCancelled = true
            Messages.sendError(event.player, "You cannot use portals outside the border!")
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onWorldLoad(event: WorldLoadEvent) {
        if (event.world.name == "manhunt_world") {
            WorldManager.setupWorldBorder(event.world)
        }
    }
}