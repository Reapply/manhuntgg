package co.sakurastudios.manhuntgg.managers

import co.sakurastudios.manhuntgg.utils.Messages
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scoreboard.Team
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet

object TeamManager {
    private val hunters = ConcurrentSkipListSet<UUID>()
    private var runner: UUID? = null
    private val eliminatedHunters = ConcurrentHashMap.newKeySet<UUID>()
    private var scoreboard = Bukkit.getScoreboardManager().newScoreboard
    private lateinit var hunterTeam: Team
    private lateinit var runnerTeam: Team

    fun initialize() {
        scoreboard = Bukkit.getScoreboardManager().newScoreboard.apply {
            registerNewTeam("Hunters").apply {
                color = ChatColor.RED
                prefix = "${ChatColor.RED}[Hunter] "
                setAllowFriendlyFire(false)
                setCanSeeFriendlyInvisibles(true)
                hunterTeam = this
            }

            registerNewTeam("Runner").apply {
                color = ChatColor.AQUA
                prefix = "${ChatColor.AQUA}[Runner] "
                setAllowFriendlyFire(false)
                runnerTeam = this
            }
        }
    }

    /**
     * Assigns players to teams at game start
     */
    fun assignTeams(players: Collection<Player>): Boolean {
        if (players.size < 2) {
            return false
        }

        reset()

        // Random runner selection
        val selectedRunner = players.random()
        setRunner(selectedRunner)

        // Assign remaining players as hunters
        players.filter { it != selectedRunner }.forEach { addHunter(it) }

        return true
    }

    private fun setRunner(player: Player) {
        runner = player.uniqueId
        player.apply {
            gameMode = GameMode.SURVIVAL
            inventory.clear()
            health = 20.0
            foodLevel = 20
            runnerTeam.addEntry(name)
            scoreboard = this@TeamManager.scoreboard
            isGlowing = false

            // Initial slowness effect
            addPotionEffect(PotionEffect(
                PotionEffectType.SLOWNESS,
                20 * 60, // 1 minute
                1,
                false,
                false
            ))
        }

        Messages.sendSuccess(player, "You are the Runner! Survive and defeat the Dragon!")
    }

    private fun addHunter(player: Player) {
        hunters.add(player.uniqueId)
        player.apply {
            gameMode = GameMode.SURVIVAL
            inventory.clear()
            health = 20.0
            foodLevel = 20
            hunterTeam.addEntry(name)
            scoreboard = this@TeamManager.scoreboard
            isGlowing = true
        }

        Messages.sendSuccess(player, "You are a Hunter! Track down the Runner!")
    }

    fun reapplyRunnerEffects(player: Player) {
        if (player.uniqueId != runner) return

        player.apply {
            gameMode = GameMode.SURVIVAL
            isGlowing = false
            runnerTeam.addEntry(name)
            scoreboard = this@TeamManager.scoreboard
        }
    }

    fun reapplyHunterEffects(player: Player) {
        if (!hunters.contains(player.uniqueId)) return

        player.apply {
            if (eliminatedHunters.contains(uniqueId)) {
                gameMode = GameMode.SPECTATOR
            } else {
                gameMode = GameMode.SURVIVAL
                isGlowing = true
            }
            hunterTeam.addEntry(name)
            scoreboard = this@TeamManager.scoreboard
        }
    }

    fun eliminateHunter(player: Player) {
        if (!hunters.contains(player.uniqueId)) return

        eliminatedHunters.add(player.uniqueId)
        player.gameMode = GameMode.SPECTATOR
        Messages.broadcastManhunt("${player.name} has been eliminated!")
    }

    fun reset() {
        hunters.clear()
        runner = null
        eliminatedHunters.clear()

        hunterTeam.entries.forEach { hunterTeam.removeEntry(it) }
        runnerTeam.entries.forEach { runnerTeam.removeEntry(it) }

        Bukkit.getOnlinePlayers().forEach { player ->
            resetPlayerState(player)
        }
    }

    fun resetPlayerState(player: Player) {
        player.apply {
            gameMode = GameMode.SURVIVAL
            inventory.clear()
            health = 20.0
            foodLevel = 20
            isGlowing = false
            activePotionEffects.forEach { removePotionEffect(it.type) }
            scoreboard = Bukkit.getScoreboardManager().newScoreboard
        }
    }

    // Utility methods
    fun isRunner(player: Player) = player.uniqueId == runner
    fun isHunter(player: Player) = hunters.contains(player.uniqueId)
    fun isParticipating(player: Player) = isRunner(player) || isHunter(player)
    fun getRunner(): Player? = runner?.let { Bukkit.getPlayer(it) }
    fun getHunters(): List<Player> = hunters.mapNotNull { Bukkit.getPlayer(it) }
    fun getActiveHunters(): List<Player> = hunters
        .filter { !eliminatedHunters.contains(it) }
        .mapNotNull { Bukkit.getPlayer(it) }
}