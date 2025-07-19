// SecureSessionManager

package com.uaa.misgastosapp.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SecureSessionManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_user_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _userIdFlow = MutableStateFlow(getUserId())
    val userIdFlow: StateFlow<Int> = _userIdFlow.asStateFlow()

    companion object {
        const val USER_ID = "user_id"
        const val USER_EMAIL = "user_email"
        const val USER_NAME = "user_name"
        const val USER_USERNAME = "user_username"
        const val ACCESS_TOKEN = "access_token"
        const val IS_LOGGED_IN = "is_logged_in"
    }

    fun saveUserSession(userId: Int, email: String, name: String, username: String, accessToken: String) {
        Log.d("SessionManager", "Saving session - userId: $userId, token: ${accessToken.take(20)}...")
        prefs.edit().apply {
            putInt(USER_ID, userId)
            putString(USER_EMAIL, email)
            putString(USER_NAME, name)
            putString(USER_USERNAME, username)
            putString(ACCESS_TOKEN, accessToken)
            putBoolean(IS_LOGGED_IN, true)
            commit() // Using commit for immediate synchronous save
        }
        _userIdFlow.value = userId
    }

    fun saveToken(token: String) {
        Log.d("SessionManager", "Saving token: ${token.take(20)}...")
        prefs.edit()
            .putString(ACCESS_TOKEN, token)
            .commit()
    }

    fun clearToken() {
        Log.d("SessionManager", "Clearing token")
        prefs.edit()
            .remove(ACCESS_TOKEN)
            .putBoolean(IS_LOGGED_IN, false)
            .commit()
    }

    fun getAccessToken(): String? {
        val token = prefs.getString(ACCESS_TOKEN, null)
        Log.d("SessionManager", "Getting token: ${token?.take(20) ?: "null"}")
        return token
    }

    fun isLoggedIn(): Boolean {
        val loggedIn = prefs.getBoolean(IS_LOGGED_IN, false)
        val token = getAccessToken()
        val hasValidToken = token != null && token.isNotEmpty()
        Log.d("SessionManager", "isLoggedIn: $loggedIn, hasValidToken: $hasValidToken")
        return loggedIn && hasValidToken
    }

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

    fun logout() {
        Log.d("SessionManager", "Clearing session completely")
        prefs.edit()
            .clear()
            .commit()
        _userIdFlow.value = 0
    }
}