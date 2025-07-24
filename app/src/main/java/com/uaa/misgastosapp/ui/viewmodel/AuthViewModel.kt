// AuthViewModel

package com.uaa.misgastosapp.ui.viewmodel

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.uaa.misgastosapp.data.AppDatabase
import com.uaa.misgastosapp.data.repository.AuthRepository
import com.uaa.misgastosapp.network.NetworkModule
import com.uaa.misgastosapp.model.ErrorResponse
import com.uaa.misgastosapp.utils.SecureSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.delay

// aca se define el viewmodel para la autenticacion. se encarga de toda la logica de negocio
// relacionada con el inicio de sesion, registro y estado de la sesion del usuario.
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    // se crea una instancia del gestor de sesiones seguras.
    private val sessionManager = SecureSessionManager(application)
    // se declara el repositorio de autenticacion, que sera la unica fuente de datos.
    private val authRepository: AuthRepository

    // el bloque 'init' se ejecuta cuando se crea una instancia de este viewmodel.
    init {
        // se obtiene la instancia de la base de datos.
        val db = AppDatabase.getInstance(application)
        // se inicializa el repositorio de autenticacion, pasandole el dao de usuario y el gestor de sesiones.
        authRepository = AuthRepository(
            userDao = db.userDao(),
            sessionManager = this.sessionManager
        )
    }

    // se crea un 'stateflow' para saber si el usuario ha iniciado sesion. es privado para que solo el viewmodel lo pueda modificar.
    private val _isLoggedIn = MutableStateFlow(sessionManager.isLoggedIn())
    // esta es la version publica y de solo lectura del estado de la sesion, para que la interfaz la observe.
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // se crea un estado para controlar si se esta mostrando una pantalla de carga.
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // se crea un estado para saber si la app esta funcionando en modo online u offline.
    private val _isOnlineMode = MutableStateFlow(true)
    val isOnlineMode: StateFlow<Boolean> = _isOnlineMode.asStateFlow()

    // esta es la funcion principal para iniciar sesion.
    fun login(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        // se inicia una corutina en el ambito del viewmodel, para no bloquear la interfaz de usuario.
        viewModelScope.launch {
            // se activa el estado de carga.
            _isLoading.value = true
            try {
                // se comprueba que los campos no esten vacios.
                if (email.isBlank() || password.isBlank()) {
                    onError("Por favor completa todos los campos")
                    return@launch
                }

                // se comprueba si la app esta en modo online.
                if (_isOnlineMode.value) {
                    try {
                        // se intenta iniciar sesion a traves de la api.
                        val loginResponse = authRepository.loginApi(email, password)
                        val token = loginResponse.accessToken
                        sessionManager.saveToken(token) // se guarda el token.
                        NetworkModule.updateApiClient() // se actualiza el cliente de red con el nuevo token.
                        delay(200) // una pequeña pausa para asegurar la actualizacion.
                        val profileResponse = authRepository.getProfileApi() // se obtiene el perfil del usuario.
                        authRepository.saveUserFromProfile(profileResponse, password, token) // se guarda el perfil en la base de datos local.
                        _isLoggedIn.value = true // se actualiza el estado de la sesion.
                        _isOnlineMode.value = true // se confirma el modo online.
                        onSuccess() // se notifica que el login fue exitoso.

                    } catch (e: UnknownHostException) {
                        // si no hay conexion, se intenta el login offline.
                        Log.e("AuthVM", "Sin conexión, intentando login offline.", e)
                        _isOnlineMode.value = false
                        performOfflineLogin(email, password, onSuccess, onError)
                    } catch (e: SocketTimeoutException) {
                        // si la conexion tarda mucho, se intenta el login offline.
                        Log.e("AuthVM", "Timeout, intentando login offline.", e)
                        _isOnlineMode.value = false
                        performOfflineLogin(email, password, onSuccess, onError)
                    } catch (e: Exception) {
                        // si ocurre otro error en el login online, se muestra un mensaje de error parseado.
                        Log.e("AuthVM", "Error en login online: ${e.message}", e)
                        onError(parseApiErrorMessage(e.message ?: "Ocurrió un error inesperado"))
                    }
                } else {
                    // si ya se estaba en modo offline, se intenta el login offline directamente.
                    performOfflineLogin(email, password, onSuccess, onError)
                }
            } finally {
                // al final, pase lo que pase, se desactiva el estado de carga.
                _isLoading.value = false
            }
        }
    }

    // esta es una funcion privada para manejar el login sin conexion.
    private suspend fun performOfflineLogin(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        try {
            // se llama al metodo de login offline del repositorio.
            authRepository.loginOffline(email, password)
            _isLoggedIn.value = true
            onSuccess()
        } catch (e: Exception) {
            // si falla, se muestra un mensaje de error.
            Log.e("AuthVM", "Error en login offline: ${e.message}", e)
            onError(e.message ?: "Error desconocido en modo offline")
        }
    }

    // se asegura que este codigo solo se ejecute en versiones de android compatibles.
    @RequiresApi(Build.VERSION_CODES.O)
    // esta es la funcion principal para registrar un nuevo usuario.
    fun register(name: String, email: String, username: String, password: String, confirmPassword: String,
                 onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // se realizan varias validaciones sobre los datos de entrada.
                when {
                    name.isBlank() || email.isBlank() || username.isBlank() || password.isBlank() ->
                        onError("Por favor completa todos los campos")
                    password != confirmPassword ->
                        onError("Las contraseñas no coinciden")
                    !isPasswordValid(password) ->
                        onError("La contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula y un número")
                    !isValidEmail(email) ->
                        onError("Email inválido")
                    else -> {
                        // si todas las validaciones pasan, se llama al metodo de registro del repositorio.
                        authRepository.register(name, email, username, password)
                        onSuccess()
                    }
                }
            } catch (e: Exception) {
                // si ocurre un error, se parsea y se muestra.
                Log.e("AuthVM", "Error en registro: ${e.message}", e)
                onError(parseApiErrorMessage(e.message ?: "Ocurrió un error inesperado"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    // esta es la funcion para cerrar la sesion del usuario.
    fun logout() {
        viewModelScope.launch {
            try {
                // se llama al metodo de logout del repositorio.
                authRepository.logout()
                _isLoggedIn.value = false
                _isOnlineMode.value = true
                delay(100)
                NetworkModule.clearAuthentication() // se limpia la autenticacion del cliente de red.

            } catch (e: Exception) {
                // si el logout falla (por ejemplo, por no tener conexion), se asegura de limpiar la sesion localmente.
                Log.e("AuthVM", "Error durante logout: ${e.message}", e)
                _isLoggedIn.value = false
                _isOnlineMode.value = true
                sessionManager.logout()
                NetworkModule.clearAuthentication()
            }
        }
    }

    // esta funcion privada intenta convertir los mensajes de error de la api a un formato mas legible.
    private fun parseApiErrorMessage(rawMessage: String): String {
        return try {
            // se intenta parsear el mensaje como un objeto json de error.
            if (rawMessage.contains("{") && rawMessage.contains("}")) {
                val jsonStart = rawMessage.indexOf("{")
                val jsonEnd = rawMessage.lastIndexOf("}") + 1
                val jsonString = rawMessage.substring(jsonStart, jsonEnd)
                val error = Gson().fromJson(jsonString, ErrorResponse::class.java)
                error.msg ?: error.error ?: "Error del servidor"
            } else {
                // si no es un json, se revisa si el texto contiene codigos de error http comunes.
                when {
                    rawMessage.contains("401") -> "Credenciales inválidas"
                    rawMessage.contains("409") -> "El email o usuario ya existe"
                    rawMessage.contains("500") -> "Error del servidor"
                    rawMessage.contains("404") -> "Servicio no disponible"
                    else -> "Ocurrió un error inesperado"
                }
            }
        } catch (e: Exception) {
            // si todo falla, se devuelve un mensaje generico.
            Log.e("AuthVM", "Error parsing error message: ${e.message}")
            "Ocurrió un error inesperado"
        }
    }

    // funciones para obtener datos del usuario actual desde el gestor de sesiones.
    fun getCurrentUserName(): String? = sessionManager.getUserName()
    fun getCurrentUserId(): Int = sessionManager.getUserId()

    // funciones de validacion privadas.
    private fun isValidEmail(email: String): Boolean = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    private fun isPasswordValid(password: String): Boolean = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$".toRegex().matches(password)
}