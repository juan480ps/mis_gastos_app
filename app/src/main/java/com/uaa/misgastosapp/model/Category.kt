// Category

package com.uaa.misgastosapp.model

// aca se define una 'data class' que representa el modelo de una categoria, tal como se usa en la logica de la aplicacion.
data class Category(
    // se guarda el id unico de la categoria.
    val id: Int,
    // se guarda el nombre de la categoria.
    val name: String
) {
    // aca se sobreescribe la funcion 'tostring'.
    // esto es util para que cuando se use el objeto en un contexto de texto, como en un menu desplegable,
    // se muestre directamente el nombre de la categoria en lugar de la informacion tecnica del objeto.
    override fun toString(): String {
        return name
    }
}