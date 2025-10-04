package com.example.budgettrain.feature.reports

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.budgettrain.data.dao.CategoryTotal
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.content.Context
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

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun ReportsScreen(vm: ReportsViewModel = viewModel()) {
    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val cal = remember { Calendar.getInstance() }
    var startMillis by remember { mutableStateOf(cal.clone().let { it as Calendar; it.set(Calendar.DAY_OF_MONTH, 1); it.timeInMillis }) }
    var endMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    val state by vm.state.collectAsState()

    // Show error dialog if there's an error
    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = { /* Error will be cleared when new data loads */ },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                Button(onClick = { /* Error will be cleared when new data loads */ }) {
                    Text("OK")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            BrandHeader()
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            Text("Reports & Graphs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            Card(
                elevation = CardDefaults.cardElevation(4.dp), 
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Date Range Filter",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF2196F3)
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                val c = Calendar.getInstance().apply { timeInMillis = startMillis }
                                DatePickerDialog(context, { _, y, m, d ->
                                    Calendar.getInstance().apply { set(y, m, d, 0, 0, 0); startMillis = timeInMillis }
                                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                            modifier = Modifier.weight(1f)
                        ) { Text("Start: ${sdf.format(startMillis)}", maxLines = 1, overflow = TextOverflow.Ellipsis) }

                        Button(
                            onClick = {
                                val c = Calendar.getInstance().apply { timeInMillis = endMillis }
                                DatePickerDialog(context, { _, y, m, d ->
                                    Calendar.getInstance().apply { set(y, m, d, 23, 59, 59); endMillis = timeInMillis }
                                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                            modifier = Modifier.weight(1f)
                        ) { Text("End: ${sdf.format(endMillis)}", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
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
                            },
                            enabled = !state.loading,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) { 
                            if (state.loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Load Report")
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                elevation = CardDefaults.cardElevation(4.dp), 
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(16.dp)) {
                    val total = state.expenses.sumOf { it.amount }
                    val count = state.expenses.size
                    val currency = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "ZA")).apply {
                        currency = java.util.Currency.getInstance("ZAR")
                    }
                    Text(
                        "Expense Summary",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF2196F3)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        currency.format(total), 
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        "$count expenses in range", 
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF757575)
                    )
                    Spacer(Modifier.height(12.dp))
                    DailyLineChart(
                        expenses = state.expenses,
                        start = state.startMillis,
                        end = state.endMillis
                    )
                }
            }
        }

        item {
            Card(
                elevation = CardDefaults.cardElevation(4.dp), 
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Budget Trends", 
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF2196F3)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${sdf.format(state.startMillis)} to ${sdf.format(state.endMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF757575)
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    // Budget Status Indicator
                    val totalSpent = state.expenses.sumOf { it.amount }
                    val prefs = context.getSharedPreferences("budget_goals", Context.MODE_PRIVATE)
                    val minGoal = prefs.getFloat("min_goal", 0f).toDouble()
                    val maxGoal = prefs.getFloat("max_goal", 0f).toDouble()
                    
                    if (maxGoal > 0) {
                        BudgetStatusIndicator(
                            totalSpent = totalSpent,
                            minGoal = minGoal,
                            maxGoal = maxGoal
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    
                    // Trend Analysis
                    val previousTotal = state.previousPeriodExpenses.sumOf { it.amount }
                    if (previousTotal > 0) {
                        TrendAnalysisIndicator(
                            currentTotal = totalSpent,
                            previousTotal = previousTotal
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    
                    CategoryTotalsList(totalsFlow = state.categoryTotals, totalSpent = totalSpent)
                }
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Budget Train",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFF2196F3)
        )
        Text(
            text = "FOR KEEPING YOUR\nBUDGETS ON TRACK",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF607D8B)
        )
    }
}

@Composable
private fun BudgetStatusIndicator(totalSpent: Double, minGoal: Double, maxGoal: Double) {
    val currency = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "ZA")).apply {
        currency = java.util.Currency.getInstance("ZAR")
    }
    
    val (status, statusColor, statusIcon, statusText) = when {
        totalSpent < minGoal -> {
            Quadruple(
                "Under Minimum",
                Color(0xFF4CAF50), // Green
                Icons.Default.CheckCircle,
                "You're spending below your minimum goal"
            )
        }
        totalSpent < maxGoal * 0.8 -> {
            Quadruple(
                "On Track",
                Color(0xFF2196F3), // Blue
                Icons.Default.CheckCircle,
                "Great! You're well within your budget"
            )
        }
        totalSpent <= maxGoal -> {
            Quadruple(
                "Approaching Limit",
                Color(0xFFFF9800), // Orange
                Icons.Default.Warning,
                "You're approaching your maximum budget"
            )
        }
        else -> {
            Quadruple(
                "Over Budget",
                Color(0xFFF44336), // Red
                Icons.Default.Error,
                "You've exceeded your maximum budget"
            )
        }
    }
    
    val progressPercent = if (maxGoal > 0) (totalSpent / maxGoal * 100).coerceAtMost(100.0) else 0.0
    
    Card(
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = status,
                tint = statusColor,
                modifier = Modifier.height(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666)
                )
                Text(
                    text = "${String.format("%.1f", progressPercent)}% of max budget (${currency.format(maxGoal)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

@Composable
private fun TrendAnalysisIndicator(currentTotal: Double, previousTotal: Double) {
    val currency = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "ZA")).apply {
        currency = java.util.Currency.getInstance("ZAR")
    }
    
    val difference = currentTotal - previousTotal
    val percentageChange = if (previousTotal > 0) (difference / previousTotal * 100) else 0.0
    
    val (trendIcon, trendColor, trendText) = when {
        difference > 0 -> {
            Triple(
                Icons.Default.TrendingUp,
                Color(0xFFF44336), // Red for increase
                "Spending increased"
            )
        }
        difference < 0 -> {
            Triple(
                Icons.Default.TrendingDown,
                Color(0xFF4CAF50), // Green for decrease
                "Spending decreased"
            )
        }
        else -> {
            Triple(
                Icons.Default.TrendingFlat,
                Color(0xFF666666), // Gray for no change
                "Spending unchanged"
            )
        }
    }
    
    Card(
        colors = CardDefaults.cardColors(containerColor = trendColor.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = trendIcon,
                contentDescription = trendText,
                tint = trendColor,
                modifier = Modifier.height(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "vs Previous Period",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = trendColor
                )
                Text(
                    text = trendText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666)
                )
                Text(
                    text = "${if (difference >= 0) "+" else ""}${currency.format(difference)} (${String.format("%.1f", percentageChange)}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666)
                )
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
        drawPath(path, color = Color(0xFF2196F3), style = Stroke(width = 4f))

        // Points
        days.forEachIndexed { idx, pair ->
            val value = pair.second.toFloat()
            val px = leftPad + idx * stepX
            val py = y0 - (value / maxVal.toFloat()) * chartHeight
            drawCircle(Color(0xFF2196F3), radius = 4f, center = Offset(px, py))
        }
    }
}

private fun dayKey(timeMs: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timeMs))
}

@Composable
private fun CategoryTotalsList(totalsFlow: List<CategoryTotal>, totalSpent: Double) {
    if (totalsFlow.isEmpty()) {
        Text("No data for selected range")
    } else {
        val currency = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "ZA")).apply {
            currency = java.util.Currency.getInstance("ZAR")
        }
        LazyColumn(
            modifier = Modifier.heightIn(max = 200.dp)
        ) {
            items(totalsFlow) { row ->
                val percentage = if (totalSpent > 0) (row.total / totalSpent * 100) else 0.0
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = row.categoryName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${String.format("%.1f", percentage)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666)
                        )
                    }
                    Text(
                        text = currency.format(row.total),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}