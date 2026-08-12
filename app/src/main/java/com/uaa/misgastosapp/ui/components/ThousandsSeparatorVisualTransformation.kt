// ThousandsSeparatorVisualTransformation

package com.uaa.misgastosapp.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Inserta separadores de miles solo para MOSTRAR el numero; el texto real que guarda el
 * TextField sigue siendo puros digitos (ej. "100000"). Antes, los campos de monto reformateaban
 * el string real en cada tecla (agregando comas y quitando el punto decimal), lo que hacia que el
 * cursor saltara al final del campo cada vez que se escribia un digito, y que un "." tipeado por
 * el usuario desapareciera sin aviso. Al transformar solo la visualizacion, el cursor se mantiene
 * en su lugar y no hay nada que "desaparezca".
 *
 * Los montos de la app son siempre guaranies enteros (sin decimales, igual que el resto de la UI
 * que ya usa maximumFractionDigits = 0), asi que el campo que use esto debe filtrar la entrada a
 * solo digitos.
 */
class ThousandsSeparatorVisualTransformation(private val groupingSeparator: Char = '.') : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val grouped = StringBuilder()
        // originalToTransformedOffsets[i] = posicion en 'grouped' justo antes de agregar digits[i].
        val originalToTransformedOffsets = IntArray(digits.length + 1)

        for (i in digits.indices) {
            val digitsFromEnd = digits.length - i
            if (i != 0 && digitsFromEnd % 3 == 0) {
                grouped.append(groupingSeparator)
            }
            originalToTransformedOffsets[i] = grouped.length
            grouped.append(digits[i])
        }
        originalToTransformedOffsets[digits.length] = grouped.length

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, digits.length)
                return originalToTransformedOffsets[clamped]
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, grouped.length)
                var result = 0
                for (i in 0..digits.length) {
                    if (originalToTransformedOffsets[i] <= clamped) {
                        result = i
                    } else {
                        break
                    }
                }
                return result
            }
        }

        return TransformedText(AnnotatedString(grouped.toString()), offsetMapping)
    }
}
