// LoginScreen

package com.uaa.misgastosapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.uaa.misgastosapp.R
import com.uaa.misgastosapp.Routes
import com.uaa.misgastosapp.ui.viewmodel.AuthViewModel
import android.widget.Toast

// pantalla de login con Google Sign-In como metodo principal.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    val isLoading by authViewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(60.dp))

                // Logo de la aplicacion.
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Logo de Mis Gastos",
                    modifier = Modifier
                        .size(120.dp)
                        .padding(bottom = 16.dp)
                )

                // Nombre y eslogan.
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
                    modifier = Modifier.padding(bottom = 40.dp)
                )

                // Boton de Google Sign-In.
                Button(
                    onClick = {
                        authViewModel.signInWithGoogle(
                            onSuccess = {
                                Toast.makeText(context, "Sesión iniciada con Google", Toast.LENGTH_SHORT).show()
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.LOGIN) { inclusive = true }
                                }
                            },
                            onError = { errorMsg ->
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = "Continuar con Google",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Texto informativo.
                Text(
                    text = "Inicia sesión con tu cuenta de Google para sincronizar tus datos en la nube.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Link para volver al Home sin sesión.
                TextButton(
                    onClick = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text("Usar sin sesión")
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}