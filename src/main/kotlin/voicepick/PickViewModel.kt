package voicepick

/** One finished pick, shown in the history. */
data class PickRecord(val partId: String, val qty: Int, val remaining: Int)

/** Everything the glasses display needs to show. The UI only renders this. */
data class UiState(
    val prompt: String = "Sagen Sie, welches Teil Sie brauchen.",
    val pending: Command.Pick? = null,
    val history: List<PickRecord> = emptyList(),
    val error: String? = null,
)

/**
 * MVVM view model: takes an utterance, parses it, calls the backend and publishes a new [UiState].
 * It knows nothing about Android, so the same class can be used by an Android activity later.
 */
class PickViewModel(
    private val client: PartsClient,
    private val parser: IntentParser = IntentParser(),
) {
    var state: UiState = UiState()
        private set
    private val observers = mutableListOf<(UiState) -> Unit>()

    fun observe(observer: (UiState) -> Unit) {
        observers += observer
        observer(state)
    }

    fun onUtterance(utterance: String) {
        state = when (val command = parser.parse(utterance)) {
            is Command.Pick -> pick(command)
            Command.Confirm -> confirm()
            Command.Next -> state.copy(
                pending = null, error = null, prompt = "Nächstes Teil bitte.")
            Command.Repeat -> state.copy(error = null, prompt = state.pending?.let {
                "Wiederholung: ${it.qty} × ${it.partId}" } ?: state.prompt)
            is Command.Unknown -> state.copy(
                error = "Nicht verstanden: \"${command.text}\"",
                prompt = "Bitte nochmal, z. B. \"Nimm drei Stück von A 17\".")
        }
        observers.forEach { it(state) }
    }

    private fun pick(command: Command.Pick): UiState = when (val result = client.pick(command.partId, command.qty)) {
        is PickResult.Ok -> state.copy(
            pending = command, error = null,
            history = state.history + PickRecord(result.partId, result.picked, result.remaining),
            prompt = "Entnommen: ${result.picked} × ${result.partId}, Rest ${result.remaining}. Bitte bestätigen.")
        is PickResult.Failed -> state.copy(pending = null, error = result.reason,
            prompt = "Entnahme nicht möglich.")
    }

    private fun confirm(): UiState = state.pending?.let {
        state.copy(pending = null, error = null, prompt = "Bestätigt: ${it.qty} × ${it.partId}. Weiter?")
    } ?: state.copy(error = "Nichts zu bestätigen.", prompt = "Erst eine Entnahme sagen.")
}
