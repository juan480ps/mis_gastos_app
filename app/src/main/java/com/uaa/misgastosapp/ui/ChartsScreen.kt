// ChartsScreen

package com.uaa.misgastosapp.ui

// se importa el color de android con un alias para evitar conflictos con el color de compose.
import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import androidx.compose.ui.platform.LocalContext
import com.uaa.misgastosapp.data.PremiumManager
import com.uaa.misgastosapp.ui.components.AdBanner
import com.uaa.misgastosapp.ui.viewmodel.ChartsViewModel
import com.uaa.misgastosapp.ui.viewmodel.PieChartData
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme

// se asegura que el codigo use apis disponibles a partir de android oreo.
@RequiresApi(Build.VERSION_CODES.O)
// se usa esta anotacion para poder utilizar componentes de material 3 que aun son experimentales.
@OptIn(ExperimentalMaterial3Api::class)
// aca se define la pantalla (composable) que muestra los graficos.
@Composable
fun ChartsScreen(
    navController: NavController,
    chartsViewModel: ChartsViewModel = viewModel()
) {
    // se obtienen los estados desde el viewmodel.
    val currentYearMonth by chartsViewModel.currentMonthYear.collectAsState()
    val processedPieData by chartsViewModel.processedExpensePieData.collectAsState(initial = emptyList())
    // se configuran los formatos de mes y moneda.
    val monthDisplayFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "ES")) }
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("es", "PY")).apply { maximumFractionDigits = 0 }
    }

    // se usa el componente scaffold para la estructura de la pantalla.
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resumen Gráfico") },
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
                },
                // se añade un navegador de meses en la barra de acciones.
                actions = {
                    MonthNavigator(
                        currentYearMonth = currentYearMonth,
                        onPreviousMonth = { chartsViewModel.setCurrentMonthYear(currentYearMonth.minusMonths(1)) },
                        onNextMonth = { chartsViewModel.setCurrentMonthYear(currentYearMonth.plusMonths(1)) }
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // se muestra el titulo con el mes y año actual.
            Text(
                text = "Gastos por Categoría: ${currentYearMonth.format(monthDisplayFormatter).replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // si no hay datos, se muestra un mensaje.
            if (processedPieData.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay datos de gastos para mostrar en este período.")
                }
            } else {
                // si hay datos, se muestra el grafico de torta.
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                ) {
                    MPAndroidPieChart(pieChartDataList = processedPieData)
                }

                Spacer(modifier = Modifier.height(16.dp))
                // se muestra la leyenda del grafico con los detalles de cada categoria.
                processedPieData.forEach { data ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // se muestra un circulo con el color de la categoria.
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .aspectRatio(1f)
                                .background(data.color, shape = androidx.compose.foundation.shape.CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // se muestra el nombre, el monto y el porcentaje del total.
                        Text(
                            text = "${data.label}: ${currencyFormat.format(data.value)} (${String.format("%.1f", (data.value / processedPieData.sumOf { it.value.toDouble() } * 100))}%)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // Banner AdMob (solo usuarios free)
            val context = LocalContext.current
            val isPremium by PremiumManager.getInstance(context).isPremium.collectAsState()
            if (!isPremium) {
                Spacer(modifier = Modifier.height(8.dp))
                AdBanner()
            }
        }
    }
}

// este composable se encarga de mostrar el grafico de torta usando la libreria mpandroidchart.
@Composable
fun MPAndroidPieChart(pieChartDataList: List<PieChartData>) {
    // se ajusta el color del texto segun el tema del sistema.
    val chartTextColor = if (isSystemInDarkTheme()) {
        Color.White
    } else {
        Color.White // Black
    }
    // se usa 'androidview' para integrar una vista de android (el grafico) dentro de compose.
    AndroidView(
        // 'factory' se ejecuta una sola vez para crear la vista.
        factory = { ctx ->
            PieChart(ctx).apply {
                // se aplican varias configuraciones de estilo y comportamiento al grafico.
                this.description.isEnabled = false
                this.isDrawHoleEnabled = true
                this.setHoleColor(AndroidColor.TRANSPARENT)
                this.setTransparentCircleColor(AndroidColor.TRANSPARENT)
                this.setTransparentCircleAlpha(0)
                this.holeRadius = 50f
                this.transparentCircleRadius = 55f
                this.setDrawCenterText(true)
                this.centerText = "Gastos"
                this.setCenterTextSize(16f)
                this.setCenterTextColor(chartTextColor.toArgb())
                this.rotationAngle = 0f
                this.isRotationEnabled = true
                this.isHighlightPerTapEnabled = true
                this.legend.isEnabled = false
                this.setUsePercentValues(true)
                this.setEntryLabelColor(chartTextColor.toArgb())
                this.setEntryLabelTextSize(12f)
            }
        },
        // 'update' se ejecuta cada vez que los datos cambian, para actualizar el grafico.
        update = { pieChart ->
            val entries = ArrayList<PieEntry>()
            for (data in pieChartDataList) {
                entries.add(PieEntry(data.value, data.label))
            }
            val dataSet = PieDataSet(entries, "")
            dataSet.sliceSpace = 2f
            dataSet.selectionShift = 5f
            val colors = ArrayList<Int>()
            for (data in pieChartDataList) {
                colors.add(data.color.toArgb())
            }
            dataSet.colors = colors
            val data = PieData(dataSet)
            data.setValueFormatter(PercentFormatter(pieChart))
            data.setValueTextSize(11f)
            data.setValueTextColor(chartTextColor.toArgb())
            pieChart.data = data
            // se invalida la vista para que se redibuje y se aplica una animacion.
            pieChart.invalidate()
            pieChart.animateY(1000)
        },
        modifier = Modifier.fillMaxSize()
    )
}

