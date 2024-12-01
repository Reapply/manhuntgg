package co.sakurastudios.manhuntgg.state.handlers

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player
import java.time.Duration

fun Player.showTitle(title: String, subtitle: String, titleColor: String, subtitleColor: String) {
    val adventureTitle = Title.title(
        Component.text(title, TextColor.fromHexString(titleColor)),
        Component.text(subtitle, TextColor.fromHexString(subtitleColor)),
        Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000))
    )
    this.showTitle(adventureTitle)
}
