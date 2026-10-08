package voicepick

/** Dependency-free test runner: `main` runs every test and exits with 1 if any fails. */
private var failures = 0

private fun <T> expect(name: String, actual: T, expected: T) {
    if (actual == expected) println("PASS  $name") else {
        failures++
        println("FAIL  $name\n      expected: $expected\n      actual:   $actual")
    }
}

private class FakeClient(private val stock: MutableMap<String, Int>) : PartsClient {
    override fun pick(partId: String, qty: Int): PickResult {
        val have = stock[partId] ?: return PickResult.Failed("Teil $partId ist unbekannt")
        if (have < qty) return PickResult.Failed("Nicht genug Bestand von $partId (noch $have)")
        stock[partId] = have - qty
        return PickResult.Ok(partId, qty, have - qty)
    }
}

fun main() {
    val parser = IntentParser()
    // parser: free speech instead of fixed commands
    expect("de pick with number word", parser.parse("Bitte nimm drei Stück von A 17"), Command.Pick("A17", 3))
    expect("en pick with hyphenated id", parser.parse("take 2 of b-4"), Command.Pick("B4", 2))
    expect("pick without quantity defaults to 1", parser.parse("hol mir B4"), Command.Pick("B4", 1))
    expect("digit quantity", parser.parse("Bring 12 von C9."), Command.Pick("C9", 12))
    expect("id digits are not the quantity", parser.parse("pick a17"), Command.Pick("A17", 1))
    expect("confirm de", parser.parse("Das passt, fertig"), Command.Confirm)
    expect("confirm en", parser.parse("okay"), Command.Confirm)
    expect("next", parser.parse("weiter"), Command.Next)
    expect("repeat", parser.parse("kannst du das wiederholen"), Command.Repeat)
    expect("unknown", parser.parse("wie ist das Wetter"), Command.Unknown("wie ist das Wetter"))
    expect("empty is unknown", parser.parse("   "), Command.Unknown("   "))
    expect("pick word without part id is unknown", parser.parse("nimm drei"), Command.Unknown("nimm drei"))

    // view model with a fake backend
    val vm = PickViewModel(FakeClient(mutableMapOf("A17" to 10, "B4" to 3)))
    vm.onUtterance("nimm drei von A17")
    expect("successful pick updates history", vm.state.history, listOf(PickRecord("A17", 3, 7)))
    expect("successful pick waits for confirmation", vm.state.pending, Command.Pick("A17", 3))
    vm.onUtterance("bestätigt")
    expect("confirm clears pending", vm.state.pending, null)
    vm.onUtterance("hol fünf B4")
    expect("insufficient stock shows an error", vm.state.error, "Nicht genug Bestand von B4 (noch 3)")
    expect("failed pick adds no history", vm.state.history.size, 1)
    vm.onUtterance("blabla")
    expect("unknown utterance asks again", vm.state.error, "Nicht verstanden: \"blabla\"")
    vm.onUtterance("ok")
    expect("confirm without pick is refused", vm.state.error, "Nichts zu bestätigen.")

    // end to end with the real HTTP backend
    val server = PartsServer().also { it.start() }
    val real = PickViewModel(HttpPartsClient("http://127.0.0.1:${server.port}"))
    real.onUtterance("nimm zwei von A 17")
    expect("http pick ok", real.state.history, listOf(PickRecord("A17", 2, 8)))
    real.onUtterance("nimm zwei von Z 99")
    expect("http unknown part", real.state.error, "Teil Z99 ist unbekannt")
    real.onUtterance("bring C 9")
    expect("http out of stock", real.state.error, "Nicht genug Bestand von C9 (noch 0)")
    server.stop()
    val offline = PickViewModel(HttpPartsClient("http://127.0.0.1:1"))
    offline.onUtterance("nimm eins von A17")
    expect("backend down is reported, not thrown", offline.state.error, "Backend nicht erreichbar")

    println(if (failures == 0) "\nAll tests passed." else "\n$failures test(s) failed.")
    if (failures > 0) kotlin.system.exitProcess(1)
}
