package repository

import data.User
import java.io.File

class UserRepository(private val file: File) {

    private val lock = Any()
    private val users = mutableListOf<User>()

    init {
        for (item in JsonFile.read(file)) {
            users.add(User(item.str("username"), item.str("salt"), item.str("passwordHash")))
        }
    }

    fun findByUsername(username: String): User? = synchronized(lock) {
        users.find { it.username == username }
    }

    fun add(user: User): Boolean = synchronized(lock) {
        if (users.any { it.username == user.username }) return@synchronized false
        users.add(user)
        try {
            save()
        } catch (e: Exception) {
            users.remove(user)
            throw e
        }
        true
    }

    private fun save() = JsonFile.write(file, users.map { it.toMap() })
}