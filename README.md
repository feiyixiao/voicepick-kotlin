# voicepick-kotlin

A small hands-free picking assistant in Kotlin: a worker says what part they need in free speech,
the app parses it, books the pick against a backend REST API and shows the result on a display.
It is a minimal closed loop (speech, intent, backend, display), written to practise the pieces of an
Android data-glasses app: MVVM, a REST backend, free speech instead of fixed commands, and local
(on-device) speech recognition.

```
audio ──▶ Recognizer ──▶ IntentParser ──▶ PickViewModel ──▶ PartsClient ──HTTP──▶ PartsServer
(local Whisper)  text       Command          UiState (display)
```

| File | Role |
|---|---|
| `Command.kt` | Sealed class of what the worker can mean: `Pick`, `Confirm`, `Next`, `Repeat`, `Unknown` |
| `IntentParser.kt` | German/English free speech to `Command` (keywords and number words, no fixed command list) |
| `Recognizer.kt` | Source of utterances: scripted, console, or `CommandRecognizer` (local Whisper through a script) |
| `PartsApi.kt` | In-memory backend on the JDK HTTP server (`POST /picks`, `GET /parts/{id}`) and an HTTP client |
| `PickViewModel.kt` | MVVM view model: utterance in, immutable `UiState` out, observers get notified |
| `Main.kt` | Demo: scripted dialogue, `--console` for typing, `--audio files...` for spoken input |
| `src/test/.../Tests.kt` | 23 checks: parser, view model with a fake backend, view model against the real HTTP backend |

## Run

```bash
brew install openjdk kotlin      # once
./build.sh test                  # 23 checks, no network, no libraries
./build.sh run                   # scripted dialogue
./build.sh run --console         # type utterances yourself
./build.sh audio                 # speech made with macOS `say`, recognised locally by Whisper base
```

Audio mode needs `python3` with `openai-whisper` and `ffmpeg` (the `base` model is read from the local
cache, nothing is sent to a cloud service).

## What happened in the audio run

Three German utterances were generated with `say` and recognised locally:

| Spoken | Recognised | Result |
|---|---|---|
| Bitte nimm drei Stück von A 17 | Bitte nimm drei Stück von A17. | Pick booked, rest 7 |
| Bestätigt | bestätigt. | Confirmed |
| Hol mir zwei von B 4 | **Hohlmeer 2 von B4.** | Not understood, the app asks again |

The third one is a real recognition error ("Hol mir" became "Hohlmeer"). The parser does not guess; the
view model asks for a repeat. Next step would be to measure how often this happens (error analysis on
more recordings) and, for the glasses, compare on-device models for the work environment.

## Limits

- Plain Kotlin on the JVM (Gradle is not set up), **not an Android app**. `PickViewModel`, `IntentParser`
  and `PartsClient` have no Android dependency and could move into one; the console display and the
  `Recognizer` would be replaced by an activity and the glasses' speech input.
- The parser is rule-based and covers a handful of verbs and number words up to ten. Free speech beyond
  that, and the "AI instead of fixed commands" part, would need an LLM or a trained intent model behind
  `IntentParser.parse`.
- The backend is a stand-in with three parts; JSON is built and read by hand to avoid libraries.
- No security work (certificate pinning, persistent login) and no real device testing.
