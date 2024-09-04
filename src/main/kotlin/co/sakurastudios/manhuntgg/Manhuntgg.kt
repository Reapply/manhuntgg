package co.sakurastudios.manhuntgg

import co.sakurastudios.manhuntgg.commands.ManhuntCommand
import co.sakurastudios.manhuntgg.commands.ShoutCommand
import co.sakurastudios.manhuntgg.listeners.ChatListener
import co.sakurastudios.manhuntgg.listeners.TeamListeners
import co.sakurastudios.manhuntgg.managers.*
import co.sakurastudios.manhuntgg.services.GameService
import org.bukkit.plugin.java.JavaPlugin

class Manhuntgg : JavaPlugin() {

    private lateinit var gameService: GameService

    override fun onEnable() {
        try {
            // Initialize all managers
            val worldManager = WorldManager(this)
            val teamManager = TeamManager(this, worldManager)
            val lobbyManager = LobbyManager(this)
            val compassManager = CompassManager(this, teamManager)
            val victoryManager = VictoryManager(this, teamManager)
            val gameManager = GameManager(this, lobbyManager, worldManager, teamManager, compassManager, victoryManager)

            // Set up dependencies
            lobbyManager.setGameManager(gameManager)
            gameService = GameService(gameManager)

            // Register commands
            getCommand("manhunt")?.setExecutor(ManhuntCommand(gameService))
            getCommand("shout")?.setExecutor(ShoutCommand(teamManager, gameManager))


            // Register listeners
            TeamListeners(teamManager, gameManager, this).registerListeners()
            server.pluginManager.registerEvents(victoryManager, this)
            server.pluginManager.registerEvents(ChatListener(teamManager, gameManager), this)

            // Register custom death handler
            server.pluginManager.registerEvents(object : org.bukkit.event.Listener {
                @org.bukkit.event.EventHandler
                fun onPlayerDeath(event: org.bukkit.event.entity.PlayerDeathEvent) {
                    if (gameManager.isGameActive()) {
                        worldManager.handlePlayerDeath(event.entity, teamManager)
                    }
                }
            }, this)

            logger.info("Manhuntgg plugin has been enabled, version ${description.version}")

        } catch (e: Exception) {
            logger.severe("An error occurred during plugin startup: ${e.message}")
            isEnabled = false
        }
    }

    override fun onDisable() {
        gameService.stopGameOnShutdown()
        logger.info("Manhuntgg plugin has been disabled.")
    }
}