// AuthViewModel.kt

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

@RequiresApi(Build.VERSION_CODES.O)
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionManager = SecureSessionManager(application)
    private val authRepository: AuthRepository

    init {
        val db = AppDatabase.getInstance(application)
        authRepository = AuthRepository(
            userDao = db.userDao(),
            sessionManager = this.sessionManager
        )
    }

    private val _isLoggedIn = MutableStateFlow(sessionManager.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun login(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (email.isBlank() || password.isBlank()) {
                    onError("Por favor completa todos los campos")
                    return@launch
                }
                // El parámetro 'email' ahora actúa como 'identifier' y se pasa directamente.
                authRepository.login(email, password)
                _isLoggedIn.value = true
                onSuccess()
            } catch (e: Exception) {
                Log.e("AuthVM", "Error en login local: ${e.message}", e)
                onError(e.message ?: "Ocurrió un error inesperado")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(name: String, email: String, username: String, password: String, confirmPassword: String,
                 onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // MODIFICACIÓN: Ahora también se valida que el username no esté vacío.
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
                        // MODIFICACIÓN: Se pasa el 'username' al repositorio.
                        authRepository.register(name, email, username, password)
                        onSuccess()
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthVM", "Error en registro local: ${e.message}", e)
                onError(e.message ?: "Ocurrió un error inesperado")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.logout()
            } catch (e: Exception) {
                Log.e("AuthVM", "Error durante logout local: ${e.message}", e)
            } finally {
                sessionManager.logout()
                _isLoggedIn.value = false
            }
        }
    }

    fun getCurrentUserName(): String? = sessionManager.getUserName()
    fun getCurrentUserId(): Int = sessionManager.getUserId()

    private fun isValidEmail(email: String): Boolean = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    private fun isPasswordValid(password: String): Boolean = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$".toRegex().matches(password)
}