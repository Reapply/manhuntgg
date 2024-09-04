package co.sakurastudios.manhuntgg.managers

import co.sakurastudios.manhuntgg.utils.MsgUtils
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team

class LobbyManager(private val plugin: JavaPlugin) : Listener {
    private lateinit var gameManager: GameManager
    var lobbyWorld: World? = null
    private lateinit var scoreboard: Scoreboard
    private lateinit var objective: Objective
    private lateinit var countdownTeam: Team
    private var countdownTask: Int = -1
    private var countdownSeconds: Int = 60

    fun setGameManager(gameManager: GameManager) {
        this.gameManager = gameManager
    }

    fun setupLobby(): Boolean {
        lobbyWorld = retrieveLobbyWorld() ?: return false
        teleportPlayersToLobby()
        setupLobbyScoreboard()
        startCountdown()
        registerEvents()
        return true
    }

    private fun retrieveLobbyWorld(): World? =
        Bukkit.getWorld("manhunt_lobby") ?: createLobbyWorld()

    private fun createLobbyWorld(): World? =
        WorldCreator("manhunt_lobby").apply {
            type(WorldType.FLAT)
            generateStructures(false)
            generatorSettings("{\"structures\": {\"structures\": {}}, \"layers\": [{\"block\": \"stone\", \"height\": 1}, {\"block\": \"air\", \"height\": 256}], \"biome\":\"plains\"}")
        }.createWorld()?.apply {
            setSpawnLocation(0, 64, 0)
            createSafePlatform()
            setupWorldBorder()
            setWorldProperties()
        } ?: run {
            MsgUtils.broadcastError("Failed to set up the lobby world. Please check the console for errors.")
            null
        }

    private fun World.createSafePlatform() {
        for (x in -5..5) for (z in -5..5) {
            getBlockAt(x, 63, z).type = Material.BARRIER
        }
    }

    private fun World.setupWorldBorder() {
        worldBorder.apply {
            setCenter(0.0, 0.0)
            size = 50.0
        }
    }

    private fun World.setWorldProperties() {
        time = 6000 // Midday
        setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)
        setGameRule(GameRule.DO_WEATHER_CYCLE, false)
        setStorm(false)
    }

    private fun teleportPlayersToLobby() {
        Bukkit.getOnlinePlayers().forEach { teleportToLobby(it) }
    }

    private fun teleportToLobby(player: Player) {
        lobbyWorld?.let { world ->
            player.teleport(world.spawnLocation.add(0.0, 1.0, 0.0))
            player.setupForLobby()
            MsgUtils.sendInfo(player, "Welcome to the Manhunt lobby! The game will start soon.")
        }
    }

    private fun Player.setupForLobby() {
        gameMode = GameMode.ADVENTURE
        inventory.clear()
        health = 20.0
        foodLevel = 20
        saturation = 20f
        exp = 0f
        level = 0
        allowFlight = true
        isFlying = true
    }

    private fun setupLobbyScoreboard() {
        scoreboard = Bukkit.getScoreboardManager().newScoreboard
        objective = scoreboard.registerNewObjective("lobby", "dummy", Component.text("Manhunt Lobby").color(NamedTextColor.GOLD))
        objective.displaySlot = DisplaySlot.SIDEBAR

        countdownTeam = scoreboard.registerNewTeam("countdown") ?: scoreboard.getTeam("countdown")!!
        countdownTeam.addEntry(NamedTextColor.AQUA.toString())

        updateScoreboard()
    }

    private fun updateScoreboard() {
        objective.getScore("Game starts in:").score = 3
        objective.getScore(NamedTextColor.AQUA.toString()).score = 2
        objective.getScore("Prepare for the Hunt!").score = 1

        countdownTeam.prefix(Component.text(formatTime(countdownSeconds)))

        Bukkit.getOnlinePlayers().forEach { it.scoreboard = scoreboard }
    }

    private fun formatTime(seconds: Int): String =
        String.format("%02d:%02d", seconds / 60, seconds % 60)

    private fun startCountdown() {
        gameManager.worldManager.preloadManhuntWorld()

        countdownTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, {
            countdownSeconds--
            updateScoreboard()

            when (countdownSeconds) {
                in listOf(30, 15, 10, 5, 4, 3, 2, 1) -> MsgUtils.broadcastInfo("Game starts in $countdownSeconds seconds!")
                0 -> {
                    Bukkit.getScheduler().cancelTask(countdownTask)
                    // Ensure the game manager proceeds to start the game
                }
            }
        }, 0L, 20L)
    }

    fun cleanupLobby() {
        cancelCountdown()
        sendPlayersToMainWorld()
        unloadLobbyWorld()
        resetCountdown()
    }

    private fun cancelCountdown() {
        Bukkit.getScheduler().cancelTask(countdownTask)
    }

    private fun sendPlayersToMainWorld() {
        val mainWorld = Bukkit.getWorlds()[0]
        Bukkit.getOnlinePlayers().forEach { player ->
            player.teleport(mainWorld.spawnLocation)
            player.resetToSurvival()
            MsgUtils.sendInfo(player, "The game has ended. You've been returned to the main world.")
        }
    }

    private fun Player.resetToSurvival() {
        gameMode = GameMode.SURVIVAL
        allowFlight = false
        isFlying = false
        scoreboard = Bukkit.getScoreboardManager().newScoreboard
    }

    private fun unloadLobbyWorld() {
        lobbyWorld?.let { world ->
            Bukkit.unloadWorld(world, false)
            val worldFolder = Bukkit.getWorldContainer().resolve(world.name)
            worldFolder.deleteRecursively()
        }
        lobbyWorld = null
    }

    private fun resetCountdown() {
        countdownSeconds = 60
    }

    private fun registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        if (event.entity is Player && event.entity.world == lobbyWorld) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerFallIntoVoidInLobby(event: PlayerMoveEvent) {
        if (event.player.world == lobbyWorld && event.to.y < 0) {
            teleportToLobby(event.player)
            event.player.playSound(event.player.location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f)
        }
    }
}