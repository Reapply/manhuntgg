package co.sakurastudios.manhuntgg.managers

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.CompassMeta
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import kotlin.math.atan2
import kotlin.math.roundToInt

class CompassManager(private val plugin: JavaPlugin, private val teamManager: TeamManager) {

    private val compassCooldowns = mutableMapOf<Player, Long>()
    private var compassTask: BukkitTask? = null
    private val cooldownDuration = 5000L // 5 seconds cooldown

    fun startCompassTracking() {
        compassTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            teamManager.getHunters()
                .filter { it.inventory.itemInMainHand.type == Material.COMPASS }
                .forEach { updateCompass(it) }
        }, 0L, 5L) // Check every 1/4 second for more responsive updates
    }

    fun stopCompassTracking() {
        compassTask?.cancel()
    }

    private fun updateCompass(hunter: Player) {
        val runner = teamManager.getRunner()
        val currentTime = System.currentTimeMillis()
        val lastUseTime = compassCooldowns[hunter] ?: 0L

        if (currentTime - lastUseTime >= cooldownDuration) {
            val direction = getDirection(hunter, runner)
            val distance = hunter.location.distance(runner.location).roundToInt()
            val compassMeta = (hunter.inventory.itemInMainHand.itemMeta as CompassMeta)

            compassMeta.lodestone = runner.location
            compassMeta.isLodestoneTracked = false

            val lore = listOf(
                Component.text("Direction: $direction").color(NamedTextColor.GOLD),
                Component.text("Distance: $distance blocks").color(NamedTextColor.AQUA)
            )
            compassMeta.lore(lore)

            hunter.inventory.itemInMainHand.itemMeta = compassMeta
            hunter.sendActionBar(Component.text("Runner: $direction, $distance blocks").color(NamedTextColor.GREEN))

            playProximitySound(hunter, distance)
            compassCooldowns[hunter] = currentTime
        }
    }

    private fun getDirection(hunter: Player, runner: Player): String {
        val hunterLoc = hunter.location
        val runnerLoc = runner.location
        val dx = runnerLoc.x - hunterLoc.x
        val dz = runnerLoc.z - hunterLoc.z
        val angle = Math.toDegrees(atan2(dz, dx))
        val yaw = (hunterLoc.yaw + 360) % 360

        val relativeAngle = (angle - yaw + 360) % 360
        return when {
            relativeAngle < 22.5 || relativeAngle >= 337.5 -> "→"
            relativeAngle < 67.5 -> "↗"
            relativeAngle < 112.5 -> "↑"
            relativeAngle < 157.5 -> "↖"
            relativeAngle < 202.5 -> "←"
            relativeAngle < 247.5 -> "↙"
            relativeAngle < 292.5 -> "↓"
            else -> "↘"
        }
    }

    private fun playProximitySound(hunter: Player, distance: Int) {
        val pitch = when {
            distance < 50 -> 2.0f
            distance < 100 -> 1.5f
            distance < 200 -> 1.0f
            else -> 0.5f
        }
        hunter.playSound(hunter.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch)
    }

    fun giveCompassToHunters() {
        val compass = ItemStack(Material.COMPASS)
        val meta = compass.itemMeta as CompassMeta
        meta.displayName(Component.text("Runner Tracker").color(NamedTextColor.RED))
        compass.itemMeta = meta
        teamManager.getHunters().forEach { it.inventory.addItem(compass) }
    }
}