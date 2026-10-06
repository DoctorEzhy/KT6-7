//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
import controller.Router
import repository.MessageRepository
import repository.SessionRepository
import repository.UserRepository
import server.HttpServer
import service.AuthService
import service.MessageService
import util.Logger
import java.io.File

fun main() {
    val storage = File("storage")
    val logger = Logger(File(storage, "requests.log"), File(storage, "errors.log"))

    try {
        val userRepository = UserRepository(File(storage, "users.json"))
        val sessionRepository = SessionRepository(File(storage, "sessions.json"))
        val messageRepository = MessageRepository(File(storage, "messages.json"))

        val router = Router(
            AuthService(userRepository, sessionRepository),
            MessageService(messageRepository),
            logger
        )
        HttpServer(8080, router, logger).start()
    } catch (e: Exception) {
        println("Не удалось запустить сервер: ${e.message}")
        logger.error("Сервер не запущен", e)
    }
}