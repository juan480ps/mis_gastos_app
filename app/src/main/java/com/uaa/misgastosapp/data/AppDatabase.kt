package com.uaa.misgastosapp.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

class Converters {
    @TypeConverter
    fun fromRecurrenceType(value: RecurrenceType): String = value.name
    @TypeConverter
    fun toRecurrenceType(value: String): RecurrenceType = RecurrenceType.valueOf(value)
}

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        RecurringTransactionEntity::class,
        UserEntity::class,
        AccountEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun userDao(): UserDao
    abstract fun accountDao(): AccountDao

    companion object {
        private const val TAG = "AppDatabase"
        private const val DB_NAME = "gastos_db"

        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        // La base de datos contiene transacciones, presupuestos y el hash de password de
        // UserEntity, asi que se cifra en disco con SQLCipher en vez de quedar como SQLite plano.
        private fun buildDatabase(appContext: Context): AppDatabase {
            val passphrase = DatabaseKeyProvider.getOrCreatePassphrase(appContext)
            SqlCipherDbMigrator.migrateIfNeeded(appContext, DB_NAME, passphrase)

            fun build(): AppDatabase = Room.databaseBuilder(appContext, AppDatabase::class.java, DB_NAME)
                .openHelperFactory(SupportOpenHelperFactory(passphrase))
                .addMigrations(*Migrations.ALL_MIGRATIONS)
                .build()

            return try {
                // se fuerza la apertura ahora (en vez de en el primer query) para detectar de
                // inmediato si la migracion dejo un archivo que Room no puede abrir cifrado.
                build().also { it.openHelper.writableDatabase }
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo abrir la base de datos cifrada; se descarta el archivo previo y se reintenta", e)
                SqlCipherDbMigrator.discardUnmigratableDatabaseIfStillPlain(appContext, DB_NAME, passphrase)
                build()
            }
        }
    }
}
