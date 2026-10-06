package client

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

private val http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()
private const val BASE = "http://localhost:8080"

private fun send(method: String, path: String, body: String? = null, token: String? = null): String {
    val publisher = if (body != null) HttpRequest.BodyPublishers.ofString(body)
    else HttpRequest.BodyPublishers.noBody()

    val builder = HttpRequest.newBuilder(URI.create(BASE + path)).method(method, publisher)
    if (body != null) builder.header("Content-Type", "application/json")
    if (token != null) builder.header("Authorization", "Bearer $token")

    val response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    println(">>> $method $path")
    println("<<< ${response.statusCode()}")
    println(response.body())
    println()
    return response.body()
}

private fun tokenFrom(json: String): String =
    Regex("\"token\":\\s*\"(\\w+)\"").find(json)?.groupValues?.get(1) ?: ""

fun main() {
    println("--- Регистрация ---")
    send("POST", "/register", """{"username": "alex", "password": "12345"}""")
    send("POST", "/register", """{"username": "alex", "password": "12345"}""")
    send("POST", "/register", """{"username": "bob", "password": "qwerty"}""")

    println("--- Вход ---")
    send("POST", "/login", """{"username": "alex", "password": "wrong"}""")
    val alexToken = tokenFrom(send("POST", "/login", """{"username": "alex", "password": "12345"}"""))
    val bobToken = tokenFrom(send("POST", "/login", """{"username": "bob", "password": "qwerty"}"""))

    println("--- Сообщения ---")
    send("POST", "/messages", """{"text": "Hello"}""")
    send("GET", "/messages", token = "bad_token")
    send("POST", "/messages", """{"text": "Hello world"}""", alexToken)
    send("POST", "/messages", """{"text": "   "}""", alexToken)
    send("POST", "/messages", """{"text": "Привет, мир!"}""", bobToken)
    send("GET", "/messages", token = alexToken)

    println("--- Удаление ---")
    send("DELETE", "/messages/1", token = bobToken)
    send("DELETE", "/messages/1", token = alexToken)
    send("DELETE", "/messages/1", token = alexToken)
    send("DELETE", "/messages/abc", token = alexToken)

    println("--- Ошибки ---")
    send("GET", "/unknown")
    send("POST", "/login", "это не json")
}