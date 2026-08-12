// StringExtensions

package com.uaa.misgastosapp.utils

// asegura que el texto guardado empiece con mayuscula, sin importar si el teclado del usuario
// aplico o no la sugerencia de capitalizacion mientras escribia (autocorrector desactivado,
// texto pegado, etc.). Solo toca la primera letra, no el resto del texto.
fun String.capitalizeFirst(): String {
    val trimmed = trim()
    if (trimmed.isEmpty()) return trimmed
    return trimmed[0].uppercase() + trimmed.substring(1)
}
