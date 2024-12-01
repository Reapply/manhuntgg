package co.sakurastudios.manhuntgg.config

import org.bukkit.plugin.java.JavaPlugin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class GameConfig(
    val lobbyDuration: Duration = 60.seconds,
    val preparationDuration: Duration = 30.seconds,
    val borderShrinkTime: Duration = 20.minutes,  // Renamed for consistency
    val endingDuration: Duration = 10.seconds,
    val initialBorderSize: Double = 4000.0,
    val finalBorderSize: Double = 100.0,
    val minPlayers: Int = 2,
    val maxPlayers: Int = 10,
    val hunterSpawnRadius: Double = 500.0,
    val hunterRespawnRadius: Double = 100.0
) {
    companion object {
        fun load(plugin: JavaPlugin): GameConfig {
            plugin.saveDefaultConfig()
            val config = plugin.config

            return GameConfig(
                lobbyDuration = config.getLong("durations.lobby", 60).seconds,
                preparationDuration = config.getLong("durations.preparation", 30).seconds,
                borderShrinkTime = config.getLong("durations.border-shrink", 1200).seconds,
                endingDuration = config.getLong("durations.ending", 10).seconds,
                initialBorderSize = config.getDouble("border.initial-size", 4000.0),
                finalBorderSize = config.getDouble("border.final-size", 100.0),
                minPlayers = config.getInt("players.min-players", 2),
                maxPlayers = config.getInt("players.max-players", 10),
                hunterSpawnRadius = config.getDouble("players.hunter-spawn-radius", 500.0),
                hunterRespawnRadius = config.getDouble("players.hunter-respawn-radius", 100.0)
            )
        }
    }
}