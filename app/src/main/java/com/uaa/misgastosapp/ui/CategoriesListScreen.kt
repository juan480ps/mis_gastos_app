// CategoriesListScreen

package com.uaa.misgastosapp.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.uaa.misgastosapp.Routes
import com.uaa.misgastosapp.data.PremiumLimits
import com.uaa.misgastosapp.data.PremiumManager
import com.uaa.misgastosapp.model.Category
import com.uaa.misgastosapp.ui.components.AdBanner
import com.uaa.misgastosapp.ui.components.AppBottomNavBar
import com.uaa.misgastosapp.ui.viewmodel.CategoryViewModel
import com.uaa.misgastosapp.utils.Result

// se usa esta anotacion para poder utilizar componentes de material 3 que aun son experimentales.
@OptIn(ExperimentalMaterial3Api::class)
// aca se define la pantalla (composable) que muestra la lista de categorias.
@Composable
fun CategoriesListScreen(navController: NavController, categoryViewModel: CategoryViewModel = viewModel()) {
    // se obtiene la lista de categorias desde el viewmodel. el estado se actualiza automaticamente.
    val categories by categoryViewModel.categories.collectAsState()
    val isLoading by categoryViewModel.isLoading.collectAsState()

    // se observa el estado de las operaciones (como borrar) para mostrar mensajes al usuario.
    val operationStatus by categoryViewModel.operationStatus.collectAsState()
    val context = LocalContext.current
    // se usan estados para manejar el dialogo de confirmacion de borrado.
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    // se usan estados para manejar el dialogo de renombrar.
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var editedName by rememberSaveable { mutableStateOf("") }

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
        // barra inferior compartida con Inicio/Presupuestos/Recurrentes/Gráficos, para que no
        // desaparezca al entrar a esta sección.
        bottomBar = { AppBottomNavBar(navController, Routes.CATEGORIES_LIST) },
        // se define un boton de accion flotante para añadir nuevas categorias.
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // se avisa el limite del plan Free antes de abrir el formulario, no despues de
                // llenarlo (antes el aviso solo aparecia recien al tocar "Guardar").
                if (!PremiumLimits.canAddCategory(context, categories.size)) {
                    Toast.makeText(
                        context,
                        "Alcanzaste el límite de ${PremiumLimits.FREE_MAX_CATEGORIES} categorías del plan Free. Pasate a Premium para categorías ilimitadas.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    navController.navigate(Routes.ADD_CATEGORY)
                }
            },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir Categoría")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(8.dp)) {
            // se distingue "cargando" (isLoading) de "realmente no hay categorias" (ya cargo y esta vacia).
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (categories.isEmpty()) {
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
                            onEdit = {
                                categoryToEdit = category
                                editedName = category.name
                            },
                            onDelete = {
                                // al hacer clic en borrar, se guarda la categoria a eliminar y se muestra el dialogo.
                                categoryToDelete = category
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
            
            // Banner AdMob (solo usuarios free). Con la barra inferior de navegacion presente,
            // el FAB flota por encima de ella en vez de sobre el contenido, asi que ya no
            // necesita espacio extra reservado.
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

    // se muestra el dialogo para renombrar si hay una categoria seleccionada para editar.
    if (categoryToEdit != null) {
        AlertDialog(
            onDismissRequest = { categoryToEdit = null },
            title = { Text("Renombrar Categoría") },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Nombre de la Categoría") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        categoryToEdit?.let { categoryViewModel.updateCategory(it, editedName) }
                        categoryToEdit = null
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToEdit = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// este es un composable reutilizable para cada elemento de la lista de categorias.
@Composable
fun CategoryListItem(category: Category, onEdit: () -> Unit, onDelete: () -> Unit) {
    // se usa una 'card' para darle un fondo y elevacion al elemento.
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        // se organiza el contenido en una fila.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // se muestra el nombre de la categoria.
            Text(text = category.name, style = MaterialTheme.typography.bodyLarge)
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Renombrar Categoría")
                }
                // se muestra el boton de borrar.
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar Categoría")
                }
            }
        }
    }
}