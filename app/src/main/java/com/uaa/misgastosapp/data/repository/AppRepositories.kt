package com.uaa.misgastosapp.data.repository

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import com.uaa.misgastosapp.data.AppDatabase
import com.uaa.misgastosapp.utils.SecureSessionManager

/**
 * Punto unico para construir los repositorios a partir de AppDatabase.
 * Evita que cada ViewModel repita "val db = AppDatabase.getInstance(application); Repository(db.xDao(), ...)"
 * en su propio bloque init. No es un framework de DI (el proyecto usa DI manual, ver GastosApp.kt);
 * solo saca la construccion repetida a un solo lugar.
 */
object AppRepositories {

    @RequiresApi(Build.VERSION_CODES.O)
    fun transactionRepository(application: Application): TransactionRepository {
        val db = AppDatabase.getInstance(application)
        return TransactionRepository(db.transactionDao())
    }

    fun categoryRepository(application: Application): CategoryRepository {
        val db = AppDatabase.getInstance(application)
        return CategoryRepository(db.categoryDao())
    }

    fun accountRepository(application: Application): AccountRepository {
        val db = AppDatabase.getInstance(application)
        return AccountRepository(db.accountDao())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun budgetRepository(application: Application): BudgetRepository {
        val db = AppDatabase.getInstance(application)
        return BudgetRepository(db.budgetDao(), db.categoryDao(), db.transactionDao())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun recurringTransactionRepository(application: Application): RecurringTransactionRepository {
        val db = AppDatabase.getInstance(application)
        return RecurringTransactionRepository(db.recurringTransactionDao(), db.transactionDao(), db.categoryDao())
    }

    // AuthRepository necesita el SecureSessionManager que ya crea AuthViewModel, asi que se lo
    // pasa en vez de crear uno nuevo (habria dos instancias distintas del mismo EncryptedSharedPreferences).
    fun authRepository(application: Application, sessionManager: SecureSessionManager): AuthRepository {
        val db = AppDatabase.getInstance(application)
        return AuthRepository(userDao = db.userDao(), sessionManager = sessionManager)
    }
}
