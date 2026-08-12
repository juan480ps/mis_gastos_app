package com.uaa.misgastosapp.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

/**
 * Genera (una sola vez) y guarda la passphrase que cifra la base de datos Room con SQLCipher.
 * La passphrase se guarda en EncryptedSharedPreferences (respaldada por Android Keystore) en vez
 * de estar hardcodeada en el código, que seria trivial de extraer del APK.
 */
object DatabaseKeyProvider {
    private const val PREFS_NAME = "db_key_prefs"
    private const val KEY_PASSPHRASE = "db_passphrase"
    private const val PASSPHRASE_BYTES = 32 // 256 bits

    fun getOrCreatePassphrase(context: Context): ByteArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val existing = prefs.getString(KEY_PASSPHRASE, null)
        if (existing != null) {
            return android.util.Base64.decode(existing, android.util.Base64.NO_WRAP)
        }

        val newPassphrase = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_PASSPHRASE, android.util.Base64.encodeToString(newPassphrase, android.util.Base64.NO_WRAP))
            .commit()
        return newPassphrase
    }
}
