package co.sakurastudios.manhuntgg.managers

import co.sakurastudios.manhuntgg.utils.MsgUtils
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scoreboard.Scoreboard
import kotlin.random.Random

class TeamManager(private val plugin: JavaPlugin, private val worldManager: WorldManager) {

    private val hunters = mutableListOf<Player>()
    private var runner: Player? = null
    private lateinit var scoreboard: Scoreboard

    enum class TeamType(val displayName: String, val color: NamedTextColor) {
        HUNTER("Hunter", NamedTextColor.RED),
        RUNNER("Runner", NamedTextColor.BLUE)
    }

    init {
        initializeScoreboard()
    }

    private fun initializeScoreboard() {
        scoreboard = Bukkit.getScoreboardManager().newScoreboard.apply {
            TeamType.entries.forEach { teamType ->
                registerNewTeam(teamType.displayName).apply {
                    prefix(Component.text("[${teamType.displayName}] ").color(teamType.color))
                }
            }
        }
    }

    fun setupTeams(): Boolean {
        val onlinePlayers = Bukkit.getOnlinePlayers().shuffled()
        if (onlinePlayers.size < 2) {
            MsgUtils.broadcastError("Not enough players to start the game!")
            return false
        }

        resetTeams()
        assignPlayers(onlinePlayers)
        spawnHuntersBehindRunner()
        applyRunnerSlowness()
        return true
    }

    fun resetTeams() {
        hunters.clear()
        runner = null
        resetPlayerColors()
    }

    private fun resetPlayerColors() {
        Bukkit.getOnlinePlayers().forEach { player ->
            player.displayName(Component.text(player.name))
            player.playerListName(Component.text(player.name))
            player.scoreboard = Bukkit.getScoreboardManager().newScoreboard
            player.isGlowing = false
        }
    }

    private fun assignPlayers(players: List<Player>) {
        players.forEachIndexed { index, player ->
            if (index == 0) {
                assignRunner(player)
            } else {
                assignHunter(player)
            }
        }
    }

    private fun assignRunner(player: Player) {
        runner = player
        addToTeam(player, TeamType.RUNNER)
        MsgUtils.sendInfo(player, "You are the Runner! Avoid the Hunters!")
    }

    private fun assignHunter(player: Player) {
        hunters.add(player)
        addToTeam(player, TeamType.HUNTER)
        addGlow(player)
        MsgUtils.sendInfo(player, "You are a Hunter! Catch the Runner!")
    }

    private fun spawnHuntersBehindRunner() {
        runner?.let { runner ->
            val world = worldManager.manhuntWorld ?: return
            val spawnLocation = world.spawnLocation

            worldManager.resetPlayer(runner, spawnLocation)

            hunters.forEach { hunter ->
                val angle = Random.nextDouble() * 2 * Math.PI
                val distance = Random.nextDouble() * 500
                val x = spawnLocation.x + distance * Math.cos(angle)
                val z = spawnLocation.z + distance * Math.sin(angle)
                val safeLocation = worldManager.findSafeLocation(Location(world, x, world.getHighestBlockYAt(x.toInt(), z.toInt()).toDouble(), z))
                worldManager.resetPlayer(hunter, safeLocation)
                MsgUtils.sendSuccess(hunter, "You have been safely teleported within a 500 block radius of the Runner!")
            }
        }
    }

    private fun applyRunnerSlowness() {
        runner?.let { runner ->
            runner.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 20 * 60, 1)) // 60 seconds of Slowness II
            MsgUtils.sendInfo(runner, "You have been slowed for the first minute of the game!")
        }
    }

    private fun addToTeam(player: Player, teamType: TeamType) {
        scoreboard.getTeam(teamType.displayName)?.addEntry(player.name)
        player.apply {
            displayName(Component.text(name).color(teamType.color))
            playerListName(Component.text(name).color(teamType.color))
            scoreboard = this@TeamManager.scoreboard
        }
    }

    private fun addGlow(player: Player) {
        player.isGlowing = true
    }

    fun isHunter(player: Player): Boolean = player in hunters

    fun isRunner(player: Player): Boolean = player == runner

    fun areTeammates(player1: Player, player2: Player): Boolean =
        (isHunter(player1) && isHunter(player2)) || (isRunner(player1) && isRunner(player2))

    fun reassignPlayerToTeam(player: Player) {
        when {
            isHunter(player) -> {
                addToTeam(player, TeamType.HUNTER)
                MsgUtils.sendInfo(player, "You have been re-assigned to the Hunters team.")
            }
            isRunner(player) -> {
                addToTeam(player, TeamType.RUNNER)
                MsgUtils.sendInfo(player, "You have been re-assigned as the Runner.")
            }
        }
    }

    fun removeHunter(player: Player) {
        hunters.remove(player)
    }

    fun getRunner(): Player = runner!!
    fun getHunters(): List<Player> = hunters
}