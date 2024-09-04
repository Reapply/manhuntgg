package co.sakurastudios.manhuntgg.managers

import co.sakurastudios.manhuntgg.utils.MsgUtils
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.plugin.java.JavaPlugin

class VictoryManager(private val plugin: JavaPlugin, private val teamManager: TeamManager) : Listener {

    private val victoryBlock = Material.DIAMOND_BLOCK

    fun initialize() {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler
    fun onCraftItem(event: CraftItemEvent) {
        val player = event.whoClicked as? Player ?: return

        if (teamManager.isRunner(player) && event.recipe.result.type == victoryBlock) {
            MsgUtils.broadcastSuccess("${player.name} (Runner) has crafted the victory block! The Runner wins!")


        }
    }
}