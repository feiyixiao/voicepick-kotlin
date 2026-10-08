package voicepick

/** What the worker wants to do, after free speech has been parsed. */
sealed interface Command {
    data class Pick(val partId: String, val qty: Int) : Command
    data object Confirm : Command
    data object Next : Command
    data object Repeat : Command
    data class Unknown(val text: String) : Command
}
