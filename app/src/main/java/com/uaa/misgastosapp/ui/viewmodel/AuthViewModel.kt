// AuthViewModel

package com.uaa.misgastosapp.ui.viewmodel

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uaa.misgastosapp.data.AppDatabase
import com.uaa.misgastosapp.data.repository.AuthRepository
import com.uaa.misgastosapp.utils.SecureSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// aca se define el viewmodel para la autenticacion. se encarga de toda la logica de negocio
// relacionada con el inicio de sesion, registro y estado de la sesion del usuario de forma local.
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

    // esta es la funcion principal para iniciar sesion de forma local.
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
                // se llama a la función de login local del repositorio
                authRepository.login(email, password)
                _isLoggedIn.value = true
                onSuccess()
            } catch (e: Exception) {
                // si hay un error, se muestra un mensaje.
                Log.e("AuthVM", "Error en login local: ${e.message}", e)
                onError(e.message ?: "Ocurrió un error inesperado")
            } finally {
                // al final, pase lo que pase, se desactiva el estado de carga.
                _isLoading.value = false
            }
        }
    }

    // se asegura que este codigo solo se ejecute en versiones de android compatibles.
    @RequiresApi(Build.VERSION_CODES.O)
    // esta es la funcion principal para registrar un nuevo usuario de forma local.
    fun register(name: String, email: String, username: String, password: String, confirmPassword: String,
                 onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // se realizan varias validaciones sobre los datos de entrada.
                when {
                    name.isBlank() || email.isBlank() || password.isBlank() ->
                        onError("Por favor completa nombre, email y contraseña")
                    password != confirmPassword ->
                        onError("Las contraseñas no coinciden")
                    !isPasswordValid(password) ->
                        onError("La contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula y un número")
                    !isValidEmail(email) ->
                        onError("Email inválido")
                    else -> {
                        // El username se ignora en el repositorio local, pero se mantiene en la firma
                        authRepository.register(name, email, password)
                        onSuccess()
                    }
                }
            } catch (e: Exception) {
                // si ocurre un error, se muestra el mensaje directamente.
                Log.e("AuthVM", "Error en registro local: ${e.message}", e)
                onError(e.message ?: "Ocurrió un error inesperado")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // esta es la funcion para cerrar la sesion del usuario.
    fun logout() {
        viewModelScope.launch {
            try {
                // se llama al metodo de logout del repositorio (que ahora es solo local).
                authRepository.logout()
            } catch (e: Exception) {
                Log.e("AuthVM", "Error durante logout local: ${e.message}", e)
            } finally {
                // se asegura de limpiar el estado de la sesion en cualquier caso.
                sessionManager.logout()
                _isLoggedIn.value = false
            }
        }
    }

    // funciones para obtener datos del usuario actual desde el gestor de sesiones.
    fun getCurrentUserName(): String? = sessionManager.getUserName()
    fun getCurrentUserId(): Int = sessionManager.getUserId()

    // funciones de validacion privadas.
    private fun isValidEmail(email: String): Boolean = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    private fun isPasswordValid(password: String): Boolean = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$".toRegex().matches(password)
}