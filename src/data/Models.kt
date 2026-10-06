package data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val timeFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME

data class User(val username: String, val salt: String, val passwordHash: String) {
    fun toMap(): Map<String, Any?> = mapOf(
        "username" to username,
        "salt" to salt,
        "passwordHash" to passwordHash
    )
}

data class Session(val token: String, val username: String, val createdAt: LocalDateTime) {
    fun toMap(): Map<String, Any?> = mapOf(
        "token" to token,
        "username" to username,
        "createdAt" to timeFormat.format(createdAt)
    )
}

data class Message(val id: Int, val author: String, val text: String, val createdAt: LocalDateTime) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "author" to author,
        "text" to text,
        "createdAt" to timeFormat.format(createdAt)
    )
}