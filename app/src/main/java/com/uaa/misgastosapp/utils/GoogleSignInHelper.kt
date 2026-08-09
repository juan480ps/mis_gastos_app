package com.uaa.misgastosapp.utils

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Helper para manejar Google Sign-In usando Credential Manager.
 * Obtiene el ID token de Google que se puede enviar al backend para autenticación.
 */
class GoogleSignInHelper(private val context: Context) {

    companion object {
        private const val TAG = "GoogleSignInHelper"
        // TODO: Reemplazar con tu Web Client ID de Google Cloud Console
        // https://console.cloud.google.com/apis/credentials
        const val WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
    }

    // Estado del proceso de Google Sign-In
    private val _signInState = MutableStateFlow<GoogleSignInState>(GoogleSignInState.Idle)
    val signInState: StateFlow<GoogleSignInState> = _signInState.asStateFlow()

    /**
     * Inicia el proceso de Google Sign-In.
     * @param onSuccess Callback con el resultado exitoso
     * @param onError Callback con el mensaje de error
     */
    suspend fun signIn(
        onSuccess: (GoogleIdTokenCredential) -> Unit,
        onError: (String) -> Unit
    ) {
        _signInState.value = GoogleSignInState.Loading

        try {
            val credentialManager = CredentialManager.create(context)

            // Configurar la solicitud para Google ID Token
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setServerClientId(WEB_CLIENT_ID)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // Ejecutar la solicitud
            val result: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = context
            )

            // Procesar la respuesta
            handleSignInResult(result, onSuccess, onError)

        } catch (e: NoCredentialException) {
            Log.e(TAG, "No hay credenciales de Google disponibles", e)
            _signInState.value = GoogleSignInState.Error("No se encontraron cuentas de Google en el dispositivo")
            onError("No se encontraron cuentas de Google en el dispositivo")
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado en Google Sign-In", e)
            _signInState.value = GoogleSignInState.Error("Error inesperado: ${e.message}")
            onError("Error inesperado: ${e.message}")
        }
    }

    /**
     * Procesa la respuesta de Google Sign-In.
     */
    private fun handleSignInResult(
        result: GetCredentialResponse,
        onSuccess: (GoogleIdTokenCredential) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            when (val credential = result.credential) {
                is GoogleIdTokenCredential -> {
                    Log.d(TAG, "Google Sign-In exitoso: ${credential.id}")
                    _signInState.value = GoogleSignInState.Success(credential)
                    onSuccess(credential)
                }
                else -> {
                    Log.e(TAG, "Tipo de credencial inesperado: ${credential.type}")
                    _signInState.value = GoogleSignInState.Error("Tipo de credencial no soportado")
                    onError("Tipo de credencial no soportado")
                }
            }
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(TAG, "Error al parsear Google ID Token", e)
            _signInState.value = GoogleSignInState.Error("Error al procesar token de Google")
            onError("Error al procesar token de Google")
        }
    }

    /**
     * Cierra la sesión de Google (limpia credenciales cacheadas).
     */
    suspend fun signOut() {
        try {
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(
                androidx.credentials.ClearCredentialStateRequest()
            )
            _signInState.value = GoogleSignInState.Idle
            Log.d(TAG, "Google Sign-Out exitoso")
        } catch (e: ClearCredentialException) {
            Log.e(TAG, "Error al cerrar sesión de Google", e)
        }
    }
}

/**
 * Estados del proceso de Google Sign-In.
 */
sealed class GoogleSignInState {
    object Idle : GoogleSignInState()
    object Loading : GoogleSignInState()
    data class Success(val credential: GoogleIdTokenCredential) : GoogleSignInState()
    data class Error(val message: String) : GoogleSignInState()
}
