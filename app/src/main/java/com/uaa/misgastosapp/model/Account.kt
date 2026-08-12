// Account

package com.uaa.misgastosapp.model

// modelo de dominio para una cuenta/banco/billetera opcional. Separado de AccountEntity para no
// filtrar el detalle de persistencia (Room) hacia ViewModels/UI.
data class Account(
    val id: Int,
    val name: String,
    val bankName: String?,
    val colorHex: String
)
