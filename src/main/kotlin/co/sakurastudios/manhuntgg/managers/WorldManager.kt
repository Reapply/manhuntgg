package co.sakurastudios.manhuntgg.managers

import co.sakurastudios.manhuntgg.utils.MsgUtils
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class WorldManager(private val plugin: JavaPlugin) {

    var manhuntWorld: World? = null
        private set
    private var worldBorderTaskId: Int = -1
    private var isFinalBorderReached = false

    fun preloadManhuntWorld(): Boolean {
        try {
            if (manhuntWorld == null) {
                manhuntWorld = Bukkit.createWorld(
                    WorldCreator("manhunt_world")
                        .environment(World.Environment.NORMAL)
                        .type(WorldType.NORMAL)
                ) ?: throw IllegalStateException("Failed to create or load manhunt_world")
            }
            return true
        } catch (e: Exception) {
            plugin.logger.severe("Error preloading Manhunt world: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    fun setupManhuntWorld(): Boolean {
        try {
            if (manhuntWorld == null) {
                manhuntWorld = Bukkit.createWorld(
                    WorldCreator("manhunt_world")
                        .environment(World.Environment.NORMAL)
                        .type(WorldType.NORMAL)
                ) ?: throw IllegalStateException("Failed to create or load manhunt_world")
            }

            setupWorldBorder()
            return true
        } catch (e: Exception) {
            plugin.logger.severe("Error setting up Manhunt world: ${e.message}")
            e.printStackTrace()
            cleanupWorld()
            return false
        }
    }

    private fun setupWorldBorder() {
        val border = manhuntWorld?.worldBorder ?: return
        border.center = manhuntWorld!!.spawnLocation
        border.size = 4000.0
        border.setSize(100.0, 1200) // Shrinks to 100 blocks over 20 minutes (1200 seconds)
        MsgUtils.broadcastInfo("World border will shrink over the next 20 minutes!")

        worldBorderTaskId = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            val size = border.size
            if (size > 100) {
                MsgUtils.broadcastInfo("World border is now at ${String.format("%.2f", size)} blocks! Stay alert!")
            } else if (!isFinalBorderReached) {
                isFinalBorderReached = true
                MsgUtils.broadcastInfo("Final border size reached! Hunters will not respawn if killed!")
            }
        }, 0, 20 * 60).taskId // Alert every minute
    }

    fun resetPlayer(player: Player, location: Location) {
        player.teleport(location)
        player.gameMode = GameMode.SURVIVAL
        player.health = 20.0
        player.foodLevel = 20
        player.inventory.clear()
        MsgUtils.sendInfo(player, "You have been teleported to the Manhunt world.")
    }

    fun cleanupManhuntWorld() {
        manhuntWorld?.let { world ->
            Bukkit.getOnlinePlayers().forEach { player ->
                if (player.world == world) {
                    player.teleport(Bukkit.getWorlds()[0].spawnLocation)
                }
            }
            Bukkit.unloadWorld(world, false)
            deleteWorld(world.name)
        }
        manhuntWorld = null
        cancelWorldBorderTask()
    }

    private fun cancelWorldBorderTask() {
        if (worldBorderTaskId != -1) {
            Bukkit.getScheduler().cancelTask(worldBorderTaskId)
            worldBorderTaskId = -1
        }
    }

    private fun deleteWorld(worldName: String) {
        val worldFolder = Bukkit.getWorldContainer().resolve(worldName)
        if (worldFolder.exists()) {
            worldFolder.deleteRecursively()
        }
    }

    private fun cleanupWorld() {
        cleanupManhuntWorld()
        cancelWorldBorderTask()
    }

    fun findSafeLocation(baseLocation: Location): Location {
        return (0..10)
            .map { y -> baseLocation.clone().add(0.0, y.toDouble(), 0.0) }
            .firstOrNull { isSafeLocation(it) }
            ?: baseLocation
    }

    private fun isSafeLocation(location: Location): Boolean {
        val blockBelow = location.block.getRelative(0, -1, 0)
        val blockAt = location.block
        val blockAbove = location.block.getRelative(0, 1, 0)
        return !blockBelow.isEmpty && !blockBelow.isLiquid && blockAt.isEmpty && blockAbove.isEmpty
    }

    fun handlePlayerDeath(player: Player, teamManager: TeamManager) {
        if (isFinalBorderReached && teamManager.isHunter(player)) {
            player.gameMode = GameMode.SPECTATOR
            MsgUtils.broadcastError("${player.name} (Hunter) has been eliminated!")
            checkForRunnerVictory(teamManager)
        } else if (teamManager.isHunter(player)) {
            val runner = teamManager.getRunner()
            val safeLocation = findSafeLocation(runner.location.add((Math.random() - 0.5) * 100, 0.0, (Math.random() - 0.5) * 100))
            resetPlayer(player, safeLocation)
            MsgUtils.broadcastError("${player.name} (Hunter) has died and respawned")
        } else if (teamManager.isRunner(player)) {
            player.gameMode = GameMode.SPECTATOR
            MsgUtils.broadcastError("${player.name} (Runner) has died! The Hunters win!")
            // Trigger game end
        }
    }

    private fun checkForRunnerVictory(teamManager: TeamManager) {
        if (teamManager.getHunters().all { it.gameMode == GameMode.SPECTATOR }) {
            MsgUtils.broadcastSuccess("All Hunters have been eliminated! The Runner wins!")
            // Trigger game end
        }
    }
}