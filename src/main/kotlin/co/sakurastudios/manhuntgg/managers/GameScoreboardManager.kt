package co.sakurastudios.manhuntgg.managers

import co.sakurastudios.manhuntgg.engine.GameEngine
import co.sakurastudios.manhuntgg.state.GameState
import me.neznamy.tab.api.TabAPI
import me.neznamy.tab.api.scoreboard.Scoreboard
import org.bukkit.scheduler.BukkitTask
import java.time.Duration

class GameScoreboardManager(private val gameEngine: GameEngine) {
    private var updateTask: BukkitTask? = null
    private var startTime: Long = 0
    private var scoreboard: Scoreboard? = null

    companion object {
        // Hex color constants
        private const val TITLE = "&#00FFFF" // Cyan
        private const val SUBTITLE = "&#636B73" // Cool gray
        private const val LABEL = "&#8A8D91" // Light gray
        private const val VALUE = "&#FFFFFF" // Pure white
        private const val HIGHLIGHT = "&#47F5FF" // Bright cyan
        private const val RUNNER = "&#00E5FF" // Bright blue
        private const val HUNTER = "&#FF4655" // Vibrant red
        private const val ALERT = "&#FFD700" // Gold
        private const val HEALTH = "&#FF5555" // Soft red
        private const val SEPARATOR = "&#2C2F33" // Dark gray

        // Style constants
        private const val SEPARATOR_LINE = "$SEPARATOR&m                          "
        private const val SERVER_IP = "${SUBTITLE}na.stray.gg"
    }

    fun initialize() {
        val tabAPI = TabAPI.getInstance()
        val scoreboardManager = tabAPI.getScoreboardManager()
            ?: throw IllegalStateException("ScoreboardManager is null")

        scoreboard = scoreboardManager.createScoreboard(
            "manhunt",
            "$TITLE&lManhunt",
            mutableListOf(SEPARATOR_LINE, "$LABEL&lStatus: $ALERT&lIdle", SERVER_IP)
        ) ?: throw IllegalStateException("Failed to create scoreboard")

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
        "$LABEL&lStatus: $ALERT&lIdle",
        "$LABEL&lPlayers: $VALUE${gameEngine.getPlugin().server.onlinePlayers.size}",
        "",
        "$HIGHLIGHT&lUse /manhunt to start",
        "",
        SERVER_IP,
        SEPARATOR_LINE
    )

    private fun createLobbyLines(): List<String> {
        val onlinePlayers = gameEngine.getPlugin().server.onlinePlayers.size
        val minPlayers = gameEngine.getConfig().minPlayers
        val maxPlayers = gameEngine.getConfig().maxPlayers

        return listOf(
            SEPARATOR_LINE,
            "$LABEL&lStatus: $HIGHLIGHT&lWaiting...",
            "$LABEL&lPlayers: $VALUE$onlinePlayers/$maxPlayers",
            "$LABEL&lRequired: $ALERT$minPlayers",
            "",
            "$LABEL&lBorder: $VALUE${gameEngine.getConfig().initialBorderSize.toInt()}",
            "",
            SERVER_IP,
            SEPARATOR_LINE
        )
    }

    private fun createPrepLines(): List<String> {
        val runner = TeamManager.getRunner()
        val prepDuration = gameEngine.getConfig().preparationDuration
        val timeLeftSeconds = (prepDuration.inWholeSeconds * 1000L - (System.currentTimeMillis() - startTime)) / 1000L

        return listOf(
            SEPARATOR_LINE,
            "$HIGHLIGHT&lPreparation Phase",
            "$LABEL&lTime Left: $VALUE${formatTime(timeLeftSeconds.toInt())}",
            "",
            "$LABEL&lRunner: $RUNNER${runner?.name ?: "None"}",
            "",
            "$ALERT&lGet Ready!",
            "",
            SERVER_IP,
            SEPARATOR_LINE
        )
    }

    private fun createGameLines(playerId: java.util.UUID): List<String> {
        val player = gameEngine.getPlugin().server.getPlayer(playerId) ?: return emptyList()
        val runner = TeamManager.getRunner()
        val activeHunters = TeamManager.getActiveHunters()
        val elapsedSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()
        val borderSize = gameEngine.getPlugin().server.getWorld("manhunt_world")?.worldBorder?.size?.toInt() ?: 0

        val lines = mutableListOf(
            SEPARATOR_LINE,
            "$LABEL&lTime: $HIGHLIGHT${formatTime(elapsedSeconds)}",
            "$LABEL&lBorder: $HIGHLIGHT$borderSize",
            ""
        )

        if (TeamManager.isRunner(player)) {
            lines.addAll(listOf(
                "$LABEL&lRole: $RUNNER&lRunner",
                "$LABEL&lHealth: $HEALTH${player.health.toInt()}❤"
            ))
        } else {
            lines.addAll(listOf(
                "$LABEL&lRole: $HUNTER&lHunter",
                runner?.let { "$LABEL&lRunner HP: $HEALTH${it.health.toInt()}❤" } ?: ""
            ))
        }

        lines.addAll(listOf(
            "",
            "$LABEL&lHunters: $HUNTER${activeHunters.size}"
        ))

        activeHunters.take(3).forEach { hunter ->
            lines.add("$SUBTITLE» $VALUE${hunter.name}")
        }

        if (activeHunters.size > 3) {
            lines.add("${SUBTITLE}and ${activeHunters.size - 3} more...")
        }

        lines.addAll(listOf(
            "",
            SERVER_IP,
            SEPARATOR_LINE
        ))

        return lines
    }

    private fun createEndLines(): List<String> {
        val elapsedSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()

        return listOf(
            SEPARATOR_LINE,
            "$ALERT&lGame Over",
            "",
            "$LABEL&lFinal Time: $HIGHLIGHT${formatTime(elapsedSeconds)}",
            "",
            "$VALUE&lThanks for playing!",
            "",
            SERVER_IP,
            SEPARATOR_LINE
        )
    }

    private fun formatTime(seconds: Int): String {
        val duration = Duration.ofSeconds(seconds.toLong())
        val hours = duration.toHours()
        val minutes = duration.toMinutesPart()
        val secs = duration.toSecondsPart()

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }

    fun cleanup() {
        stopUpdating()
        scoreboard?.unregister()
    }
}
