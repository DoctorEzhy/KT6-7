package server

import util.Json

data class HttpRequest(
    val method: String,
    val path: String,
    val headers: Map<String, String>,
    val body: String
)

data class HttpResponse(val status: Int, val body: String) {

    fun toBytes(): ByteArray {
        val bodyBytes = body.toByteArray(Charsets.UTF_8)
        val head = "HTTP/1.1 $status ${reason(status)}\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Content-Length: ${bodyBytes.size}\r\n" +
                "Connection: close\r\n" +
                "\r\n"
        return head.toByteArray(Charsets.ISO_8859_1) + bodyBytes
    }

    companion object {
        fun json(status: Int, value: Any?) = HttpResponse(status, Json.stringify(value))

        fun error(status: Int, message: String) = json(status, mapOf("error" to message))
    }
}

private fun reason(status: Int): String = when (status) {
    200 -> "OK"
    201 -> "Created"
    400 -> "Bad Request"
    401 -> "Unauthorized"
    403 -> "Forbidden"
    404 -> "Not Found"
    500 -> "Internal Server Error"
    else -> "Unknown"
}