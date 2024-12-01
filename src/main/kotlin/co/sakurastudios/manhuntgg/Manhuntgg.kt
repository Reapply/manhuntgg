package co.sakurastudios.manhuntgg

import co.sakurastudios.manhuntgg.commands.ManhuntCommand
import co.sakurastudios.manhuntgg.commands.ShoutCommand
import co.sakurastudios.manhuntgg.engine.GameEngine
import co.sakurastudios.manhuntgg.listeners.*
import co.sakurastudios.manhuntgg.managers.GameScoreboardManager
import org.bukkit.plugin.java.JavaPlugin

class Manhuntgg : JavaPlugin() {
    private var gameEngine: GameEngine? = null
    private var scoreboardManager: GameScoreboardManager? = null

    override fun onEnable() {
        try {
            setupEngine()
            setupScoreboardManager()
            registerCommands()
            registerListeners()
            logger.info("ManhuntGG enabled successfully!")
        } catch (e: Exception) {
            logger.severe("Failed to enable ManhuntGG: ${e.message}")
            e.printStackTrace()
            server.pluginManager.disablePlugin(this)
        }
    }

    private fun setupEngine() {
        gameEngine = GameEngine(this)
    }

    private fun setupScoreboardManager() {
        gameEngine?.let { engine ->
            scoreboardManager = GameScoreboardManager(engine).apply {
                initialize()
            }
        }
    }

    private fun registerCommands() {
        gameEngine?.let { engine ->
            getCommand("manhunt")?.setExecutor(ManhuntCommand(engine))
            getCommand("shout")?.setExecutor(ShoutCommand(engine))
        }
    }

    private fun registerListeners() {
        gameEngine?.let { engine ->
            val listeners = listOf(
                GameEventListener(engine),
                PlayerListener(engine),
                WorldListener(engine),
                CombatListener(engine),
                scoreboardManager?.let { ScoreboardListener(it) }
            ).filterNotNull()

            listeners.forEach { listener ->
                server.pluginManager.registerEvents(listener, this)
            }
        }
    }

    override fun onDisable() {
        try {
            scoreboardManager?.cleanup()
            gameEngine?.shutdown()
            logger.info("ManhuntGG disabled successfully!")
        } catch (e: Exception) {
            logger.severe("Error during ManhuntGG shutdown: ${e.message}")
            e.printStackTrace()
        }
    }
}
