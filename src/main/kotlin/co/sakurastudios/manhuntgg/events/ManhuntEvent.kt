package co.sakurastudios.manhuntgg.events

import co.sakurastudios.manhuntgg.state.GameState
import co.sakurastudios.manhuntgg.state.GameTeam
import co.sakurastudios.manhuntgg.state.VictoryReason
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

sealed class ManhuntEvent : Event() {
    companion object {
        private val HANDLERS = HandlerList()
        @JvmStatic
        fun getHandlerList() = HANDLERS
    }
    override fun getHandlers() = HANDLERS
}

class GameStateChangeEvent(
    val oldState: GameState,
    val newState: GameState
) : ManhuntEvent()

class GameVictoryEvent(
    val winner: GameTeam,
    val reason: VictoryReason
) : ManhuntEvent()