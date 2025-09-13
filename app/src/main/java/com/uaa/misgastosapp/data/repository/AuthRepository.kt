// AuthRepository.kt

package com.uaa.misgastosapp.data.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.uaa.misgastosapp.data.UserDao
import com.uaa.misgastosapp.data.UserEntity
import com.uaa.misgastosapp.utils.SecureSessionManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// esta clase es la encargada de manejar toda la logica de autenticacion (inicio de sesion, registro, etc.).
// ahora funciona de manera completamente local, utilizando solo la base de datos (dao).
class AuthRepository(
    private val userDao: UserDao,
    private val sessionManager: SecureSessionManager
) {

    // esta funcion permite iniciar sesion usando los datos guardados localmente.
    suspend fun login(email: String, password: String): UserEntity {
        // se encripta la contraseña ingresada para compararla con la que esta guardada.
        val hashedPassword = hashPassword(password)
        // se busca al usuario en la base de datos local. si no se encuentra, se lanza un error.
        val user = userDao.login(email.lowercase(), hashedPassword)
            ?: throw Exception("Credenciales inválidas.")

        // si las credenciales son correctas, se guarda una sesion local.
        sessionManager.saveUserSession(
            userId = user.id,
            email = user.email,
            name = user.name,
            username = email.substringBefore("@"), // Se genera un nombre de usuario a partir del email
            accessToken = "local_session_token" // Un token genérico para indicar una sesión local activa
        )
        // se devuelve el usuario encontrado.
        return user
    }

    // se asegura que este codigo solo se ejecute en versiones de android compatibles.
    @RequiresApi(Build.VERSION_CODES.O)
    // esta funcion se encarga del registro de un nuevo usuario de forma local.
    suspend fun register(name: String, email: String, password: String) {
        // se verifica si ya existe un usuario con el mismo correo electronico.
        val existingUser = userDao.getUserByEmail(email.lowercase())
        if (existingUser != null) {
            throw Exception("El correo electrónico ya está registrado.")
        }

        // si no existe, se procede con el registro local.
        val hashedPassword = hashPassword(password)
        val newUser = UserEntity(
            email = email.lowercase(),
            password = hashedPassword,
            name = name,
            createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) // se guarda la fecha actual.
        )
        userDao.insert(newUser)
    }

    // esta funcion se encarga de cerrar la sesion del usuario.
    suspend fun logout() {
        // simplemente se cierra la sesion localmente limpiando los datos guardados.
        sessionManager.logout()
    }

    // esta es una funcion privada que se usa para encriptar contraseñas.
    private fun hashPassword(password: String): String {
        // se usa el algoritmo sha-256 para crear un hash seguro de la contraseña.
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        // se convierte el resultado a un formato de texto hexadecimal.
        return bytes.fold("") { str, it -> str + "%02x".format(it) }
    }
}