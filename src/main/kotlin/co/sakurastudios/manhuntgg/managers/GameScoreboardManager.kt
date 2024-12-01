package co.sakurastudios.manhuntgg.managers

import co.sakurastudios.manhuntgg.engine.GameEngine
import co.sakurastudios.manhuntgg.state.GameState
import me.neznamy.tab.api.TabAPI
import me.neznamy.tab.api.scoreboard.Scoreboard
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask
import java.time.Duration

class GameScoreboardManager(private val gameEngine: GameEngine) {
    private var updateTask: BukkitTask? = null
    private var startTime: Long = 0
    private var scoreboard: Scoreboard? = null

    companion object {
        private const val SEPARATOR_LINE = "§7&m-------------------------"
        private const val SERVER_IP = "§7play.example.com"
    }

    fun initialize() {
        val tabAPI = TabAPI.getInstance()
        if (tabAPI == null) {
            gameEngine.getPlugin().logger.warning("TabAPI instance is not available. Ensure the TAB plugin is installed.")
            return
        }

        val scoreboardManager = tabAPI.scoreboardManager
        if (scoreboardManager == null) {
            gameEngine.getPlugin().logger.warning("TabAPI ScoreboardManager is not available. Check TAB configuration.")
            return
        }

        scoreboard = scoreboardManager.createScoreboard(
            "manhunt",
            "§b§lManhunt",
            mutableListOf(SEPARATOR_LINE, "§7Status: §cIdle", SERVER_IP)
        )

        if (scoreboard == null) {
            gameEngine.getPlugin().logger.warning("Failed to create a TabAPI scoreboard.")
        }

        startUpdating()
    }


    fun startUpdating() {
        stopUpdating()
        startTime = System.currentTimeMillis()

        updateTask = gameEngine.getPlugin().server.scheduler.runTaskTimer(
            gameEngine.getPlugin(),
            Runnable { updateAllScoreboards() },
            0L,
            20L
        )
    }

    fun stopUpdating() {
        updateTask?.cancel()
        updateTask = null
    }

    private fun updateAllScoreboards() {
        gameEngine.getPlugin().server.onlinePlayers.forEach { player ->
            updatePlayerScoreboard(player.uniqueId)
        }
    }

    fun updatePlayerScoreboard(playerId: java.util.UUID) {
        val lines = when (gameEngine.getCurrentState()) {
            GameState.IDLE -> createIdleLines()
            GameState.LOBBY -> createLobbyLines()
            GameState.PREPARATION -> createPrepLines()
            GameState.RUNNING -> createGameLines(playerId)
            GameState.ENDING -> createEndLines()
        }

        scoreboard?.apply {
            val existingLines = getLines()
            for (i in existingLines.indices.reversed()) {
                removeLine(i)
            }
            lines.forEach { addLine(it) }
        }
    }

    private fun createIdleLines(): List<String> = listOf(
        SEPARATOR_LINE,
        "§7Status: §cIdle",
        "§7Players: §a${Bukkit.getOnlinePlayers().size}",
        "",
        "§b§lUse /manhunt to start!",
        "",
        SERVER_IP,
        SEPARATOR_LINE
    )

    private fun createLobbyLines(): List<String> = listOf(
        SEPARATOR_LINE,
        "§7Status: §eWaiting...",
        "§7Players: §a${Bukkit.getOnlinePlayers().size}",
        "",
        "§b§lPrepare for the game!",
        "",
        SERVER_IP,
        SEPARATOR_LINE
    )

    private fun createPrepLines(): List<String> = listOf(
        SEPARATOR_LINE,
        "§e§lPreparation Phase",
        "§7Get ready!",
        "",
        SERVER_IP,
        SEPARATOR_LINE
    )

    private fun createGameLines(playerId: java.util.UUID): List<String> {
        val player = Bukkit.getPlayer(playerId)
        return if (player != null) {
            listOf(
                SEPARATOR_LINE,
                "§7Game is running...",
                "",
                SERVER_IP,
                SEPARATOR_LINE
            )
        } else {
            listOf(SEPARATOR_LINE, "§cPlayer not found!", SERVER_IP, SEPARATOR_LINE)
        }
    }

    private fun createEndLines(): List<String> = listOf(
        SEPARATOR_LINE,
        "§cGame Over",
        "",
        "§7Thanks for playing!",
        "",
        SERVER_IP,
        SEPARATOR_LINE
    )

    fun cleanup() {
        stopUpdating()
        scoreboard?.unregister()
    }
}
