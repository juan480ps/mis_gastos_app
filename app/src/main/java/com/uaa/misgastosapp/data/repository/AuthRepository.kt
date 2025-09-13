// AuthRepository.kt

package com.uaa.misgastosapp.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.uaa.misgastosapp.data.UserDao
import com.uaa.misgastosapp.data.UserEntity
import com.uaa.misgastosapp.utils.SecureSessionManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AuthRepository(
    private val userDao: UserDao,
    private val sessionManager: SecureSessionManager
) {

    // MODIFICACIÓN: El parámetro 'email' ahora se llama 'identifier' para mayor claridad.
    suspend fun login(identifier: String, password: String): UserEntity {
        val hashedPassword = hashPassword(password)
        // La lógica de negocio no cambia, solo llama al nuevo método del DAO.
        val user = userDao.login(identifier.lowercase(), hashedPassword)
            ?: throw Exception("Credenciales inválidas.")

        sessionManager.saveUserSession(
            userId = user.id,
            email = user.email,
            name = user.name,
            username = user.username, // Ahora se obtiene el username real del usuario
            accessToken = "local_session_token"
        )
        return user
    }

    @RequiresApi(Build.VERSION_CODES.O)
    // MODIFICACIÓN: El método de registro ahora acepta y procesa el 'username'.
    suspend fun register(name: String, email: String, username: String, password: String) {
        // Se valida que ni el email ni el username estén ya en uso.
        if (userDao.getUserByEmail(email.lowercase()) != null) {
            throw Exception("El correo electrónico ya está registrado.")
        }
        if (userDao.getUserByUsername(username.lowercase()) != null) {
            throw Exception("El nombre de usuario ya está en uso.")
        }

        val hashedPassword = hashPassword(password)
        val newUser = UserEntity(
            email = email.lowercase(),
            username = username.lowercase(), // Se guarda el username
            password = hashedPassword,
            name = name,
            createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
        userDao.insert(newUser)
    }

    suspend fun logout() {
        sessionManager.logout()
    }

    private fun hashPassword(password: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.fold("") { str, it -> str + "%02x".format(it) }
    }
}