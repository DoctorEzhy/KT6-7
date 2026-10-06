package service

import data.Message
import repository.MessageRepository

class MessageService(private val messages: MessageRepository) {

    fun create(author: String, text: String): Message {
        val clean = text.trim()
        if (clean.isEmpty()) throw ServiceException(400, "Текст сообщения не может быть пустым")
        if (clean.length > 500) throw ServiceException(400, "Сообщение длиннее 500 символов")
        return messages.add(author, clean)
    }

    fun getAll(): List<Message> = messages.getAll()

    fun delete(id: Int, username: String) {
        val message = messages.findById(id)
            ?: throw ServiceException(404, "Сообщение с ID $id не найдено")
        if (message.author != username) {
            throw ServiceException(403, "Удалить сообщение может только его автор")
        }
        if (!messages.delete(id)) {
            throw ServiceException(404, "Сообщение с ID $id не найдено")
        }
    }
}