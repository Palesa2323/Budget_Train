package com.example.budgettrain.feature.reports

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun ReportsScreen(vm: ReportsViewModel = viewModel()) {
    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val cal = remember { Calendar.getInstance() }
    val state by vm.state.collectAsState()
    
    // Use ViewModel's state for date range instead of local state
    val startMillis = state.startMillis
    val endMillis = state.endMillis

    // Automatically load data when screen is first displayed if no data is loaded yet
    LaunchedEffect(Unit) {
        if (!state.hasLoadedData && !state.loading) {
            vm.load()
        }
    }

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
                                    val newStart = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0) }.timeInMillis
                                    vm.setRange(newStart, endMillis)
                                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                            modifier = Modifier.weight(1f)
                        ) { Text("Start: ${sdf.format(startMillis)}", maxLines = 1, overflow = TextOverflow.Ellipsis) }

                        Button(
                            onClick = {
                                val c = Calendar.getInstance().apply { timeInMillis = endMillis }
                                DatePickerDialog(context, { _, y, m, d ->
                                    val newEnd = Calendar.getInstance().apply { set(y, m, d, 23, 59, 59) }.timeInMillis
                                    vm.setRange(startMillis, newEnd)
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

        item {
            Card(
                elevation = CardDefaults.cardElevation(4.dp), 
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Spending by Category", 
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF2196F3)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Visual breakdown of your expenses",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF757575)
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    val totalSpent = state.expenses.sumOf { it.amount }
                    if (totalSpent > 0) {
                        CategoryPieChart(
                            categoryTotals = state.categoryTotals,
                            totalSpent = totalSpent
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text("No spending data to display", color = Color.Gray)
                        }
                    }
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(horizontal = 8.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text("No data to chart", color = Color.Gray)
        }
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
    val minVal = days.minOfOrNull { it.second } ?: 0.0
    val range = maxVal - minVal
    
    // Calculate trend line
    val trendLine = calculateTrendLine(days.map { it.second })
    
    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth().height(220.dp).padding(horizontal = 8.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val leftPadding = 40.dp.toPx() // Space for Y-axis labels
            val rightPadding = 16.dp.toPx()
            val topPadding = 16.dp.toPx()
            val bottomPadding = 30.dp.toPx() // Space for X-axis labels
            
            val chartWidth = width - leftPadding - rightPadding
            val chartHeight = height - topPadding - bottomPadding
            
            val points = days.mapIndexed { index, pair ->
                val x = leftPadding + (index.toFloat() / (days.size - 1)) * chartWidth
                val normalizedAmount = if (range > 0) ((pair.second - minVal) / range).toFloat() else 0.5f
                val y = topPadding + (1f - normalizedAmount) * chartHeight
                androidx.compose.ui.geometry.Offset(x, y)
            }
            
            // Draw grid lines
            val gridColor = Color.Gray.copy(alpha = 0.2f)
            val strokeWidth = 1.dp.toPx()
            
            // Horizontal grid lines
            for (i in 0..4) {
                val y = topPadding + (i / 4f) * chartHeight
                drawLine(
                    color = gridColor,
                    start = androidx.compose.ui.geometry.Offset(leftPadding, y),
                    end = androidx.compose.ui.geometry.Offset(leftPadding + chartWidth, y),
                    strokeWidth = strokeWidth
                )
            }
            
            // Vertical grid lines
            for (i in 0..4) {
                val x = leftPadding + (i / 4f) * chartWidth
                drawLine(
                    color = gridColor,
                    start = androidx.compose.ui.geometry.Offset(x, topPadding),
                    end = androidx.compose.ui.geometry.Offset(x, topPadding + chartHeight),
                    strokeWidth = strokeWidth
                )
            }
            
            // Draw trend line
            if (trendLine != null) {
                val trendStart = androidx.compose.ui.geometry.Offset(
                    leftPadding,
                    topPadding + (1f - ((trendLine.first - minVal) / range).toFloat()) * chartHeight
                )
                val trendEnd = androidx.compose.ui.geometry.Offset(
                    leftPadding + chartWidth,
                    topPadding + (1f - ((trendLine.second - minVal) / range).toFloat()) * chartHeight
                )
                
                drawLine(
                    color = Color(0xFFFF5722).copy(alpha = 0.7f),
                    start = trendStart,
                    end = trendEnd,
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
                )
            }
            
            // Draw area under the curve with gradient
            if (points.size > 1) {
                val path = Path()
                path.moveTo(points[0].x, topPadding + chartHeight)
                path.lineTo(points[0].x, points[0].y)
                
                for (i in 1 until points.size) {
                    path.lineTo(points[i].x, points[i].y)
                }
                
                path.lineTo(points.last().x, topPadding + chartHeight)
                path.close()
                
                // Create gradient for area fill
                val gradient = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2196F3).copy(alpha = 0.3f),
                        Color(0xFF2196F3).copy(alpha = 0.1f)
                    ),
                    startY = topPadding,
                    endY = topPadding + chartHeight
                )
                
                drawPath(path = path, brush = gradient)
            }
            
            // Draw the main line
            if (points.size > 1) {
                val path = Path()
                path.moveTo(points[0].x, points[0].y)
                
                for (i in 1 until points.size) {
                    path.lineTo(points[i].x, points[i].y)
                }
                
                drawPath(
                    path = path,
                    color = Color(0xFF1976D2),
                    style = Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
            
            // Draw data points with enhanced styling
            points.forEachIndexed { index, point ->
                // Outer circle
                drawCircle(
                    color = Color.White,
                    radius = 6.dp.toPx(),
                    center = point
                )
                // Inner circle
                drawCircle(
                    color = Color(0xFF1976D2),
                    radius = 4.dp.toPx(),
                    center = point
                )
            }
        }
        
        // Y-axis labels
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(35.dp)
                .padding(top = 16.dp, bottom = 30.dp)
        ) {
            for (i in 0..4) {
                val value = minVal + (range * (4 - i) / 4)
                val formattedValue = if (value >= 1000) {
                    "R${String.format("%.1f", value / 1000)}k"
                } else {
                    "R${String.format("%.0f", value)}"
                }
                
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = androidx.compose.ui.Alignment.CenterEnd
                ) {
                    Text(
                        text = formattedValue,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            }
        }
        
        // X-axis labels (dates)
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .padding(start = 40.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            days.forEachIndexed { index, pair ->
                if (index % maxOf(1, days.size / 5) == 0 || index == days.size - 1) {
                    Text(
                        text = sdf.format(java.util.Date(pair.first)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            }
        }
        
        // Add trend indicator
        if (trendLine != null) {
            val trendDirection = if (trendLine.second > trendLine.first) "↗" else if (trendLine.second < trendLine.first) "↘" else "→"
            val trendColor = if (trendLine.second > trendLine.first) Color(0xFFF44336) else Color(0xFF4CAF50)
            
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.TopEnd)
                    .padding(8.dp)
                    .background(
                        Color.White.copy(alpha = 0.9f),
                        androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(
                        text = trendDirection,
                        color = trendColor,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Trend",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

private fun calculateTrendLine(data: List<Double>): Pair<Double, Double>? {
    if (data.size < 2) return null
    
    // Simple linear regression to calculate trend line
    val n = data.size
    val sumX = (0 until n).sum().toDouble()
    val sumY = data.sum()
    val sumXY = data.mapIndexed { index, value -> index * value }.sum()
    val sumXX = (0 until n).sumOf { it * it }.toDouble()
    
    val slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX)
    val intercept = (sumY - slope * sumX) / n
    
    return Pair(intercept, intercept + slope * (n - 1))
}

private fun dayKey(timeMs: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timeMs))
}

@Composable
private fun CategoryPieChart(
    categoryTotals: List<CategoryTotal>,
    totalSpent: Double,
    modifier: Modifier = Modifier
) {
    if (categoryTotals.isEmpty() || totalSpent <= 0) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text("No category data to display", color = Color.Gray)
        }
        return
    }
    
    val colors = listOf(
        Color(0xFF2196F3), // Blue
        Color(0xFF4CAF50), // Green
        Color(0xFFFF9800), // Orange
        Color(0xFFF44336), // Red
        Color(0xFF9C27B0), // Purple
        Color(0xFF00BCD4), // Cyan
        Color(0xFFFFEB3B), // Yellow
        Color(0xFF795548)  // Brown
    )
    
    Box(modifier = modifier.fillMaxWidth().height(250.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val radius = minOf(centerX, centerY) * 0.6f
            val startAngle = -90f // Start from top
            
            var currentAngle = startAngle
            
            categoryTotals.forEachIndexed { index, category ->
                val percentage = category.total / totalSpent
                val sweepAngle = (percentage * 360f).toFloat()
                
                if (sweepAngle > 0.5f) { // Only draw segments larger than 0.5 degrees
                    val color = colors[index % colors.size]
                    
                    // Draw pie slice
                    val path = Path()
                    path.moveTo(centerX, centerY)
                    path.arcTo(
                        rect = androidx.compose.ui.geometry.Rect(
                            centerX - radius,
                            centerY - radius,
                            centerX + radius,
                            centerY + radius
                        ),
                        startAngleDegrees = currentAngle,
                        sweepAngleDegrees = sweepAngle,
                        forceMoveTo = false
                    )
                    path.close()
                    
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    
                    // Fill the slice
                    drawPath(
                        path = path,
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color.copy(alpha = 0.8f),
                                color.copy(alpha = 0.6f)
                            ),
                            center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                            radius = radius
                        )
                    )
                    
                    currentAngle += sweepAngle
                }
            }
        }
        
        // Legend
        Column(
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.CenterEnd)
                .padding(16.dp)
                .width(120.dp)
        ) {
            categoryTotals.forEachIndexed { index, category ->
                if (category.total > 0) {
                    val percentage = (category.total / totalSpent * 100).toInt()
                    val color = colors[index % colors.size]
                    val currency = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "ZA")).apply {
                        currency = java.util.Currency.getInstance("ZAR")
                    }
                    
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(color, androidx.compose.foundation.shape.CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = category.categoryName,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${percentage}% • ${currency.format(category.total)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
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