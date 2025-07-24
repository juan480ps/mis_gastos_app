// LoginScreen

package com.uaa.misgastosapp.ui

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.uaa.misgastosapp.R
import com.uaa.misgastosapp.Routes
import com.uaa.misgastosapp.ui.viewmodel.AuthViewModel
import android.widget.Toast
import androidx.compose.ui.text.style.TextAlign

// se usa esta anotacion para poder utilizar componentes de material 3 que aun son experimentales.
@OptIn(ExperimentalMaterial3Api::class)
// aca se define la pantalla (composable) de inicio de sesion.
@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    // se definen los estados para los campos del formulario y la visibilidad de la contraseña.
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }
    // se observa el estado de carga desde el viewmodel.
    val isLoading by authViewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // se obtiene una instancia de 'sharedpreferences' para guardar las credenciales si el usuario lo desea.
    val sharedPreferences = remember {
        context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
    }

    // este efecto se ejecuta una vez al crear la pantalla para cargar las credenciales guardadas.
    LaunchedEffect(Unit) {
        val savedEmail = sharedPreferences.getString("email", null)
        val savedPassword = sharedPreferences.getString("password", null)
        if (savedEmail != null && savedPassword != null) {
            email = savedEmail
            password = savedPassword
            rememberMe = true
        }
    }

    // se usa el componente scaffold como contenedor principal de la pantalla.
    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // se usa una columna con scroll para que el contenido se ajuste en pantallas pequeñas.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                // se muestra el logo de la aplicacion.
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Logo de Mis Gastos",
                    modifier = Modifier
                        .size(120.dp)
                        .padding(bottom = 16.dp)
                )

                // se muestra el nombre de la aplicacion y un eslogan.
                Text(
                    text = "Mis Gastos",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Controla tus finanzas personales",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // se agrupan los campos de texto y el boton en una tarjeta para un mejor diseño.
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Iniciar Sesión",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // campo de texto para el email o usuario.
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email o Usuario") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isLoading
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // campo de texto para la contraseña.
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Contraseña") },
                            // se cambia la transformacion visual para ocultar o mostrar la contraseña.
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isLoading
                        )

                        // checkbox para la opcion de "recordar credenciales".
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                enabled = !isLoading
                            )
                            Text(
                                text = "Recordar credenciales",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // boton de inicio de sesion.
                        Button(
                            onClick = {
                                // se llama al viewmodel para que inicie la sesion.
                                authViewModel.login(
                                    email = email.trim(),
                                    password = password,
                                    onSuccess = {
                                        // si el login es exitoso, se guardan o borran las credenciales segun la opcion del usuario.
                                        with(sharedPreferences.edit()) {
                                            if (rememberMe) {
                                                putString("email", email)
                                                putString("password", password)
                                            } else {
                                                remove("email")
                                                remove("password")
                                            }
                                            apply()
                                        }
                                        // se navega a la pantalla principal, limpiando la pantalla de login del historial.
                                        navController.navigate(Routes.HOME) {
                                            popUpTo(Routes.LOGIN) { inclusive = true }
                                        }
                                    },
                                    onError = { errorMsg ->
                                        // si hay un error, se muestra un mensaje.
                                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
                        ) {
                            // se muestra un indicador de carga dentro del boton si 'isloading' es verdadero.
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Iniciar Sesión", fontSize = 16.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // se muestra un texto informativo sobre la necesidad de conexion.
                Text(
                    text = "Se requiere conexión a internet para iniciar sesión.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // boton para navegar a la pantalla de registro.
                TextButton(
                    onClick = { navController.navigate(Routes.REGISTER) },
                    enabled = !isLoading
                ) {
                    Text("¿No tienes cuenta? Regístrate")
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}