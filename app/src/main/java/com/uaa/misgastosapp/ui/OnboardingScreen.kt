package com.uaa.misgastosapp.ui

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val pages = listOf(
        OnboardingPage(
            icon = Icons.Default.AccountBalanceWallet,
            title = "Controla tus Gastos",
            description = "Registra tus ingresos y gastos de forma fácil y rápida.",
            color = MaterialTheme.colorScheme.primary
        ),
        OnboardingPage(
            icon = Icons.Default.PieChart,
            title = "Visualiza tu Dinero",
            description = "Gráficos intuitivos muestran dónde va tu dinero.",
            color = MaterialTheme.colorScheme.secondary
        ),
        OnboardingPage(
            icon = Icons.Default.Savings,
            title = "Establece Metas",
            description = "Crea presupuestos por categoría y alcanza tus objetivos.",
            color = MaterialTheme.colorScheme.tertiary
        ),
        OnboardingPage(
            icon = Icons.Default.Repeat,
            title = "Gastos Recurrentes",
            description = "Automatiza tus gastos fijos como alquiler y servicios.",
            color = MaterialTheme.colorScheme.primary
        ),
        OnboardingPage(
            icon = Icons.Default.CloudSync,
            title = "Sincroniza en la Nube",
            description = "Inicia sesión con Google para sincronizar tus datos.",
            color = MaterialTheme.colorScheme.secondary
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pages[pagerState.currentPage].color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Botón saltar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        saveOnboardingCompleted(context)
                        onFinish()
                    }
                ) {
                    Text("Saltar")
                }
            }

            // Pager de páginas
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                OnboardingPageContent(pages[page])
            }

            // Indicadores y botón
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Indicadores de página
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    repeat(pages.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 12.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                        )
                    }
                }

                // Botón siguiente / finalizar
                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            saveOnboardingCompleted(context)
                            onFinish()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = if (pagerState.currentPage < pages.size - 1) "Siguiente" else "¡Comenzar!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = page.color
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun saveOnboardingCompleted(context: Context) {
    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("onboarding_completed", true)
        .apply()
}

fun isOnboardingCompleted(context: Context): Boolean {
    return context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        .getBoolean("onboarding_completed", false)
}
