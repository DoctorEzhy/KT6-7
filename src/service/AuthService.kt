package service

import data.Session
import data.User
import repository.SessionRepository
import repository.UserRepository
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.UUID

class AuthService(
    private val users: UserRepository,
    private val sessions: SessionRepository
) {

    fun register(username: String, password: String) {
        val name = username.trim()
        if (!Regex("[A-Za-z0-9_]{3,20}").matches(name)) {
            throw ServiceException(400, "Имя пользователя: 3–20 символов, только латиница, цифры и _")
        }
        if (password.length < 4 || password.length > 100) {
            throw ServiceException(400, "Пароль: от 4 до 100 символов")
        }

        val salt = PasswordHasher.newSalt()
        val created = users.add(User(name, salt, PasswordHasher.hash(password, salt)))
        if (!created) {
            throw ServiceException(400, "Пользователь «$name» уже существует")
        }
    }

    fun login(username: String, password: String): String {
        val user = users.findByUsername(username.trim())
        if (user == null || !PasswordHasher.verify(password, user.salt, user.passwordHash)) {
            throw ServiceException(401, "Неверное имя пользователя или пароль")
        }
        val token = UUID.randomUUID().toString().replace("-", "")
        sessions.add(Session(token, user.username, LocalDateTime.now().withNano(0)))
        return token
    }

    fun usernameFromHeader(header: String?): String? {
        if (header == null) return null
        val parts = header.trim().split(Regex("\\s+"), limit = 2)
        if (parts.size != 2 || !parts[0].equals("Bearer", ignoreCase = true)) return null
        return sessions.findByToken(parts[1].trim())?.username
    }
}

object PasswordHasher {

    fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return toHex(bytes)
    }

    fun hash(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return toHex(digest.digest((salt + password).toByteArray(Charsets.UTF_8)))
    }

    fun verify(password: String, salt: String, expectedHash: String): Boolean =
        MessageDigest.isEqual(
            hash(password, salt).toByteArray(Charsets.UTF_8),
            expectedHash.toByteArray(Charsets.UTF_8)
        )

    private fun toHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }
}