// AuthViewModel

package com.uaa.misgastosapp.ui.viewmodel

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uaa.misgastosapp.data.repository.AppRepositories
import com.uaa.misgastosapp.data.repository.AuthRepository
import com.uaa.misgastosapp.utils.SecureSessionManager
import com.uaa.misgastosapp.utils.GoogleSignInHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// aca se define el viewmodel para la autenticacion. se encarga de toda la logica de negocio
// relacionada con el inicio de sesion, registro y estado de la sesion del usuario.
// la app es 100% local (ver AuthRepository): no hay backend, asi que no existe un modo
// online/offline distinto, todo corre siempre contra la base de datos cifrada del dispositivo.
// las dependencias se pueden inyectar (para tests, igual que en TransactionViewModel);
// @JvmOverloads genera el constructor de un solo parametro que necesita el ViewModelProvider
// por defecto para instanciarlo en produccion.
class AuthViewModel @JvmOverloads constructor(
    application: Application,
    private val sessionManager: SecureSessionManager = SecureSessionManager(application),
    private val authRepository: AuthRepository = AppRepositories.authRepository(application, sessionManager),
    private val googleSignInHelper: GoogleSignInHelper = GoogleSignInHelper(application)
) : AndroidViewModel(application) {

    // se crea un 'stateflow' para saber si el usuario ha iniciado sesion. es privado para que solo el viewmodel lo pueda modificar.
    private val _isLoggedIn = MutableStateFlow(sessionManager.isLoggedIn())
    // esta es la version publica y de solo lectura del estado de la sesion, para que la interfaz la observe.
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // se crea un estado para controlar si se esta mostrando una pantalla de carga.
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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

                authRepository.login(email, password)
                _isLoggedIn.value = true
                onSuccess()
            } catch (e: Exception) {
                Log.e("AuthVM", "Error en login: ${e.message}", e)
                onError(e.message ?: "Credenciales inválidas")
            } finally {
                // al final, pase lo que pase, se desactiva el estado de carga.
                _isLoading.value = false
            }
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
                        try {
                            // si todas las validaciones pasan, se registra localmente y se inicia sesion.
                            authRepository.register(name, email, username, password)
                            _isLoggedIn.value = true
                            onSuccess()
                        } catch (e: Exception) {
                            Log.e("AuthVM", "Error en registro: ${e.message}", e)
                            onError(e.message ?: "No se pudo completar el registro")
                        }
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    // esta es la funcion para cerrar la sesion del usuario.
    fun logout() {
        viewModelScope.launch {
            // se cierra sesion de Google tambien (limpia credenciales cacheadas del dispositivo).
            googleSignInHelper.signOut()
            authRepository.logout()
            _isLoggedIn.value = false
        }
    }

    // esta funcion maneja el inicio de sesion con Google. no hay backend que verifique el ID
    // token: se confia en los datos que entrega Credential Manager (una API del sistema
    // operativo, no una llamada de red hecha a mano) y se busca/crea el usuario local por email,
    // igual que con email+password (ver AuthRepository.loginOrRegisterWithGoogle).
    fun signInWithGoogle(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                googleSignInHelper.signIn(
                    onSuccess = { credential ->
                        viewModelScope.launch {
                            try {
                                val email = credential.id
                                val name = credential.displayName ?: email.substringBefore("@")
                                authRepository.loginOrRegisterWithGoogle(email, name)
                                _isLoggedIn.value = true
                                onSuccess()
                            } catch (e: Exception) {
                                Log.e("AuthVM", "Error en Google Sign-In: ${e.message}", e)
                                onError("No se pudo iniciar sesión con Google: ${e.message}")
                            } finally {
                                _isLoading.value = false
                            }
                        }
                    },
                    onError = { errorMsg ->
                        _isLoading.value = false
                        onError(errorMsg)
                    }
                )
            } catch (e: Exception) {
                Log.e("AuthVM", "Error en Google Sign-In: ${e.message}", e)
                _isLoading.value = false
                onError("Error al iniciar sesión con Google: ${e.message}")
            }
        }
    }

    // esta funcion elimina la cuenta y todos los datos financieros locales del dispositivo (ver
    // AuthRepository.deleteAccount). Requerido por las politicas de Google Play para apps que
    // permiten crear una cuenta.
    fun deleteAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                authRepository.deleteAccount()
                _isLoggedIn.value = false
                onSuccess()
            } catch (e: Exception) {
                Log.e("AuthVM", "Error eliminando cuenta: ${e.message}", e)
                onError("No se pudo eliminar la cuenta: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // funciones para obtener datos del usuario actual desde el gestor de sesiones.
    fun getCurrentUserName(): String? = sessionManager.getUserName()
    fun getCurrentUserId(): Int = sessionManager.getUserId()

    // funciones de validacion privadas.
    // se usa un regex propio en vez de android.util.Patterns.EMAIL_ADDRESS: ese campo depende del
    // framework de Android real y en el stub que usan los tests unitarios de JVM (sin Robolectric)
    // vale null, lo que hacia que esta validacion lanzara NullPointerException en tests.
    private fun isValidEmail(email: String): Boolean =
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex().matches(email)
    private fun isPasswordValid(password: String): Boolean = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$".toRegex().matches(password)
}
