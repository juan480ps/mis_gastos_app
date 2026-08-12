// RecurringTransactionRepository.kt

package com.uaa.misgastosapp.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.uaa.misgastosapp.data.*
import com.uaa.misgastosapp.model.RecurringTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

// una transaccion recurrente que se acaba de procesar (convertir en una transaccion real), para
// poder avisarle al usuario por notificacion que sucedio, en vez de que cambie el balance en
// silencio sin ninguna señal visible.
data class ProcessedRecurringTransaction(val title: String, val amount: Double)

// se asegura que este codigo solo se ejecute en versiones de android compatibles.
@RequiresApi(Build.VERSION_CODES.O)
// esta clase es la encargada de manejar toda la logica relacionada con las transacciones recurrentes.
class RecurringTransactionRepository(
    private val recurringDao: RecurringTransactionDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) {

    // aca se define una variable que contiene una lista de todas las transacciones recurrentes.
    // esta lista se actualiza automaticamente gracias al uso de 'flow'.
    val allRecurringTransactions: Flow<List<RecurringTransaction>> = recurringDao.getAll()
        .map { entities -> entities.map { entity -> toModel(entity) } }

    // esta funcion busca una transaccion recurrente por su id y devuelve el modelo de dominio
    // (no la entidad de Room), para no filtrar el detalle de persistencia hacia el ViewModel/UI.
    suspend fun getById(id: Int): RecurringTransaction? = recurringDao.getById(id)?.let { toModel(it) }

    // convierte la entidad de Room al modelo de dominio, resolviendo el nombre de categoria.
    private suspend fun toModel(entity: RecurringTransactionEntity): RecurringTransaction {
        val categoryName = entity.categoryId?.let { categoryDao.getCategoryNameById(it) }
        return RecurringTransaction(
            id = entity.id,
            title = entity.title,
            amount = entity.amount,
            categoryId = entity.categoryId,
            // si no se encontro un nombre de categoria, se le asigna "sin categoria".
            categoryName = categoryName ?: "Sin Categoría",
            recurrenceType = entity.recurrenceType,
            dayOfMonth = entity.dayOfMonth,
            startDate = entity.startDate,
            endDate = entity.endDate,
            nextDueDate = entity.nextDueDate,
            isActive = entity.isActive
        )
    }

    // esta funcion inserta una nueva transaccion recurrente.
    suspend fun insert(entity: RecurringTransactionEntity) = recurringDao.insert(entity)

    // esta funcion actualiza una transaccion recurrente existente.
    suspend fun update(entity: RecurringTransactionEntity) = recurringDao.update(entity)

    // esta funcion borra una transaccion recurrente.
    suspend fun delete(item: RecurringTransaction) {
        // se convierte el objeto del modelo a una entidad de base de datos antes de borrar.
        val entity = toEntity(item)
        recurringDao.delete(entity)
    }

    // esta es la funcion principal que procesa las transacciones recurrentes que ya vencieron.
    // devuelve las que realmente se procesaron, para poder avisarle al usuario por notificacion.
    suspend fun processDueRecurringTransactions(): List<ProcessedRecurringTransaction> {
        // se obtiene la fecha actual.
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        // se piden al dao todas las transacciones recurrentes vencidas a fecha de hoy.
        val dueItems = recurringDao.getDueRecurringTransactions(today.format(formatter))
        val processed = mutableListOf<ProcessedRecurringTransaction>()

        // se recorre cada una de las transacciones vencidas.
        for (item in dueItems) {
            val nextDueDate = LocalDate.parse(item.nextDueDate, formatter)
            // se obtiene la fecha de fin de la recurrencia, si es que tiene una.
            val endDate = item.endDate?.let { LocalDate.parse(it, formatter) }

            // se comprueba si la transaccion esta inactiva o si su fecha de vencimiento ya paso la fecha final.
            if (!item.isActive || (endDate != null && nextDueDate.isAfter(endDate))) {
                // si la condicion se cumple, se salta a la siguiente transaccion de la lista.
                continue
            }

            // se crea una transaccion normal (un gasto) a partir de los datos de la transaccion recurrente.
            transactionDao.insert(
                TransactionEntity(
                    title = item.title,
                    amount = -item.amount, // se guarda como un numero negativo para que se considere un gasto.
                    date = item.nextDueDate,
                    categoryId = item.categoryId
                )
            )
            processed.add(ProcessedRecurringTransaction(item.title, -item.amount))

            // se calcula cual sera la proxima fecha de vencimiento para esta transaccion recurrente.
            val newNextDueDate = calculateNextDueDate(
                nextDueDate.plusDays(1),
                item.dayOfMonth,
                item.recurrenceType
            )

            // se comprueba si la nueva fecha de vencimiento supera la fecha final de la recurrencia.
            val updatedItem = if (endDate != null && newNextDueDate.isAfter(endDate)) {
                // si la supera, se crea una copia de la transaccion marcada como inactiva.
                item.copy(isActive = false, nextDueDate = newNextDueDate.format(formatter))
            } else {
                // si no, solo se actualiza con la nueva fecha de vencimiento.
                item.copy(nextDueDate = newNextDueDate.format(formatter))
            }
            // se actualiza la transaccion recurrente en la base de datos con los nuevos datos.
            recurringDao.update(updatedItem)
        }
        return processed
    }

    // esta funcion privada se encarga de llamar al metodo de calculo correcto segun el tipo de recurrencia.
    private fun calculateNextDueDate(fromDate: LocalDate, dayOfMonth: Int, type: RecurrenceType): LocalDate {
        return when (type) {
            RecurrenceType.MONTHLY -> getNextMonthlyDueDate(fromDate, dayOfMonth)
        }
    }

    // esta funcion privada calcula la proxima fecha para una recurrencia mensual.
    private fun getNextMonthlyDueDate(currentDate: LocalDate, day: Int): LocalDate {
        // se obtiene el mes y año de la fecha actual.
        var yearMonth = YearMonth.from(currentDate)
        // se comprueba si el dia de vencimiento de este mes ya paso.
        if (currentDate.dayOfMonth >= day) {
            // si ya paso, se avanza al siguiente mes.
            yearMonth = yearMonth.plusMonths(1)
        }
        // se ajusta el dia por si el proximo mes tiene menos dias (ej. 31 en un mes de 30).
        val targetDay = day.coerceAtMost(yearMonth.lengthOfMonth())
        // se devuelve la nueva fecha calculada.
        return yearMonth.atDay(targetDay)
    }

    // esta funcion privada convierte un objeto del modelo de la app a una entidad de base de datos.
    private fun toEntity(item: RecurringTransaction): RecurringTransactionEntity {
        return RecurringTransactionEntity(
            id = item.id,
            title = item.title,
            amount = item.amount,
            categoryId = item.categoryId,
            recurrenceType = item.recurrenceType,
            dayOfMonth = item.dayOfMonth,
            startDate = item.startDate,
            endDate = item.endDate,
            nextDueDate = item.nextDueDate,
            isActive = item.isActive
        )
    }
}