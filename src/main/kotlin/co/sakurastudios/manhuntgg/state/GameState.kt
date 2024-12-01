package co.sakurastudios.manhuntgg.state

enum class GameState {
    IDLE,
    LOBBY,
    PREPARATION,
    RUNNING,
    ENDING;

    fun canTransitionTo(nextState: GameState): Boolean = when (this) {
        IDLE -> nextState == LOBBY
        LOBBY -> nextState == PREPARATION || nextState == IDLE
        PREPARATION -> nextState == RUNNING || nextState == IDLE
        RUNNING -> nextState == ENDING || nextState == IDLE
        ENDING -> nextState == IDLE
    }
}