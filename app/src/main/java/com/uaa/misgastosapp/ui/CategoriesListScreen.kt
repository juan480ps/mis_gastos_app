// CategoriesListScreen

package com.uaa.misgastosapp.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.uaa.misgastosapp.Routes
import com.uaa.misgastosapp.data.PremiumManager
import com.uaa.misgastosapp.model.Category
import com.uaa.misgastosapp.ui.components.AdBanner
import com.uaa.misgastosapp.ui.viewmodel.CategoryViewModel
import com.uaa.misgastosapp.utils.Result

// se usa esta anotacion para poder utilizar componentes de material 3 que aun son experimentales.
@OptIn(ExperimentalMaterial3Api::class)
// aca se define la pantalla (composable) que muestra la lista de categorias.
@Composable
fun CategoriesListScreen(navController: NavController, categoryViewModel: CategoryViewModel = viewModel()) {
    // se obtiene la lista de categorias desde el viewmodel. el estado se actualiza automaticamente.
    val categories by categoryViewModel.categories.collectAsState()

    // se observa el estado de las operaciones (como borrar) para mostrar mensajes al usuario.
    val operationStatus by categoryViewModel.operationStatus.collectAsState()
    val context = LocalContext.current
    // se usan estados para manejar el dialogo de confirmacion de borrado.
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    // este efecto se ejecuta cada vez que 'operationstatus' cambia.
    // se usa para mostrar los mensajes de exito o error de las operaciones.
    LaunchedEffect(operationStatus) {
        when (val status = operationStatus) {
            is Result.Success -> {
                Toast.makeText(context, status.data, Toast.LENGTH_SHORT).show()
                // se limpia el estado para que el mensaje no se muestre de nuevo.
                categoryViewModel.clearOperationStatus()
            }
            is Result.Error -> {
                Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                categoryViewModel.clearOperationStatus()
            }
            // no se hace nada para los otros estados.
            is Result.Loading -> {}
            null -> {}
        }
    }

    // se usa el componente scaffold para la estructura de la pantalla.
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorías de Gastos") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        // se define un boton de accion flotante para añadir nuevas categorias.
        floatingActionButton = {
            FloatingActionButton(onClick = {
                navController.navigate(Routes.ADD_CATEGORY) },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir Categoría")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(8.dp)) {
            // si la lista de categorias esta vacia, se muestra un mensaje.
            if (categories.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay categorías. ¡Añade una!")
                }
            } else {
                // si hay categorias, se muestran en una 'lazycolumn' para un desplazamiento eficiente.
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // se crea un item en la lista por cada categoria.
                    items(categories) { category ->
                        CategoryListItem(
                            category = category,
                            onDelete = {
                                // al hacer clic en borrar, se guarda la categoria a eliminar y se muestra el dialogo.
                                categoryToDelete = category
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
            
            // Banner AdMob (solo usuarios free)
            val isPremium by PremiumManager.getInstance(context).isPremium.collectAsState()
            if (!isPremium) {
                Spacer(modifier = Modifier.height(8.dp))
                AdBanner()
            }
        }
    }

    // se muestra el dialogo de confirmacion si 'showdeletedialog' es verdadero.
    if (showDeleteDialog && categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                // si el usuario cierra el dialogo sin elegir, se resetean los estados.
                showDeleteDialog = false
                categoryToDelete = null
            },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Estás seguro de que quieres eliminar la categoría '${categoryToDelete?.name}'? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // si se confirma, se llama al viewmodel para borrar la categoria y se cierra el dialogo.
                        categoryToDelete?.let { categoryViewModel.deleteCategory(it) }
                        showDeleteDialog = false
                        categoryToDelete = null
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        // si se cancela, simplemente se cierra el dialogo.
                        showDeleteDialog = false
                        categoryToDelete = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// este es un composable reutilizable para cada elemento de la lista de categorias.
@Composable
fun CategoryListItem(category: Category, onDelete: () -> Unit) {
    // se usa una 'card' para darle un fondo y elevacion al elemento.
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        // se organiza el contenido en una fila.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // se muestra el nombre de la categoria.
            Text(text = category.name, style = MaterialTheme.typography.bodyLarge)
            // se muestra el boton de borrar.
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar Categoría")
            }
        }
    }
}