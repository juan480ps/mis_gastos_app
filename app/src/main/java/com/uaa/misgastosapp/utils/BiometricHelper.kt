package com.uaa.misgastosapp.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Helper para autenticación biométrica (huella dactilar / Face ID).
 * Proporciona métodos para verificar disponibilidad y mostrar el prompt.
 */
class BiometricHelper(private val activity: FragmentActivity) {

    /**
     * Resultado de la verificación de disponibilidad.
     */
    sealed class BiometricResult {
        data object Available : BiometricResult()
        data class NotAvailable(val reason: String) : BiometricResult()
    }

    /**
     * Callback para el resultado de la autenticación.
     */
    interface BiometricCallback {
        fun onAuthenticationSuccess()
        fun onAuthenticationError(errorCode: Int, errString: CharSequence)
        fun onAuthenticationFailed()
    }

    /**
     * Verifica si la autenticación biométrica está disponible.
     */
    fun checkBiometricAvailability(): BiometricResult {
        val biometricManager = BiometricManager.from(activity)
        
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricResult.Available
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> 
                BiometricResult.NotAvailable("No hay hardware biométrico disponible")
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> 
                BiometricResult.NotAvailable("El hardware biométrico no está disponible")
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> 
                BiometricResult.NotAvailable("No hay datos biométricos registrados")
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> 
                BiometricResult.NotAvailable("Se requiere actualización de seguridad")
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> 
                BiometricResult.NotAvailable("Autenticación biométrica no soportada")
            else -> BiometricResult.NotAvailable("Error desconocido")
        }
    }

    /**
     * Muestra el prompt de autenticación biométrica.
     */
    fun showBiometricPrompt(
        title: String = "Autenticación Biométrica",
        subtitle: String = "Usa tu huella o rostro para acceder",
        negativeButtonText: String = "Cancelar",
        callback: BiometricCallback
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    callback.onAuthenticationSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    callback.onAuthenticationError(errorCode, errString)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    callback.onAuthenticationFailed()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    companion object {
        private const val PREFS_NAME = "biometric_prefs"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"

        /**
         * Guarda si el usuario habilitó la autenticación biométrica.
         */
        fun setBiometricEnabled(context: Context, enabled: Boolean) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
                .apply()
        }

        /**
         * Verifica si el usuario habilitó la autenticación biométrica.
         */
        fun isBiometricEnabled(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_BIOMETRIC_ENABLED, false)
        }
    }
}
