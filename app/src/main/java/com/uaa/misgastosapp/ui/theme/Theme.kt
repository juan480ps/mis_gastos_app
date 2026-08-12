package com.uaa.misgastosapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext

// desplaza el tono (hue) de un color manteniendo su saturacion/brillo, para derivar un
// secondary/tertiary que combine con el primary elegido, en vez de usar un rosa/purpura fijo
// que no tiene relacion con el color del tema activo.
private fun Color.shiftHue(degrees: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toArgb(), hsv)
    hsv[0] = (hsv[0] + degrees).let { if (it < 0f) it + 360f else it % 360f }
    return Color(android.graphics.Color.HSVToColor(hsv))
}

// texto/icono legible encima de 'background': blanco si el fondo es oscuro, casi negro si es claro.
// antes se dejaba que Material3 completara onPrimary/onSecondary/onTertiary con sus valores por
// defecto (pensados para el morado de fabrica), lo que en modo oscuro se notaba como colores que
// no combinaban con el primary/secondary/tertiary que si estaban personalizados.
private fun onColorFor(background: Color): Color =
    if (background.luminance() > 0.5f) Color(0xFF1B1B1B) else Color.White

// tono de "contenedor" (fondo de chips, FAB de baja enfasis, etc.) derivado del mismo color, en
// vez del contenedor morado fijo de Material3 que no tenia relacion con el tema elegido.
private fun containerFor(color: Color, darkTheme: Boolean): Color =
    if (darkTheme) lerp(color, Color.Black, 0.55f) else lerp(color, Color.White, 0.8f)

@Composable
fun GastosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    // color del tema elegido en ThemesScreen, o ModernBlue si el usuario nunca elige uno
    // (antes esto caia en el color dinamico de Android 12+, que sale del wallpaper del
    // usuario y puede terminar en cualquier tono, incluido un rosa que no combina con nada).
    val selectedThemeId by ThemePrefs.currentThemeId(context)
    val primary = selectedThemeId
        ?.let { id -> AppThemes.themes.find { it.id == id } }
        ?.primaryColor
        ?: ModernBlue

    val secondary = primary.shiftHue(24f)
    val tertiary = primary.shiftHue(-24f)

    val primaryContainer = containerFor(primary, darkTheme)
    val secondaryContainer = containerFor(secondary, darkTheme)
    val tertiaryContainer = containerFor(tertiary, darkTheme)

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primary,
            onPrimary = onColorFor(primary),
            primaryContainer = primaryContainer,
            onPrimaryContainer = onColorFor(primaryContainer),
            secondary = secondary,
            onSecondary = onColorFor(secondary),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onColorFor(secondaryContainer),
            tertiary = tertiary,
            onTertiary = onColorFor(tertiary),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onColorFor(tertiaryContainer)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onColorFor(primary),
            primaryContainer = primaryContainer,
            onPrimaryContainer = onColorFor(primaryContainer),
            secondary = secondary,
            onSecondary = onColorFor(secondary),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onColorFor(secondaryContainer),
            tertiary = tertiary,
            onTertiary = onColorFor(tertiary),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onColorFor(tertiaryContainer)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
