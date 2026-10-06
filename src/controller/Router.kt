package controller

import server.HttpRequest
import server.HttpResponse
import service.AuthService
import service.MessageService
import service.ServiceException
import util.Json
import util.JsonException
import util.Logger

class Router(
    private val auth: AuthService,
    private val messages: MessageService,
    private val logger: Logger
) {

    fun handle(request: HttpRequest): HttpResponse {
        var user: String? = null
        val response = try {
            user = auth.usernameFromHeader(request.headers["authorization"])
            route(request, user)
        } catch (e: ServiceException) {
            HttpResponse.error(e.status, e.message ?: "Ошибка")
        } catch (e: JsonException) {
            HttpResponse.error(400, "Некорректный JSON: ${e.message}")
        } catch (e: Exception) {
            logger.error("Необработанная ошибка: ${request.method} ${request.path}", e)
            HttpResponse.error(500, "Внутренняя ошибка сервера")
        }
        logger.request(request.method, request.path, user)
        return response
    }

    private fun route(request: HttpRequest, user: String?): HttpResponse {
        val method = request.method
        val path = request.path
        return when {
            method == "POST" && path == "/register" -> register(request)
            method == "POST" && path == "/login" -> login(request)
            method == "GET" && path == "/messages" -> {
                requireUser(user)
                HttpResponse.json(200, messages.getAll().map { it.toMap() })
            }
            method == "POST" && path == "/messages" -> createMessage(request, requireUser(user))
            method == "DELETE" && path.startsWith("/messages/") -> deleteMessage(path, requireUser(user))
            else -> throw ServiceException(404, "Маршрут не найден: $method $path")
        }
    }

    private fun register(request: HttpRequest): HttpResponse {
        val body = readBody(request)
        auth.register(body.requireString("username"), body.requireString("password"))
        return HttpResponse.json(201, mapOf("message" to "Пользователь зарегистрирован"))
    }

    private fun login(request: HttpRequest): HttpResponse {
        val body = readBody(request)
        val token = auth.login(body.requireString("username"), body.requireString("password"))
        return HttpResponse.json(200, mapOf("token" to token))
    }

    private fun createMessage(request: HttpRequest, user: String): HttpResponse {
        val text = readBody(request).requireString("text")
        val message = messages.create(user, text)
        return HttpResponse.json(201, message.toMap())
    }

    private fun deleteMessage(path: String, user: String): HttpResponse {
        val id = path.removePrefix("/messages/").toIntOrNull()
            ?: throw ServiceException(400, "ID сообщения должен быть числом")
        messages.delete(id, user)
        return HttpResponse.json(200, mapOf("message" to "Сообщение удалено"))
    }

    private fun requireUser(user: String?): String =
        user ?: throw ServiceException(401, "Нужна авторизация: заголовок «Authorization: Bearer <token>»")

    private fun readBody(request: HttpRequest): Map<String, Any?> {
        if (request.body.isBlank()) throw ServiceException(400, "Тело запроса пустое, ожидался JSON")
        return Json.parseObject(request.body)
    }

    private fun Map<String, Any?>.requireString(key: String): String =
        this[key] as? String ?: throw ServiceException(400, "Поле «$key» обязательно и должно быть строкой")
}