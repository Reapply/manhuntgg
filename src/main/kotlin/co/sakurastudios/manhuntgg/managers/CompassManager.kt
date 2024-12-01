package co.sakurastudios.manhuntgg.managers

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.CompassMeta
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import kotlin.math.atan2
import kotlin.math.roundToInt

object CompassManager {
    private var trackingTask: BukkitTask? = null
    private val compassCooldowns = mutableMapOf<Player, Long>()
    private const val COOLDOWN_MILLIS = 5000L

    fun startTracking(plugin: JavaPlugin) {
        stopTracking()

        trackingTask = plugin.server.scheduler.runTaskTimer(
            plugin,
            Runnable {
                TeamManager.getHunters()
                    .filter { it.inventory.itemInMainHand.type == Material.COMPASS }
                    .forEach { updateCompass(it) }
            },
            0L,
            5L
        )
    }

    private fun updateCompass(hunter: Player) {
        val runner = TeamManager.getRunner() ?: return
        val currentTime = System.currentTimeMillis()
        val lastUseTime = compassCooldowns[hunter] ?: 0L

        if (currentTime - lastUseTime >= COOLDOWN_MILLIS) {
            val compass = hunter.inventory.itemInMainHand
            val meta = compass.itemMeta as? CompassMeta ?: return

            meta.lodestone = runner.location
            meta.isLodestoneTracked = false

            val direction = getDirectionArrow(hunter.location, runner.location)
            val distance = hunter.location.distance(runner.location).roundToInt()

            meta.lore = listOf(
                "§6→ $direction",
                "§b$distance blocks"
            )

            compass.itemMeta = meta
            playTrackerSound(hunter, distance)
            compassCooldowns[hunter] = currentTime
        }
    }

    private fun getDirectionArrow(from: Location, to: Location): String {
        val dx = to.x - from.x
        val dz = to.z - from.z
        val angle = Math.toDegrees(atan2(dz, dx))
        val yaw = (from.yaw + 360) % 360

        return when (val relativeAngle = (angle - yaw + 360) % 360) {
            in 0.0..22.5, in 337.5..360.0 -> "→"
            in 22.5..67.5 -> "↗"
            in 67.5..112.5 -> "↑"
            in 112.5..157.5 -> "↖"
            in 157.5..202.5 -> "←"
            in 202.5..247.5 -> "↙"
            in 247.5..292.5 -> "↓"
            else -> "↘"
        }
    }

    private fun playTrackerSound(player: Player, distance: Int) {
        val pitch = when {
            distance < 50 -> 2.0f
            distance < 100 -> 1.5f
            distance < 200 -> 1.0f
            else -> 0.5f
        }
        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch)
    }

    fun stopTracking() {
        trackingTask?.cancel()
        trackingTask = null
    }
}