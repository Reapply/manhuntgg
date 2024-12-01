package co.sakurastudios.manhuntgg.listeners

import co.sakurastudios.manhuntgg.engine.GameEngine
import co.sakurastudios.manhuntgg.managers.TeamManager
import co.sakurastudios.manhuntgg.utils.Messages
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PotionSplashEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class CombatListener(private val gameEngine: GameEngine) : Listener {
    private val lastDamageTime = ConcurrentHashMap<UUID, Long>()

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityDamage(event: EntityDamageByEntityEvent) {
        if (!gameEngine.isActive()) return

        val damager = event.damager
        val victim = event.entity

        if (damager !is Player || victim !is Player) return

        // Prevent friendly fire
        if (TeamManager.isHunter(damager) && TeamManager.isHunter(victim)) {
            event.isCancelled = true
            Messages.sendError(damager, "You cannot damage fellow Hunters!")
            return
        }

        // Track runner damage with explicit Runnable for type safety
        if (TeamManager.isRunner(victim)) {
            val now = System.currentTimeMillis()
            val lastDamage = lastDamageTime.getOrDefault(victim.uniqueId, 0L)

            if (now - lastDamage > 1000) { // Only broadcast every second
                lastDamageTime[victim.uniqueId] = now

                // Create explicit Runnable to avoid ambiguity
                val healthUpdateTask = Runnable {
                    if (victim.isOnline) {
                        Messages.broadcastInfo("Runner HP: ${String.format("%.1f", victim.health)}/20")
                    }
                }

                gameEngine.getPlugin().server.scheduler.runTask(
                    gameEngine.getPlugin(),
                    healthUpdateTask
                )
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPotionSplash(event: PotionSplashEvent) {
        if (!gameEngine.isActive()) return

        val thrower = event.entity.shooter as? Player ?: return

        if (TeamManager.isHunter(thrower)) {
            event.affectedEntities
                .filterIsInstance<Player>()
                .filter { TeamManager.isHunter(it) }
                .forEach { hunter ->
                    event.setIntensity(hunter, 0.0)
                }
        }
    }
}