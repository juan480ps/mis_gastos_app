// AccountEntity

package com.uaa.misgastosapp.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// representa una cuenta/banco/billetera opcional a la que se pueden asociar transacciones,
// para que el usuario pueda separar sus gastos por banco en vez de tener todo en una sola bolsa.
@Entity(
    tableName = "accounts",
    indices = [Index(value = ["name"], unique = true)]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    // nombre que el usuario le da a la cuenta, ej. "Ahorros Itaú".
    val name: String,
    // nombre del banco, opcional (puede ser una billetera/efectivo sin banco real).
    val bankName: String? = null,
    // color en formato "#RRGGBB" para identificar la cuenta visualmente en las listas.
    val colorHex: String
)
