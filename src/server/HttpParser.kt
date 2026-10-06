package server

import java.io.ByteArrayOutputStream
import java.io.InputStream

class BadRequestException(message: String) : Exception(message)

object HttpParser {

    private const val MAX_LINE = 8192
    private const val MAX_HEADERS = 100
    private const val MAX_BODY = 1_000_000

    fun parse(input: InputStream): HttpRequest? {
        val requestLine = readLine(input) ?: return null

        val parts = requestLine.split(" ")
        if (parts.size != 3 || !parts[2].startsWith("HTTP/") || !parts[1].startsWith("/")) {
            throw BadRequestException("Неверная стартовая строка запроса")
        }
        val method = parts[0].uppercase()
        val path = parts[1].substringBefore('?').let { if (it.length > 1) it.trimEnd('/') else it }

        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = readLine(input) ?: throw BadRequestException("Соединение оборвано в заголовках")
            if (line.isEmpty()) break
            if (headers.size >= MAX_HEADERS) throw BadRequestException("Слишком много заголовков")
            val colon = line.indexOf(':')
            if (colon <= 0) throw BadRequestException("Неверный заголовок: $line")
            headers[line.substring(0, colon).trim().lowercase()] = line.substring(colon + 1).trim()
        }

        val length = headers["content-length"]?.let {
            it.toIntOrNull() ?: throw BadRequestException("Content-Length должен быть числом")
        } ?: 0
        if (length < 0 || length > MAX_BODY) throw BadRequestException("Недопустимый размер тела запроса")

        val body = if (length > 0) String(readBody(input, length), Charsets.UTF_8) else ""
        return HttpRequest(method, path, headers, body)
    }

    private fun readLine(input: InputStream): String? {
        val buffer = ByteArrayOutputStream()
        while (true) {
            val b = input.read()
            if (b == -1) {
                if (buffer.size() == 0) return null
                throw BadRequestException("Соединение оборвано посреди строки")
            }
            if (b == '\n'.code) break
            if (b != '\r'.code) buffer.write(b)
            if (buffer.size() > MAX_LINE) throw BadRequestException("Слишком длинная строка в запросе")
        }
        return String(buffer.toByteArray(), Charsets.ISO_8859_1)
    }

    private fun readBody(input: InputStream, length: Int): ByteArray {
        val data = ByteArray(length)
        var read = 0
        while (read < length) {
            val n = input.read(data, read, length - read)
            if (n == -1) throw BadRequestException("Тело запроса неполное: соединение оборвано")
            read += n
        }
        return data
    }
}