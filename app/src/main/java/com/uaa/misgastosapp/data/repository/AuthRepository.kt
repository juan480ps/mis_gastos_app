// AuthRepository.kt

package com.uaa.misgastosapp.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.uaa.misgastosapp.data.AppDatabase
import com.uaa.misgastosapp.data.UserDao
import com.uaa.misgastosapp.data.UserEntity
import com.uaa.misgastosapp.utils.SecureSessionManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// esta clase maneja toda la logica de autenticacion (inicio de sesion, registro, etc.).
// la app es 100% local: no hay backend ni sync remota (las transacciones/presupuestos/cuentas ya
// vivian solo en Room cifrado con SQLCipher), asi que la cuenta tampoco necesita servidor propio.
class AuthRepository(
    private val userDao: UserDao,
    private val sessionManager: SecureSessionManager,
    // se necesita la base de datos completa (no solo userDao) para poder borrar TODOS los datos
    // locales del usuario (transacciones, presupuestos, cuentas, etc.) al eliminar la cuenta:
    // esta app no tiene datos por-usuario, todo lo del dispositivo pertenece a una sola cuenta.
    private val appDatabase: AppDatabase
) {

    // esta funcion inicia sesion contra la base de datos local.
    suspend fun login(email: String, password: String): UserEntity {
        val emailLower = email.lowercase()
        val user = userDao.getUserByEmail(emailLower)
            ?: throw Exception("Credenciales inválidas")

        if (!verifyPassword(password, user.password)) {
            throw Exception("Credenciales inválidas")
        }

        sessionManager.saveUserSession(
            userId = user.id,
            email = user.email,
            name = user.name,
            username = emailLower.substringBefore("@"),
            // no hay servidor al que enviarle un token: solo se necesita un valor no vacio para
            // que SecureSessionManager.isLoggedIn() lo considere una sesion activa.
            accessToken = localSessionToken()
        )
        return user
    }

    // se asegura que este codigo solo se ejecute en versiones de android compatibles.
    @RequiresApi(Build.VERSION_CODES.O)
    // esta funcion registra un nuevo usuario directamente en la base de datos local.
    suspend fun register(name: String, email: String, username: String, password: String) {
        val emailLower = email.lowercase()
        if (userDao.getUserByEmail(emailLower) != null) {
            throw Exception("El email ya está registrado")
        }

        val newUser = UserEntity(
            email = emailLower,
            password = hashPassword(password),
            name = name,
            createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
        val newUserId = userDao.insert(newUser)

        sessionManager.saveUserSession(
            userId = newUserId.toInt(),
            email = emailLower,
            name = name,
            username = username,
            accessToken = localSessionToken()
        )
    }

    // esta funcion maneja el inicio de sesion con Google: no hay backend que verifique el ID
    // token, asi que se confia en los datos que ya entrega Credential Manager (una API del
    // sistema operativo, no una llamada de red hecha a mano) y se busca/crea el usuario local
    // por email, igual que con email+password.
    suspend fun loginOrRegisterWithGoogle(email: String, displayName: String): UserEntity {
        val emailLower = email.lowercase()
        val existingUser = userDao.getUserByEmail(emailLower)

        val user = existingUser ?: run {
            // las cuentas creadas via Google no tienen contraseña propia: se guarda el hash de un
            // valor aleatorio que nunca coincidira con ninguna contraseña que alguien pueda
            // escribir, en vez de dejar el campo vacio (la columna es NOT NULL).
            val randomPassword = java.security.SecureRandom().let { random ->
                ByteArray(32).also { random.nextBytes(it) }.joinToString("") { "%02x".format(it) }
            }
            val newUser = UserEntity(
                email = emailLower,
                password = hashPassword(randomPassword),
                name = displayName,
                createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
            val newUserId = userDao.insert(newUser)
            newUser.copy(id = newUserId.toInt())
        }

        sessionManager.saveUserSession(
            userId = user.id,
            email = user.email,
            name = user.name,
            username = emailLower.substringBefore("@"),
            accessToken = localSessionToken()
        )
        return user
    }

    // esta funcion cierra la sesion del usuario. no hay servidor al que avisarle: alcanza con
    // limpiar la sesion local.
    fun logout() {
        sessionManager.logout()
    }

    // esta funcion elimina la cuenta y todos los datos financieros locales del dispositivo.
    // requerido por las politicas de Google Play para apps que permiten crear una cuenta.
    suspend fun deleteAccount() {
        // esta app no separa datos por usuario (todo el dispositivo es de una sola cuenta), asi
        // que se borran todas las tablas en vez de solo la fila de 'users'.
        appDatabase.clearAllTables()
        sessionManager.logout()
    }

    // genera un identificador aleatorio para marcar la sesion como activa localmente (ver
    // SecureSessionManager.isLoggedIn, que solo exige que el token no este vacio).
    private fun localSessionToken(): String {
        val bytes = ByteArray(32)
        java.security.SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // esta funcion se usa para encriptar contraseñas con PBKDF2 y salt aleatorio.
    private fun hashPassword(password: String): String {
        // se genera un salt aleatorio de 16 bytes.
        val salt = ByteArray(16)
        java.security.SecureRandom().nextBytes(salt)
        // se usa PBKDF2 con HMAC-SHA256, 100000 iteraciones, y 256 bits de salida.
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, 100000, 256)
        val hash = factory.generateSecret(spec).encoded
        // se convierten salt y hash a hexadecimal.
        val saltHex = salt.joinToString("") { "%02x".format(it) }
        val hashHex = hash.joinToString("") { "%02x".format(it) }
        // se guardan juntos separados por dos puntos para poder verificar despues.
        return "$saltHex:$hashHex"
    }

    // esta funcion verifica si una contraseña coincide con un hash guardado.
    private fun verifyPassword(password: String, storedHash: String): Boolean {
        // se separa el salt y el hash del valor guardado.
        val parts = storedHash.split(":")
        if (parts.size != 2) return false
        // se convierte el salt de hexadecimal a bytes.
        val salt = parts[0].chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        // se calcula el hash de la contraseña ingresada con el mismo salt y parametros.
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, 100000, 256)
        val hash = factory.generateSecret(spec).encoded
        val hashHex = hash.joinToString("") { "%02x".format(it) }
        // se compara el hash calculado con el hash guardado.
        return hashHex == parts[1]
    }
}
