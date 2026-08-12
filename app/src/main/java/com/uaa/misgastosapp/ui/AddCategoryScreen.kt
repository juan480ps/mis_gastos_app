// AddCategoryScreen

package com.uaa.misgastosapp.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.uaa.misgastosapp.data.PremiumLimits
import com.uaa.misgastosapp.data.PremiumManager
import com.uaa.misgastosapp.ui.components.AdBanner
import com.uaa.misgastosapp.ui.viewmodel.CategoryViewModel
import com.uaa.misgastosapp.utils.Result

// se usa esta anotacion para poder utilizar componentes de material 3 que aun son experimentales.
@OptIn(ExperimentalMaterial3Api::class)
// aca se define la pantalla (composable) para añadir una nueva categoria.
@Composable
fun AddCategoryScreen(navController: NavController, categoryViewModel: CategoryViewModel = viewModel()) {
    // se crea una variable de estado para guardar el nombre de la categoria que el usuario escribe.
    var categoryName by remember { mutableStateOf("") }
    // se obtiene el contexto actual, que es necesario para mostrar mensajes (toast).
    val context = LocalContext.current
    val categories by categoryViewModel.categories.collectAsState()
    val operationStatus by categoryViewModel.operationStatus.collectAsState()

    // se observa el resultado de addCategory: exito muestra el toast y vuelve atras, error solo avisa.
    LaunchedEffect(operationStatus) {
        when (val status = operationStatus) {
            is Result.Success -> {
                Toast.makeText(context, status.data, Toast.LENGTH_SHORT).show()
                categoryViewModel.clearOperationStatus()
                // se le pasa el id de la categoria recien creada a la pantalla anterior, para que
                // pueda dejarla seleccionada en su combo en vez de quedar en "Sin Categoría".
                navController.previousBackStackEntry?.savedStateHandle?.set(
                    "newCategoryId", categoryViewModel.lastCreatedCategoryId.value
                )
                navController.popBackStack()
            }
            is Result.Error -> {
                Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                categoryViewModel.clearOperationStatus()
            }
            is Result.Loading -> {}
            null -> {}
        }
    }

    // se usa el componente scaffold que provee una estructura basica de pantalla con barra superior, etc.
    Scaffold(
        // aca se configura la barra superior de la pantalla.
        topBar = {
            // se define el contenido y apariencia de la barra superior.
            TopAppBar(
                // se establece el titulo de la pantalla.
                title = { Text("Añadir Nueva Categoría") },
                // se personalizan los colores de la barra superior.
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                // se define el icono de navegacion, en este caso, el boton para volver atras.
                navigationIcon = {
                    // al hacer clic en el boton, se navega a la pantalla anterior.
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
        // el contenido de la pantalla se coloca dentro de este bloque, usando los margenes que provee el scaffold.
    ) { paddingValues ->
        // se organiza el contenido en una columna vertical.
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // se crea un campo de texto con borde para que el usuario ingrese el nombre.
            OutlinedTextField(
                value = categoryName,
                // cada vez que el texto cambia, se actualiza la variable de estado 'categoryname'.
                onValueChange = { categoryName = it },
                label = { Text("Nombre de la Categoría") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            // se crea el boton para guardar la categoria.
            Button(
                // aca se define la logica que se ejecuta al presionar el boton.
                onClick = {
                    // se comprueba que el nombre no este vacio.
                    if (categoryName.isNotBlank()) {
                        // se aplica el limite de categorias del plan Free antes de guardar.
                        if (!PremiumLimits.canAddCategory(context, categories.size)) {
                            Toast.makeText(
                                context,
                                "Alcanzaste el límite de ${PremiumLimits.FREE_MAX_CATEGORIES} categorías del plan Free. Pasate a Premium para categorías ilimitadas.",
                                Toast.LENGTH_LONG
                            ).show()
                            return@Button
                        }
                        // se llama a la funcion del viewmodel para añadir la categoria; el resultado
                        // se maneja en el LaunchedEffect que observa operationStatus.
                        categoryViewModel.addCategory(categoryName)
                    } else {
                        // si el nombre esta vacio, se muestra un mensaje de validacion.
                        Toast.makeText(context, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar Categoría")
            }
            
            // Banner AdMob (solo usuarios free)
            val isPremium by PremiumManager.getInstance(context).isPremium.collectAsState()
            if (!isPremium) {
                Spacer(modifier = Modifier.height(8.dp))
                AdBanner()
            }
        }
    }
}