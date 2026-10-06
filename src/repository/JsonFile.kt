package repository

import util.Json
import util.JsonException
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object JsonFile {

    fun read(file: File): List<Map<String, Any?>> {
        if (!file.exists()) return emptyList()
        val text = file.readText()
        if (text.isBlank()) return emptyList()

        val parsed = try {
            Json.parse(text)
        } catch (e: JsonException) {
            throw IllegalStateException("Файл ${file.path} повреждён: ${e.message}")
        }
        val list = parsed as? List<*>
            ?: throw IllegalStateException("Файл ${file.path}: ожидался JSON-массив")
        return list.map { asObject(it, file) }
    }


    fun write(file: File, items: List<Map<String, Any?>>) {
        file.parentFile?.mkdirs()
        val temp = File(file.path + ".tmp")
        temp.writeText(Json.stringify(items))
        try {
            Files.move(
                temp.toPath(), file.toPath(),
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE
            )
        } catch (e: AtomicMoveNotSupportedException) {
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun asObject(item: Any?, file: File): Map<String, Any?> =
        item as? Map<String, Any?>
            ?: throw IllegalStateException("Файл ${file.path}: элемент массива не является объектом")
}

fun Map<String, Any?>.str(key: String): String =
    this[key] as? String ?: throw IllegalStateException("В данных нет строкового поля «$key»")

fun Map<String, Any?>.int(key: String): Int =
    (this[key] as? Number)?.toInt() ?: throw IllegalStateException("В данных нет числового поля «$key»")