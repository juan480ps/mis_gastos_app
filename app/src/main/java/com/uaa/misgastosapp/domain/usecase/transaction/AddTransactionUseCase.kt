package com.uaa.misgastosapp.domain.usecase.transaction

import com.uaa.misgastosapp.data.repository.TransactionRepository

/**
 * UseCase para agregar una nueva transacción.
 */
class AddTransactionUseCase(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(
        title: String,
        amount: Double,
        date: String,
        categoryId: Int?
    ) {
        require(title.isNotBlank()) { "El título no puede estar vacío" }
        require(title.length <= 100) { "El título no puede tener más de 100 caracteres" }
        require(amount != 0.0) { "El monto no puede ser cero" }
        require(date.isNotBlank()) { "La fecha no puede estar vacía" }
        repository.insertTransaction(title, amount, date, categoryId)
    }
}
