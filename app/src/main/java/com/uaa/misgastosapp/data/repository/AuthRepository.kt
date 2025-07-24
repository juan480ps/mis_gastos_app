// AuthRepository.kt

package com.uaa.misgastosapp.data.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.uaa.misgastosapp.data.UserDao
import com.uaa.misgastosapp.data.UserEntity
import com.uaa.misgastosapp.network.GastosApiService
import com.uaa.misgastosapp.network.NetworkModule
import com.uaa.misgastosapp.model.LoginRequest
import com.uaa.misgastosapp.model.LoginResponse
import com.uaa.misgastosapp.model.ProfileResponse
import com.uaa.misgastosapp.model.RegisterRequest
import com.uaa.misgastosapp.utils.SecureSessionManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

// esta clase es la encargada de manejar toda la logica de autenticacion (inicio de sesion, registro, etc.).
// combina el acceso a la base de datos local (dao) y al servidor remoto (api).
class AuthRepository(
    private val userDao: UserDao,
    private val sessionManager: SecureSessionManager
) {

    // se crea un acceso directo al servicio de la api para no tener que escribir 'networkmodule.apiservice' cada vez.
    private val apiService: GastosApiService
        get() = NetworkModule.apiService

    // esta funcion se encarga de iniciar sesion a traves del servidor (api).
    suspend fun loginApi(email: String, password: String): LoginResponse {

        // se limpia cualquier sesion o token anterior para asegurar un inicio de sesion limpio.
        sessionManager.clearToken()
        NetworkModule.clearAuthentication()
        // se agrega una pequeña pausa para asegurar que todo se haya limpiado correctamente.
        delay(100)

        // se llama a la funcion de login en la api con el email y la contraseña.
        val response = apiService.login(LoginRequest(identifier = email, password = password))
        // si la respuesta del servidor no es exitosa o no tiene cuerpo, se lanza un error.
        if (!response.isSuccessful || response.body() == null) {
            throw Exception("API Login fallido: ${response.code()} - ${response.errorBody()?.string()}")
        }
        // si todo sale bien, se devuelve la respuesta del servidor.
        return response.body()!!
    }

    // esta funcion obtiene los datos del perfil del usuario desde el servidor.
    suspend fun getProfileApi(): ProfileResponse {
        // se llama a la funcion de obtener perfil en la api.
        val response = apiService.getProfile()
        // si la respuesta no es exitosa, se lanza un error.
        if (!response.isSuccessful || response.body() == null) {
            throw Exception("API GetProfile fallido: ${response.code()} - ${response.errorBody()?.string()}")
        }
        // si todo sale bien, se devuelve el perfil del usuario.
        return response.body()!!
    }

    // esta funcion guarda los datos del perfil del usuario en la base de datos local.
    suspend fun saveUserFromProfile(profile: ProfileResponse, password: String, token: String) {
        // se crea un objeto 'userentity' con los datos del perfil y la contraseña encriptada.
        val localUser = UserEntity(
            id = profile.id,
            email = profile.email,
            password = hashPassword(password), // se encripta la contraseña antes de guardarla.
            name = profile.fullName,
            createdAt = profile.createdAt
        )

        // se intenta actualizar o insertar el usuario en la base de datos local.
        try {
            // se revisa si el usuario ya existe en la base de datos local.
            val existingUser = userDao.getUserById(profile.id)
            if (existingUser != null) {
                // si existe, se actualizan sus datos.
                userDao.update(localUser)
            } else {
                // si no existe, se inserta como un nuevo usuario.
                userDao.insert(localUser)
            }
        } catch (e: Exception) {
            // si ocurre un error (por ejemplo, al intentar actualizar un usuario que no existe), se registra y se intenta insertar.
            Log.e("AuthRepository", "Error saving user, trying insert: ${e.message}")
            try {
                userDao.insert(localUser)
            } catch (insertError: Exception) {
                // si la insercion tambien falla, se registra el error.
                Log.e("AuthRepository", "Insert also failed: ${insertError.message}")
            }
        }

        // finalmente, se guarda la sesion del usuario en el gestor de sesiones seguras.
        sessionManager.saveUserSession(
            userId = profile.id,
            email = profile.email,
            name = profile.fullName,
            username = profile.username,
            accessToken = token
        )
    }

    // esta funcion permite iniciar sesion sin conexion a internet, usando los datos guardados localmente.
    suspend fun loginOffline(email: String, password: String): UserEntity {
        // se encripta la contraseña ingresada para compararla con la que esta guardada.
        val hashedPassword = hashPassword(password)
        // se busca al usuario en la base de datos local. si no se encuentra, se lanza un error.
        val user = userDao.login(email.lowercase(), hashedPassword)
            ?: throw Exception("Credenciales inválidas (modo offline)")

        // si las credenciales son correctas, se guarda una sesion local con un token especial de "modo offline".
        sessionManager.saveUserSession(
            userId = user.id,
            email = user.email,
            name = user.name,
            username = email.substringBefore("@"),
            accessToken = "offline_mode"
        )
        // se devuelve el usuario encontrado.
        return user
    }

    // se asegura que este codigo solo se ejecute en versiones de android compatibles.
    @RequiresApi(Build.VERSION_CODES.O)
    // esta funcion se encarga del registro de un nuevo usuario.
    suspend fun register(name: String, email: String, username: String, password: String) {
        // primero, se intenta registrar al usuario en el servidor.
        val response = apiService.register(
            RegisterRequest(
                fullName = name,
                email = email,
                username = username,
                password = password
            )
        )

        // si el registro en el servidor falla, se lanza un error.
        if (!response.isSuccessful) {
            throw Exception("API Register fallido: ${response.code()} - ${response.errorBody()?.string()}")
        }

        // si el registro en el servidor es exitoso, se guarda el nuevo usuario en la base de datos local.
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
        try {
            // se comprueba si el usuario no esta en modo offline.
            if (sessionManager.getAccessToken() != "offline_mode") {
                try {
                    // si esta online, se intenta cerrar la sesion en el servidor.
                    apiService.logout()
                } catch (e: Exception) {
                    // si la llamada al servidor falla, se registra el error pero se continua con el cierre de sesion local.
                    Log.e("AuthRepository", "API logout failed, continuing with local logout", e)
                }
            }
        } finally {
            // pase lo que pase, siempre se cierra la sesion localmente limpiando los datos guardados.
            sessionManager.logout()
        }
    }

    // esta es una funcion privada que se usa para encriptar contraseñas.
    private fun hashPassword(password: String): String {
        // se usa el algoritmo sha-256 para crear un hash seguro de la contraseña.
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        // se convierte el resultado a un formato de texto hexadecimal.
        return bytes.fold("") { str, it -> str + "%02x".format(it) }
    }
}