package voicepick

import java.io.File
import java.util.concurrent.TimeUnit

/** Source of utterances. On the glasses this would be on-device speech recognition. */
fun interface Recognizer {
    /** Returns the next recognised utterance, or null when the input has ended. */
    fun listen(): String?
}

/** Plays back a fixed list of utterances: used for the demo and for tests. */
class ScriptedRecognizer(utterances: List<String>) : Recognizer {
    private val queue = ArrayDeque(utterances)
    override fun listen(): String? = queue.removeFirstOrNull()
}

/** Reads utterances typed on the keyboard (one per line). */
class ConsoleRecognizer : Recognizer {
    override fun listen(): String? = readlnOrNull()
}

/**
 * Local speech recognition by calling an external command that prints the transcript of an
 * audio file, e.g. a script around a local Whisper model. Nothing leaves the machine.
 */
class CommandRecognizer(private val command: List<String>, private val audioFiles: List<File>) : Recognizer {
    private val queue = ArrayDeque(audioFiles)
    override fun listen(): String? {
        val file = queue.removeFirstOrNull() ?: return null
        val process = ProcessBuilder(command + file.absolutePath).redirectErrorStream(false).start()
        val text = process.inputStream.bufferedReader().readText().trim()
        if (!process.waitFor(120, TimeUnit.SECONDS) || process.exitValue() != 0) return ""
        return text
    }
}
