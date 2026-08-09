package com.uaa.misgastosapp.domain.usecase.transaction

import com.uaa.misgastosapp.data.repository.TransactionRepository

/**
 * UseCase para eliminar una transacción.
 */
class DeleteTransactionUseCase(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transactionId: Int) {
        require(transactionId > 0) { "ID de transacción inválido" }
        repository.deleteTransaction(transactionId)
    }
}
