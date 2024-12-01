package co.sakurastudios.manhuntgg.managers

import co.sakurastudios.manhuntgg.world.VoidGenerator
import org.bukkit.*
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.time.Duration
import java.util.Random
import java.util.logging.Level

/**
 * Manages world operations for the Manhunt game.
 * Handles world creation, border management, and cleanup operations.
 */
object WorldManager {
    // Cache game worlds to prevent unnecessary world loading
    private val worldCache = ConcurrentHashMap<String, World>()
    private var borderTask: BukkitTask? = null

    // Constants for world configuration
    private const val LOBBY_WORLD_NAME = "manhunt_lobby"
    private const val GAME_WORLD_NAME = "manhunt_world"
    private const val SPAWN_PLATFORM_RADIUS = 5
    private const val SPAWN_PLATFORM_HEIGHT = 64
    private const val SAFE_LOCATION_ATTEMPTS = 50
    private const val LOBBY_BORDER_SIZE = 100.0

    /**
     * Creates and sets up the lobby world with a void generator
     */
    fun createLobbyWorld(plugin: JavaPlugin): World? {
        return try {
            if (worldCache.containsKey(LOBBY_WORLD_NAME)) {
                return worldCache[LOBBY_WORLD_NAME]
            }

            WorldCreator(LOBBY_WORLD_NAME).apply {
                type(WorldType.FLAT)
                generateStructures(false)
                environment(World.Environment.NORMAL)
                generator(VoidGenerator())
            }.createWorld()?.apply {
                // Configure lobby world settings
                setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)
                setGameRule(GameRule.DO_WEATHER_CYCLE, false)
                setGameRule(GameRule.DO_MOB_SPAWNING, false)
                setGameRule(GameRule.FALL_DAMAGE, false)
                time = 6000L
                difficulty = Difficulty.PEACEFUL

                // Get reference to the world before setting border
                val world = this

                // Set up lobby border
                worldBorder.apply {
                    center = Location(world, 0.0, 64.0, 0.0)
                    size = LOBBY_BORDER_SIZE * 2 // Diameter is twice the radius
                    warningDistance = 5
                    warningTime = 3
                }

                createSpawnPlatform()
                worldCache[LOBBY_WORLD_NAME] = this
            }
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to create lobby world", e)
            null
        }
    }


    /**
     * Creates and configures the main game world
     */
    fun createGameWorld(plugin: JavaPlugin): World {
        return try {
            if (worldCache.containsKey(GAME_WORLD_NAME)) {
                return worldCache[GAME_WORLD_NAME]!!
            }

            WorldCreator(GAME_WORLD_NAME).apply {
                environment(World.Environment.NORMAL)
                type(WorldType.NORMAL)
            }.createWorld()?.apply {
                // Configure game world settings
                difficulty = Difficulty.NORMAL
                setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true)
                setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false)
                setGameRule(GameRule.DO_INSOMNIA, false)
                worldBorder.center = spawnLocation

                worldCache[GAME_WORLD_NAME] = this
            } ?: throw IllegalStateException("Failed to create game world")
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to create game world", e)
            throw e
        }
    }

    /**
     * Configures world border settings for a given world
     */
    fun setupWorldBorder(world: World, initialSize: Double = 4000.0) {
        world.worldBorder.apply {
            center = world.spawnLocation
            size = initialSize
            warningDistance = 25
            warningTime = 15
            damageBuffer = 5.0
            damageAmount = 1.0
        }
    }

    /**
     * Initiates the world border shrinking process
     */
    fun startBorderShrinking(
        plugin: JavaPlugin,
        initialSize: Double,
        finalSize: Double,
        duration: Duration,
        progressCallback: (Double) -> Unit
    ) {
        stopBorderShrinking()
        val startTime = System.currentTimeMillis()

        borderTask = plugin.server.scheduler.runTaskTimer(
            plugin,
            Runnable {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toDouble() / duration.inWholeMilliseconds).coerceIn(0.0, 1.0)

                val size = initialSize - ((initialSize - finalSize) * progress)
                getGameWorld()?.worldBorder?.size = size

                progressCallback(progress)

                if (progress >= 1.0) {
                    stopBorderShrinking()
                }
            },
            0L,
            20L
        )
    }

    /**
     * Stops the border shrinking process
     */
    fun stopBorderShrinking() {
        borderTask?.cancel()
        borderTask = null
    }

    /**
     * Checks if a location is safe for player teleportation
     */
    fun isLocationSafe(location: Location): Boolean {
        val world = location.world ?: return false
        val border = world.worldBorder

        return border.isInside(location) &&
                !location.block.type.isSolid &&
                !location.clone().add(0.0, 1.0, 0.0).block.type.isSolid &&
                location.clone().subtract(0.0, 1.0, 0.0).block.type.isSolid
    }

    /**
     * Finds a safe spawn location within a given radius
     */
    fun findSafeSpawnLocation(center: Location, radius: Double): Location {
        val world = center.world ?: return center
        val border = world.worldBorder
        val random = Random()

        for (attempt in 1..SAFE_LOCATION_ATTEMPTS) {
            val angle = random.nextDouble() * 2 * Math.PI
            val distance = random.nextDouble() * radius
            val x = center.x + Math.cos(angle) * distance
            val z = center.z + Math.sin(angle) * distance

            val y = world.getHighestBlockYAt(x.toInt(), z.toInt())
            val spawnLoc = Location(world, x, y + 1.0, z)

            if (isLocationSafe(spawnLoc) && border.isInside(spawnLoc)) {
                return spawnLoc
            }
        }

        // Fallback to a safe height at the center location
        return findSafeHeight(center)
    }

    /**
     * Finds a safe height at a given location
     */
    private fun findSafeHeight(location: Location): Location {
        val world = location.world ?: return location
        val x = location.blockX
        val z = location.blockZ
        val y = world.getHighestBlockYAt(x, z)

        return Location(world, x + 0.5, y + 1.0, z + 0.5)
    }

    /**
     * Creates the spawn platform in the lobby world
     */
    private fun World.createSpawnPlatform() {
        for (x in -SPAWN_PLATFORM_RADIUS..SPAWN_PLATFORM_RADIUS) {
            for (z in -SPAWN_PLATFORM_RADIUS..SPAWN_PLATFORM_RADIUS) {
                getBlockAt(x, SPAWN_PLATFORM_HEIGHT, z).type = Material.BARRIER
            }
        }

        // Create glass border around platform
        val radius = SPAWN_PLATFORM_RADIUS + 1
        for (y in SPAWN_PLATFORM_HEIGHT + 1..SPAWN_PLATFORM_HEIGHT + 3) {
            for (x in -radius..radius) {
                getBlockAt(x, y, -radius).type = Material.GLASS
                getBlockAt(x, y, radius).type = Material.GLASS
            }
            for (z in -radius..radius) {
                getBlockAt(-radius, y, z).type = Material.GLASS
                getBlockAt(radius, y, z).type = Material.GLASS
            }
        }

        spawnLocation = Location(this, 0.0, SPAWN_PLATFORM_HEIGHT + 1.0, 0.0)
    }

    /**
     * Cleans up the lobby world
     */
    fun cleanupLobbyWorld() {
        cleanupWorld(LOBBY_WORLD_NAME)
    }

    /**
     * Cleans up the game world
     */
    fun cleanupGameWorld() {
        cleanupWorld(GAME_WORLD_NAME)
    }

    /**
     * Generic world cleanup implementation
     */
    private fun cleanupWorld(worldName: String) {
        Bukkit.getWorld(worldName)?.let { world ->
            // Teleport all players to the main world
            Bukkit.getOnlinePlayers().forEach { player ->
                if (player.world == world) {
                    player.teleport(Bukkit.getWorlds()[0].spawnLocation)
                }
            }

            // Unload and delete the world
            Bukkit.unloadWorld(world, false)
            world.worldFolder.toPath().let { path ->
                if (path.exists()) {
                    try {
                        path.listDirectoryEntries().forEach { it.deleteIfExists() }
                        path.deleteIfExists()
                    } catch (e: Exception) {
                        Bukkit.getLogger().log(Level.WARNING, "Failed to delete world files for $worldName", e)
                    }
                }
            }

            // Clear from cache
            worldCache.remove(worldName)
        }
    }

    /**
     * Gets the current game world
     */
    fun getGameWorld(): World? = worldCache[GAME_WORLD_NAME] ?: Bukkit.getWorld(GAME_WORLD_NAME)

    /**
     * Cleanup all managed worlds
     */
    fun shutdown() {
        stopBorderShrinking()
        cleanupLobbyWorld()
        cleanupGameWorld()
        worldCache.clear()
    }
}