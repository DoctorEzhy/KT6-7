package repository

import data.Session
import java.io.File
import java.time.LocalDateTime

class SessionRepository(private val file: File) {

    private val lock = Any()
    private val sessions = mutableListOf<Session>()

    init {
        for (item in JsonFile.read(file)) {
            sessions.add(
                Session(item.str("token"), item.str("username"), LocalDateTime.parse(item.str("createdAt")))
            )
        }
    }

    fun add(session: Session) {
        synchronized(lock) {
            sessions.add(session)
            try {
                save()
            } catch (e: Exception) {
                sessions.remove(session)
                throw e
            }
        }
    }

    fun findByToken(token: String): Session? = synchronized(lock) {
        sessions.find { it.token == token }
    }

    private fun save() = JsonFile.write(file, sessions.map { it.toMap() })
}