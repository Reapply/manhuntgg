package co.sakurastudios.manhuntgg.state

interface GameStateHandler {
    fun onEnter()
    fun onExit()
    fun update()
    fun cleanup()
}