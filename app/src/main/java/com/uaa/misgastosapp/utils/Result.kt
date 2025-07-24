package com.uaa.misgastosapp.utils

// aca se define una 'clase sellada' (sealed class).
// es una forma de crear un tipo de dato que solo puede tener un numero limitado de subtipos.
// se usa para representar los diferentes estados de una operacion: exito, error o en progreso.
sealed class Result<out T> {
    // esta clase representa el caso de exito de la operacion.
    // contiene los 'data' (datos) del resultado, que pueden ser de cualquier tipo.
    data class Success<T>(val data: T) : Result<T>()
    // esta clase representa el caso de error de la operacion.
    // contiene un 'message' (mensaje) que describe que salio mal.
    data class Error(val message: String) : Result<Nothing>()
    // este es un objeto que representa el estado de 'cargando' o 'en progreso' de la operacion.
    // es un 'object' porque no necesita guardar ninguna informacion adicional.
    object Loading : Result<Nothing>()
}