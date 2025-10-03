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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
                    Button(onClick = {
                        // Normalize to full-day boundaries and then load
                        val startCal = Calendar.getInstance().apply {
                            timeInMillis = startMillis
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val endCal = Calendar.getInstance().apply {
                            timeInMillis = endMillis
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }
                        vm.setRange(startCal.timeInMillis, endCal.timeInMillis)
                        vm.load()
                    }) { Text("Load Report") }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                val total = state.value.expenses.sumOf { it.amount }
                val count = state.value.expenses.size
                val currency = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "ZA")).apply {
                    currency = java.util.Currency.getInstance("ZAR")
                }
                Text(currency.format(total), style = MaterialTheme.typography.titleLarge, color = Color(0xFF00BCD4))
                Text("$count expenses in range", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                DailyLineChart(
                    expenses = state.value.expenses,
                    start = state.value.startMillis,
                    end = state.value.endMillis
                )
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
private fun DailyLineChart(expenses: List<com.example.budgettrain.data.entity.Expense>, start: Long, end: Long) {
    if (expenses.isEmpty() || end <= start) {
        Text("No data to chart")
        return
    }
    val sdf = remember { java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()) }
    val days = remember(start, end, expenses) {
        val byDay = expenses.groupBy { dayKey(it.date) }.mapValues { it.value.sumOf { e -> e.amount } }
        val result = mutableListOf<Pair<Long, Double>>()
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = start
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        while (cal.timeInMillis <= end) {
            val key = dayKey(cal.timeInMillis)
            result.add(cal.timeInMillis to (byDay[key] ?: 0.0))
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        result
    }
    val maxVal = (days.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(1.0)
    Canvas(modifier = Modifier.fillMaxWidth().height(220.dp).padding(horizontal = 8.dp)) {
        // Padding inside canvas for axes
        val leftPad = 48f
        val bottomPad = 24f
        val topPad = 8f
        val rightPad = 8f
        val chartWidth = size.width - leftPad - rightPad
        val chartHeight = size.height - topPad - bottomPad

        // Axes
        drawLine(Color.LightGray, Offset(leftPad, topPad), Offset(leftPad, topPad + chartHeight))
        drawLine(Color.LightGray, Offset(leftPad, topPad + chartHeight), Offset(leftPad + chartWidth, topPad + chartHeight))

        // Y ticks (0, 50%, max)
        val y0 = topPad + chartHeight
        val yMid = topPad + chartHeight * 0.5f
        val yMax = topPad
        drawLine(Color(0xFFE0E0E0), Offset(leftPad, yMid), Offset(leftPad + chartWidth, yMid))

        // X positions
        val stepX = if (days.size > 1) chartWidth / (days.size - 1) else chartWidth

        // Line path
        val path = Path()
        days.forEachIndexed { idx, pair ->
            val value = pair.second.toFloat()
            val px = leftPad + idx * stepX
            val py = y0 - (value / maxVal.toFloat()) * chartHeight
            if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        drawPath(path, color = Color(0xFF00BCD4), style = Stroke(width = 4f))

        // Points
        days.forEachIndexed { idx, pair ->
            val value = pair.second.toFloat()
            val px = leftPad + idx * stepX
            val py = y0 - (value / maxVal.toFloat()) * chartHeight
            drawCircle(Color(0xFF00BCD4), radius = 4f, center = Offset(px, py))
        }
    }
}

private fun dayKey(timeMs: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timeMs))
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


