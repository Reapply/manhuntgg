package co.sakurastudios.manhuntgg.managers

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

class CompassManager(private val plugin: JavaPlugin, private val teamManager: TeamManager) {

    private val compassCooldowns = mutableMapOf<Player, Long>()
    private var compassTask: BukkitTask? = null

    fun startCompassTracking() {
        compassTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            teamManager.getHunters().forEach { hunter ->
                if (hunter.inventory.itemInMainHand.type == Material.COMPASS) {
                    updateCompass(hunter)
                }
            }
        }, 0L, 20L) // Check every second
    }

    fun stopCompassTracking() {
        compassTask?.cancel()
    }

    private fun updateCompass(hunter: Player) {
        val runner = teamManager.getRunner()
        val currentTime = System.currentTimeMillis()
        val lastUseTime = compassCooldowns[hunter] ?: 0L

        if (currentTime - lastUseTime >= 30000) { // 30 seconds cooldown
            hunter.compassTarget = runner.location
            playProximitySound(hunter, runner)
            compassCooldowns[hunter] = currentTime
        }
    }

    private fun playProximitySound(hunter: Player, runner: Player) {
        val distance = hunter.location.distance(runner.location)
        val pitch = when {
            distance < 50 -> 2.0f
            distance < 100 -> 1.5f
            distance < 200 -> 1.0f
            else -> 0.5f
        }
        hunter.playSound(hunter.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch)
    }

    fun giveCompassToHunters() {
        teamManager.getHunters().forEach { hunter ->
            hunter.inventory.addItem(ItemStack(Material.COMPASS))
        }
    }
}