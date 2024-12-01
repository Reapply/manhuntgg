package co.sakurastudios.manhuntgg.state.handlers

import co.sakurastudios.manhuntgg.config.GameConfig
import co.sakurastudios.manhuntgg.state.GameStateHandler
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.atomic.AtomicBoolean

abstract class BaseStateHandler(
    protected val plugin: JavaPlugin,
    protected val config: GameConfig
) : GameStateHandler {
    protected var isActive = AtomicBoolean(false)

    override fun cleanup() {
        isActive.set(false)
    }
}
