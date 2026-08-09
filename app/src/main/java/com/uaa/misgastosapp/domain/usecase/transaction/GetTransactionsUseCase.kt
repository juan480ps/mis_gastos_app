package com.uaa.misgastosapp.domain.usecase.transaction

import com.uaa.misgastosapp.data.repository.TransactionRepository
import com.uaa.misgastosapp.model.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * UseCase para obtener la lista de transacciones.
 */
class GetTransactionsUseCase(
    private val repository: TransactionRepository
) {
    operator fun invoke(): Flow<List<Transaction>> {
        return repository.allTransactions
    }
}
