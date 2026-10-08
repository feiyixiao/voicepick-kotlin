package voicepick

/**
 * Turns a free utterance (German or English) into a [Command].
 * It replaces a fixed command list ("Befehl 1", "Befehl 2") with keyword and number matching,
 * so "Bitte nimm drei Stück von A 17" and "take 3 of a-17" give the same result.
 * Rule-based on purpose: it is predictable and testable offline. An LLM could replace [parse] later.
 */
class IntentParser {
    private val partIdRegex = Regex("""\b([a-z]{1,3})[\s-]?(\d{1,4})\b""")
    private val pickWords = setOf("nimm", "nehme", "hol", "hole", "bring", "bringe", "entnimm", "brauche",
        "pick", "take", "need", "get")
    private val confirmWords = setOf("bestätigt", "bestätige", "bestaetigt", "ok", "okay", "fertig",
        "erledigt", "passt", "done", "confirm", "confirmed")
    private val nextWords = setOf("weiter", "nächste", "nächstes", "naechste", "next")
    private val repeatWords = setOf("wiederhole", "nochmal", "repeat", "again", "wiederholen")
    private val numberWords = mapOf(
        "null" to 0, "zero" to 0, "ein" to 1, "eine" to 1, "einen" to 1, "eins" to 1, "one" to 1,
        "zwei" to 2, "two" to 2, "drei" to 3, "three" to 3, "vier" to 4, "four" to 4,
        "fünf" to 5, "fuenf" to 5, "five" to 5, "sechs" to 6, "six" to 6, "sieben" to 7, "seven" to 7,
        "acht" to 8, "eight" to 8, "neun" to 9, "nine" to 9, "zehn" to 10, "ten" to 10,
    )

    fun parse(utterance: String): Command {
        val text = utterance.lowercase().replace(Regex("""[.,!?;:]"""), " ").trim()
        if (text.isEmpty()) return Command.Unknown(utterance)
        val words = text.split(Regex("""\s+"""))
        val idMatch = partIdRegex.find(text)
        return when {
            words.any { it in pickWords } && idMatch != null -> {
                val partId = idMatch.groupValues[1].uppercase() + idMatch.groupValues[2]
                val rest = text.removeRange(idMatch.range).split(Regex("""\s+"""))
                Command.Pick(partId, quantityIn(rest) ?: 1)
            }
            words.any { it in confirmWords } -> Command.Confirm
            words.any { it in nextWords } -> Command.Next
            words.any { it in repeatWords } -> Command.Repeat
            else -> Command.Unknown(utterance)
        }
    }

    private fun quantityIn(words: List<String>): Int? =
        words.firstNotNullOfOrNull { w -> w.toIntOrNull() ?: numberWords[w] }
}
