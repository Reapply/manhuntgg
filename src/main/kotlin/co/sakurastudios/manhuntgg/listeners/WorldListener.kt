package co.sakurastudios.manhuntgg.listeners

import co.sakurastudios.manhuntgg.engine.GameEngine
import co.sakurastudios.manhuntgg.managers.TeamManager
import co.sakurastudios.manhuntgg.managers.WorldManager
import co.sakurastudios.manhuntgg.utils.Messages
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.world.PortalCreateEvent
import org.bukkit.event.world.WorldLoadEvent

class WorldListener(private val gameEngine: GameEngine) : Listener {

    @EventHandler
    fun onWorldChange(event: PlayerChangedWorldEvent) {
        if (!gameEngine.isActive()) return

        val player = event.player
        val toWorld = player.world

        if (toWorld.name == "manhunt_world" && !TeamManager.isParticipating(player)) {
            player.gameMode = GameMode.SPECTATOR
            Messages.sendInfo(player, "You've been set to spectator mode.")
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPortalCreate(event: PortalCreateEvent) {
        if (!gameEngine.isActive()) return
        if (event.world.name != "manhunt_world") {
            event.isCancelled = true
            return
        }

        // Ensure portals are within border
        val worldBorder = event.world.worldBorder
        event.blocks
            .filter { !worldBorder.isInside(it.location) }
            .forEach { it.type = Material.AIR }
    }

    @EventHandler
    fun onWorldLoad(event: WorldLoadEvent) {
        if (event.world.name == "manhunt_world") {
            WorldManager.setupWorldBorder(event.world)
        }
    }
}