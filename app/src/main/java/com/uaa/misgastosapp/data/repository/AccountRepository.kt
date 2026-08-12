// AccountRepository.kt

package com.uaa.misgastosapp.data.repository

import com.uaa.misgastosapp.data.AccountDao
import com.uaa.misgastosapp.data.AccountEntity
import com.uaa.misgastosapp.model.Account
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// esta clase es la encargada de manejar la logica de las cuentas/bancos.
// sirve como un intermediario entre la base de datos (dao) y la interfaz de usuario.
class AccountRepository(private val accountDao: AccountDao) {

    val allAccounts: Flow<List<Account>> = accountDao.getAll()
        .map { entities ->
            entities.map { Account(id = it.id, name = it.name, bankName = it.bankName, colorHex = it.colorHex) }
        }

    suspend fun insertAccount(name: String, bankName: String?, colorHex: String): Long {
        val existingAccounts = allAccounts.first()
        if (existingAccounts.any { it.name.equals(name, ignoreCase = true) }) {
            throw IllegalStateException("La cuenta '$name' ya existe.")
        }
        return accountDao.insert(AccountEntity(name = name, bankName = bankName, colorHex = colorHex))
    }

    suspend fun updateAccount(id: Int, name: String, bankName: String?, colorHex: String) {
        val existingAccounts = allAccounts.first()
        if (existingAccounts.any { it.id != id && it.name.equals(name, ignoreCase = true) }) {
            throw IllegalStateException("La cuenta '$name' ya existe.")
        }
        accountDao.update(AccountEntity(id = id, name = name, bankName = bankName, colorHex = colorHex))
    }

    // borrar una cuenta no borra sus transacciones: quedan sin cuenta asignada (ver
    // ForeignKey.SET_NULL en TransactionEntity).
    suspend fun deleteAccount(account: Account) {
        val entity = AccountEntity(id = account.id, name = account.name, bankName = account.bankName, colorHex = account.colorHex)
        accountDao.delete(entity)
    }
}
