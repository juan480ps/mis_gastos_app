// SecureSessionManager

package com.uaa.misgastosapp.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.uaa.misgastosapp.BuildConfig
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// esta clase se encarga de gestionar la sesion del usuario de forma segura.
// guarda datos importantes como el id del usuario y el token de acceso de manera encriptada.
class SecureSessionManager(context: Context) {
    // se crea una llave maestra que se usara para encriptar y desencriptar los datos.
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // se crea una instancia de 'encryptedsharedpreferences', que es como un archivo de preferencias
    // pero con la capacidad de encriptar tanto las llaves como los valores que se guardan.
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_user_session", // nombre del archivo de preferencias.
        masterKey, // la llave maestra para la encriptacion.
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // se crea un 'flow' para que otras partes de la app puedan observar cambios en el id del usuario.
    private val _userIdFlow = MutableStateFlow(getUserId())
    val userIdFlow: StateFlow<Int> = _userIdFlow.asStateFlow()

    // 'companion object' se usa para definir constantes que pertenecen a la clase.
    companion object {
        const val USER_ID = "user_id"
        const val USER_EMAIL = "user_email"
        const val USER_NAME = "user_name"
        const val USER_USERNAME = "user_username"
        const val ACCESS_TOKEN = "access_token"
        const val IS_LOGGED_IN = "is_logged_in"
    }

    // esta funcion guarda todos los datos de la sesion del usuario.
    fun saveUserSession(userId: Int, email: String, name: String, username: String, accessToken: String) {
        if (BuildConfig.DEBUG) Log.d("SessionManager", "Saving session - userId: $userId")
        prefs.edit().apply {
            putInt(USER_ID, userId)
            putString(USER_EMAIL, email)
            putString(USER_NAME, name)
            putString(USER_USERNAME, username)
            putString(ACCESS_TOKEN, accessToken)
            putBoolean(IS_LOGGED_IN, true)
            commit() // se usa 'commit' para asegurar que los datos se guarden inmediatamente.
        }
        _userIdFlow.value = userId // se actualiza el valor del 'flow'.
    }

    // esta funcion guarda solo el token de acceso.
    fun saveToken(token: String) {
        prefs.edit()
            .putString(ACCESS_TOKEN, token)
            .commit()
    }

    // esta funcion limpia el token y marca al usuario como no logueado.
    fun clearToken() {
        Log.d("SessionManager", "Clearing token")
        prefs.edit()
            .remove(ACCESS_TOKEN)
            .putBoolean(IS_LOGGED_IN, false)
            .commit()
    }

    // esta funcion obtiene el token de acceso guardado.
    fun getAccessToken(): String? {
        return prefs.getString(ACCESS_TOKEN, null)
    }

    // esta funcion comprueba si el usuario ha iniciado sesion.
    fun isLoggedIn(): Boolean {
        val loggedIn = prefs.getBoolean(IS_LOGGED_IN, false)
        val token = getAccessToken()
        // se considera que la sesion esta iniciada solo si la bandera 'is_logged_in' es verdadera y existe un token valido.
        val hasValidToken = token != null && token.isNotEmpty()
        return loggedIn && hasValidToken
    }

    // las siguientes funciones obtienen datos especificos del usuario.
    fun getUserId(): Int {
        return prefs.getInt(USER_ID, 0)
    }

    fun getUserEmail(): String? {
        return prefs.getString(USER_EMAIL, null)
    }

    fun getUserName(): String? {
        return prefs.getString(USER_NAME, null)
    }

    fun getUserUsername(): String? {
        return prefs.getString(USER_USERNAME, null)
    }

    // esta funcion limpia completamente todos los datos de la sesion. se usa para el cierre de sesion.
    fun logout() {
        Log.d("SessionManager", "Clearing session completely")
        prefs.edit()
            .clear()
            .commit()
        _userIdFlow.value = 0
    }
}