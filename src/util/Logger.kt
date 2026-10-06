package util

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Logger(private val requestsFile: File, private val errorsFile: File) {

    private val lock = Any()

    private fun now(): String =
        DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now().withNano(0))

    fun request(method: String, path: String, user: String?) {
        write(requestsFile, "${now()} | $method | $path | ${user ?: "guest"}")
    }

    fun error(message: String, cause: Throwable? = null) {
        val details = if (cause != null) " | ${cause::class.simpleName}: ${cause.message}" else ""
        write(errorsFile, "${now()} | ERROR | $message$details")
    }

    private fun write(file: File, line: String) {
        synchronized(lock) {
            println(line)
            try {
                file.parentFile?.mkdirs()
                file.appendText(line + "\n")
            } catch (e: Exception) {
                System.err.println("Не удалось записать лог: ${e.message}")
            }
        }
    }
}