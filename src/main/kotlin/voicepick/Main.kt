package voicepick

/** Demo: starts the local backend, plays a short dialogue and prints what the glasses would show. */
fun main(args: Array<String>) {
    val server = PartsServer().also { it.start() }
    val viewModel = PickViewModel(HttpPartsClient("http://127.0.0.1:${server.port}"))
    viewModel.observe { s ->
        println("  [Display] ${s.prompt}" + (s.error?.let { "  !! $it" } ?: ""))
    }
    val recognizer: Recognizer = when {
        args.firstOrNull() == "--console" -> ConsoleRecognizer()
        args.firstOrNull() == "--audio" -> CommandRecognizer(
            listOf("python3", "scripts/transcribe.py"), args.drop(1).map { java.io.File(it) })
        else -> ScriptedRecognizer(listOf(
        "Bitte nimm drei Stück von A 17",
        "bestätigt",
        "hol mir fünf von B4",              // only 3 in stock
        "take two of b-4",
        "ok",
        "wie ist das Wetter",               // not understood
        "bring C 9",                        // out of stock
        ))
    }
    while (true) {
        val utterance = recognizer.listen() ?: break
        println("Worker: $utterance")
        viewModel.onUtterance(utterance)
    }
    server.stop()
}
