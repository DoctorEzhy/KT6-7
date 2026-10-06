package repository

import data.Message
import java.io.File
import java.time.LocalDateTime

class MessageRepository(private val file: File) {

    private val lock = Any()
    private val messages = mutableListOf<Message>()
    private var nextId = 1

    init {
        for (item in JsonFile.read(file)) {
            messages.add(
                Message(
                    item.int("id"), item.str("author"), item.str("text"),
                    LocalDateTime.parse(item.str("createdAt"))
                )
            )
        }
        nextId = (messages.maxOfOrNull { it.id } ?: 0) + 1
    }

    fun add(author: String, text: String): Message = synchronized(lock) {
        val message = Message(nextId, author, text, LocalDateTime.now().withNano(0))
        messages.add(message)
        try {
            save()
        } catch (e: Exception) {
            messages.remove(message)
            throw e
        }
        nextId++
        message
    }

    fun getAll(): List<Message> = synchronized(lock) { messages.toList() }

    fun findById(id: Int): Message? = synchronized(lock) { messages.find { it.id == id } }

    fun delete(id: Int): Boolean = synchronized(lock) {
        val index = messages.indexOfFirst { it.id == id }
        if (index == -1) return@synchronized false
        val removed = messages.removeAt(index)
        try {
            save()
        } catch (e: Exception) {
            messages.add(index, removed)
            throw e
        }
        true
    }

    private fun save() = JsonFile.write(file, messages.map { it.toMap() })
}