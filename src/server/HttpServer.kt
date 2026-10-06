package server

import controller.Router
import util.Logger
import java.io.BufferedInputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.Executors

class HttpServer(
    private val port: Int,
    private val router: Router,
    private val logger: Logger,
    poolSize: Int = 16
) {
    private val pool = Executors.newFixedThreadPool(poolSize)

    fun start() {
        ServerSocket(port).use { serverSocket ->
            Runtime.getRuntime().addShutdownHook(Thread {
                serverSocket.close()
                pool.shutdown()
            })
            println("Сервер запущен: http://localhost:$port")

            while (!serverSocket.isClosed) {
                try {
                    val socket = serverSocket.accept()
                    pool.execute { handleClient(socket) }
                } catch (e: IOException) {
                    if (!serverSocket.isClosed) logger.error("Ошибка при приёме соединения", e)
                }
            }
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.use { serve(it) }
        } catch (e: SocketTimeoutException) {
            logger.error("Клиент слишком долго молчал (таймаут)")
        } catch (e: IOException) {
            logger.error("Соединение оборвано", e)
        } catch (e: Exception) {
            logger.error("Неожиданная ошибка при обслуживании клиента", e)
        }
    }

    private fun serve(socket: Socket) {
        socket.soTimeout = 10_000
        val input = BufferedInputStream(socket.getInputStream())

        val response = try {
            val request = HttpParser.parse(input) ?: return
            router.handle(request)
        } catch (e: BadRequestException) {
            HttpResponse.error(400, e.message ?: "Bad Request")
        }

        val output = socket.getOutputStream()
        output.write(response.toBytes())
        output.flush()
    }
}