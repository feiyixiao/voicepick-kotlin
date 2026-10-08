package voicepick

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

data class Part(val id: String, val name: String, var stock: Int)

sealed interface PickResult {
    data class Ok(val partId: String, val picked: Int, val remaining: Int) : PickResult
    data class Failed(val reason: String) : PickResult
}

/** What the view model needs from the backend. A fake can replace it in tests. */
interface PartsClient {
    fun pick(partId: String, qty: Int): PickResult
}

/** Small in-memory backend (JDK HTTP server, no libraries) that stands in for the real backend API. */
class PartsServer(port: Int = 0) {
    private val parts = mutableMapOf(
        "A17" to Part("A17", "Winkel 40x20", 10),
        "B4" to Part("B4", "Sensorhalter", 3),
        "C9" to Part("C9", "Abdeckblech", 0),
    )
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", port), 0)
    val port: Int get() = server.address.port

    init {
        server.createContext("/picks") { ex -> handlePick(ex) }
        server.createContext("/parts/") { ex -> handlePart(ex) }
    }

    fun start() = server.start()
    fun stop() = server.stop(0)

    private fun handlePick(ex: HttpExchange) {
        if (ex.requestMethod != "POST") return reply(ex, 405, """{"error":"method not allowed"}""")
        val body = ex.requestBody.bufferedReader().readText()
        val id = Regex(""""partId"\s*:\s*"([^"]+)"""").find(body)?.groupValues?.get(1)
        val qty = Regex(""""qty"\s*:\s*(\d+)""").find(body)?.groupValues?.get(1)?.toInt()
        if (id == null || qty == null || qty <= 0) return reply(ex, 400, """{"error":"bad request"}""")
        val part = parts[id] ?: return reply(ex, 404, """{"error":"unknown part"}""")
        if (part.stock < qty) return reply(ex, 409, """{"error":"insufficient stock","remaining":${part.stock}}""")
        part.stock -= qty
        reply(ex, 200, """{"partId":"$id","picked":$qty,"remaining":${part.stock}}""")
    }

    private fun handlePart(ex: HttpExchange) {
        val part = parts[ex.requestURI.path.substringAfterLast('/')]
            ?: return reply(ex, 404, """{"error":"unknown part"}""")
        reply(ex, 200, """{"partId":"${part.id}","name":"${part.name}","stock":${part.stock}}""")
    }

    private fun reply(ex: HttpExchange, status: Int, json: String) {
        val bytes = json.toByteArray()
        ex.responseHeaders.add("Content-Type", "application/json")
        ex.sendResponseHeaders(status, bytes.size.toLong())
        ex.responseBody.use { it.write(bytes) }
    }
}

/** HTTP client for [PartsServer] using the JDK client. */
class HttpPartsClient(private val baseUrl: String) : PartsClient {
    private val http = HttpClient.newHttpClient()

    override fun pick(partId: String, qty: Int): PickResult {
        val request = HttpRequest.newBuilder(URI.create("$baseUrl/picks"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("""{"partId":"$partId","qty":$qty}"""))
            .build()
        val response = try {
            http.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: java.io.IOException) {
            return PickResult.Failed("Backend nicht erreichbar")
        }
        val body = response.body()
        return when (response.statusCode()) {
            200 -> PickResult.Ok(
                partId,
                Regex(""""picked"\s*:\s*(\d+)""").find(body)!!.groupValues[1].toInt(),
                Regex(""""remaining"\s*:\s*(\d+)""").find(body)!!.groupValues[1].toInt(),
            )
            404 -> PickResult.Failed("Teil $partId ist unbekannt")
            409 -> PickResult.Failed(
                "Nicht genug Bestand von $partId (noch ${Regex(""""remaining"\s*:\s*(\d+)""").find(body)?.groupValues?.get(1) ?: "?"})")
            else -> PickResult.Failed("Fehler ${response.statusCode()}")
        }
    }
}
