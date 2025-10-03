package com.example.budgettrain.feature.reports

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.budgettrain.data.dao.CategoryTotal
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ReportsScreen(vm: ReportsViewModel = viewModel()) {
    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val cal = remember { Calendar.getInstance() }
    var startMillis by remember { mutableStateOf(cal.clone().let { it as Calendar; it.set(Calendar.DAY_OF_MONTH, 1); it.timeInMillis }) }
    var endMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    vm.setRange(startMillis, endMillis)
    val state = vm.state

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Budget Train", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF2196F3))
        Text("FOR KEEPING YOUR\nBUDGETS ON TRACK", style = MaterialTheme.typography.labelSmall, color = Color(0xFF607D8B))
        Spacer(Modifier.height(8.dp))
        Text("Reports & Graphs", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Card(elevation = CardDefaults.cardElevation(4.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        val c = Calendar.getInstance().apply { timeInMillis = startMillis }
                        DatePickerDialog(context, { _, y, m, d ->
                            Calendar.getInstance().apply { set(y, m, d, 0, 0, 0); startMillis = timeInMillis }
                        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                    }) { Text("Start: ${sdf.format(startMillis)}", maxLines = 1, overflow = TextOverflow.Ellipsis) }

                    Button(onClick = {
                        val c = Calendar.getInstance().apply { timeInMillis = endMillis }
                        DatePickerDialog(context, { _, y, m, d ->
                            Calendar.getInstance().apply { set(y, m, d, 23, 59, 59); endMillis = timeInMillis }
                        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                    }) { Text("End: ${sdf.format(endMillis)}", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.load() }) { Text("Load Report") }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("R 2,350.00", style = MaterialTheme.typography.titleLarge, color = Color(0xFF00BCD4))
                        Text("70% Overall Budget Spent", style = MaterialTheme.typography.bodySmall)
                    }
                    PaydayCard(days = 10)
                }
                Spacer(Modifier.height(12.dp))
                SimpleBars()
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Budget Trends", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Jan to Aug overview")
                Spacer(Modifier.height(8.dp))
                CategoryTotalsList(totalsFlow = state.value.categoryTotals)
            }
        }
    }
}

@Composable
private fun PaydayCard(days: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp)) {
            Text("Days Til Pay Day", style = MaterialTheme.typography.bodySmall)
            Text("$days", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun SimpleBars() {
    val values = listOf(40, 70, 50, 65, 80, 55, 75, 45)
    val labels = listOf("JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG")
    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp).padding(horizontal = 8.dp)) {
        val barWidth = size.width / (values.size * 2)
        val maxVal = (values.maxOrNull() ?: 1).toFloat()
        values.forEachIndexed { index, v ->
            val left = index * 2 * barWidth + barWidth / 2
            val barHeight = (v / maxVal) * (size.height * 0.7f)
            drawRoundRect(
                color = Color(0xFF00BCD4),
                topLeft = Offset(left, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
            )
        }
    }
}

@Composable
private fun CategoryTotalsList(totalsFlow: List<CategoryTotal>) {
    if (totalsFlow.isEmpty()) {
        Text("No data for selected range")
    } else {
        val currency = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "ZA")).apply {
            currency = java.util.Currency.getInstance("ZAR")
        }
        LazyColumn {
            items(totalsFlow) { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(row.categoryName)
                    Text(currency.format(row.total))
                }
            }
        }
    }
}


