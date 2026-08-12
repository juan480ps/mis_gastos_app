package com.uaa.misgastosapp.data

import android.content.Context
import android.util.Log
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File

/**
 * Migra el archivo de base de datos Room de SQLite plano a SQLCipher cifrado, una sola vez.
 *
 * Instalaciones nuevas: no hay archivo previo, Room lo crea directamente cifrado (ver
 * AppDatabase.getInstance) y esta clase no hace nada.
 *
 * Instalaciones existentes (base de datos ya creada sin cifrar antes de este cambio): se copia
 * el contenido a un archivo temporal cifrado via ATTACH + sqlcipher_export, se verifica que el
 * archivo resultante abre correctamente y tiene el mismo esquema, y solo entonces se reemplaza
 * el archivo original. Si algo falla en el camino, el archivo original no se toca.
 *
 * IMPORTANTE: como fallback de último recurso (para no dejar la app crasheando en un loop si la
 * migración falla de forma persistente), si tras varios intentos el archivo sigue sin poder
 * abrirse cifrado, se descarta el archivo plano y se deja que Room cree una base de datos cifrada
 * vacía. Probar este flujo con datos reales antes de publicar.
 */
object SqlCipherDbMigrator {
    private const val TAG = "SqlCipherDbMigrator"

    fun migrateIfNeeded(context: Context, dbName: String, passphrase: ByteArray) {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) return // instalación nueva: nada que migrar.
        if (isAlreadyEncryptedWith(dbFile, passphrase)) return // ya migrada en un run anterior.
        if (!looksLikePlainSqlite(dbFile)) return // no es un SQLite plano reconocible: no tocar.

        val tempEncrypted = context.getDatabasePath("$dbName.migrating")
        tempEncrypted.delete()

        try {
            val plainTableCount = countTables(dbFile, passphrase = null)
            val hexKey = passphrase.joinToString("") { "%02x".format(it) }

            val plainDb = SQLiteDatabase.openDatabase(
                dbFile.absolutePath, "", null, SQLiteDatabase.OPEN_READWRITE, null
            )
            plainDb.execSQL("ATTACH DATABASE '${tempEncrypted.absolutePath}' AS encrypted KEY \"x'$hexKey'\"")
            plainDb.execSQL("SELECT sqlcipher_export('encrypted')")
            plainDb.execSQL("DETACH DATABASE encrypted")
            plainDb.close()

            val encryptedTableCount = countTables(tempEncrypted, passphrase)
            if (encryptedTableCount == 0 || encryptedTableCount != plainTableCount) {
                throw IllegalStateException(
                    "Verificación de migración falló: tablas plano=$plainTableCount cifrado=$encryptedTableCount"
                )
            }

            deleteDbFiles(context, dbName)
            tempEncrypted.renameTo(dbFile)
            Log.i(TAG, "Base de datos migrada a SQLCipher correctamente ($encryptedTableCount tablas)")
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo migrar la base de datos a SQLCipher, se reintentará en el próximo inicio", e)
            tempEncrypted.delete()
        }
    }

    /**
     * Último recurso: si tras varios inicios la base sigue sin poder migrarse (y por lo tanto
     * Room no puede abrirla cifrada), se descarta para evitar un crash-loop permanente.
     */
    fun discardUnmigratableDatabaseIfStillPlain(context: Context, dbName: String, passphrase: ByteArray) {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) return
        if (isAlreadyEncryptedWith(dbFile, passphrase)) return
        if (!looksLikePlainSqlite(dbFile)) return
        Log.e(TAG, "Descartando base de datos sin cifrar tras fallos de migración repetidos")
        deleteDbFiles(context, dbName)
    }

    private fun countTables(file: File, passphrase: ByteArray?): Int {
        val db = if (passphrase == null) {
            SQLiteDatabase.openDatabase(file.absolutePath, "", null, SQLiteDatabase.OPEN_READONLY, null)
        } else {
            SQLiteDatabase.openDatabase(file.absolutePath, passphrase, null, SQLiteDatabase.OPEN_READONLY, null)
        }
        return try {
            db.rawQuery("SELECT count(*) FROM sqlite_master WHERE type='table'", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
        } finally {
            db.close()
        }
    }

    private fun isAlreadyEncryptedWith(file: File, passphrase: ByteArray): Boolean {
        return try {
            SQLiteDatabase.openDatabase(file.absolutePath, passphrase, null, SQLiteDatabase.OPEN_READONLY, null).close()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun looksLikePlainSqlite(file: File): Boolean {
        return try {
            val header = ByteArray(16)
            file.inputStream().use { it.read(header) }
            String(header, Charsets.US_ASCII).startsWith("SQLite format 3")
        } catch (e: Exception) {
            false
        }
    }

    private fun deleteDbFiles(context: Context, dbName: String) {
        listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
            context.getDatabasePath("$dbName$suffix").delete()
        }
    }
}
