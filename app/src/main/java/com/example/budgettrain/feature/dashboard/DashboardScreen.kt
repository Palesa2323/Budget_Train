package com.example.budgettrain.feature.dashboard

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onNavigateToAddExpense: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onNavigateToGoals: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    
    // Refresh data when the screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        BrandHeader()
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Budget Goals", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))
        TipsCard(state = state)
        Spacer(modifier = Modifier.height(16.dp))
        HeaderSection(
            username = state.username, 
            date = state.currentDateFormatted, 
            monthLabel = state.currentMonthLabel,
            onRefresh = { viewModel.loadData() }
        )
        Spacer(modifier = Modifier.height(16.dp))
        QuickActionButtons(
            onAddExpense = onNavigateToAddExpense,
            onViewReports = onNavigateToReports,
            onSetGoals = onNavigateToGoals
        )
        Spacer(modifier = Modifier.height(16.dp))
        BudgetOverviewCard(state)
        Spacer(modifier = Modifier.height(12.dp))
        QuickStatsGrid(state)
        Spacer(modifier = Modifier.height(12.dp))
        TrendSection(state)
        Spacer(modifier = Modifier.height(12.dp))
        TopCategoriesSection(state)
    }
}

@Composable
private fun HeaderSection(
    username: String, 
    date: String, 
    monthLabel: String,
    onRefresh: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Welcome, $username", style = MaterialTheme.typography.titleLarge)
            Text(text = date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text(text = monthLabel, style = MaterialTheme.typography.bodyMedium)
        }
        IconButton(onClick = onRefresh) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = "Refresh",
                tint = Color(0xFF2196F3)
            )
        }
    }
}

@Composable
private fun QuickActionButtons(
    onAddExpense: () -> Unit = {},
    onViewReports: () -> Unit = {},
    onSetGoals: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Add Expense Button
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable { onAddExpense() },
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add Expense",
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Add Expense",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF1976D2)
                )
            }
        }
        
        // View Reports Button
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable { onViewReports() },
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.TrendingUp,
                    contentDescription = "View Reports",
                    tint = Color(0xFF9C27B0),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "View Reports",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7B1FA2)
                )
            }
        }
        
        // Budget Goals Button
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable { onSetGoals() },
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Budget Goals",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Set Goals",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF388E3C)
                )
            }
        }
    }
}

@Composable
private fun BudgetOverviewCard(state: DashboardState) {
    val currency = NumberFormat.getCurrencyInstance(Locale("en", "ZA")).apply {
        currency = java.util.Currency.getInstance("ZAR")
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total Spent", style = MaterialTheme.typography.titleMedium)
                if (!state.isLoading) {
                    Text(
                        text = getBudgetStatusIcon(state.budgetStatus),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
            
            if (state.isLoading) {
                SkeletonBlock(modifier = Modifier.fillMaxWidth().height(36.dp))
                Spacer(Modifier.height(16.dp))
                SkeletonBlock(modifier = Modifier.fillMaxWidth().height(10.dp))
                Spacer(Modifier.height(8.dp))
                SkeletonBlock(modifier = Modifier.fillMaxWidth().height(16.dp))
                return@Column
            } else {
                Text(
                    text = currency.format(state.totalSpentThisMonth), 
                    style = MaterialTheme.typography.headlineLarge,
                    color = getBudgetStatusColor(state.budgetStatus)
                )
                Spacer(Modifier.height(16.dp))
            }
            
            val progress = if (state.budgetGoal?.maximumGoal ?: 0.0 > 0.0) state.progressPercent / 100f else 0f
            val animated = animateFloatAsState(targetValue = progress, label = "progress")
            val color = getBudgetStatusColor(state.budgetStatus)
            
            // Enhanced progress indicator
            LinearProgressIndicator(
                progress = { animated.value },
                trackColor = Color.LightGray.copy(alpha = 0.3f),
                color = color,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val goalText = state.budgetGoal?.let { goal ->
                    "${currency.format(state.totalSpentThisMonth)} of ${currency.format(goal.maximumGoal)}"
                } ?: "No budget goals set"
                Text(goalText, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                
                if (state.budgetGoal != null) {
                    Text(
                        text = "${String.format("%.0f", state.progressPercent)}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = color
                    )
                }
            }
            
            if (state.budgetGoal != null && state.amountRemainingToMax > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "R${String.format("%.2f", state.amountRemainingToMax)} remaining until max budget",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun QuickStatsGrid(state: DashboardState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.weight(1f), 
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("All Expenses", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("📊", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (state.isLoading) {
                    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(14.dp))
                    Spacer(Modifier.height(6.dp))
                    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(14.dp))
                    Spacer(Modifier.height(6.dp))
                    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(14.dp))
                } else {
                    Text("${state.expenseCountThisMonth} expenses", style = MaterialTheme.typography.bodyMedium)
                    Text("Avg ${formatCurrency(state.averageExpenseThisMonth)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text("All time total", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
        Card(
            modifier = Modifier.weight(1f), 
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = getBudgetStatusColor(state.budgetStatus).copy(alpha = 0.1f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Budget Status", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(getBudgetStatusIcon(state.budgetStatus), style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (state.isLoading) {
                    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(14.dp))
                    Spacer(Modifier.height(6.dp))
                    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(14.dp))
                    Spacer(Modifier.height(6.dp))
                    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(14.dp))
                } else {
                    Text(
                        statusLabel(state.budgetStatus), 
                        style = MaterialTheme.typography.bodyMedium,
                        color = getBudgetStatusColor(state.budgetStatus)
                    )
                    val percentText = String.format(Locale.getDefault(), "%.0f%% used", state.progressPercent)
                    Text(percentText, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    if (state.budgetGoal != null) {
                        Text("${formatCurrency(state.amountRemainingToMax)} until max", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.weight(1f), 
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F8FF))
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Top Category", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("🏆", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (state.isLoading) {
                    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(6.dp))
                    Spacer(Modifier.height(6.dp))
                    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(14.dp))
                } else {
                    val top = state.topCategory
                    if (top == null) {
                        Text("No categories yet", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    } else {
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(top.color.toInt()))
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("${top.name}", style = MaterialTheme.typography.bodyMedium)
                        Text(formatCurrency(top.amount), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
        Card(
            modifier = Modifier.weight(1f), 
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Recent Activity", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("⏰", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (state.isLoading) {
                    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(14.dp))
                    } else {
                    val recent = state.recentExpense
                    if (recent == null) {
                        Text("No expenses yet", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    } else {
                        Text(formatCurrency(recent.amount), style = MaterialTheme.typography.bodyMedium)
                        Text(state.lastActivityAgo, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendSection(state: DashboardState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween, 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Spending Trend", style = MaterialTheme.typography.titleSmall)
                if (!state.isLoading && state.dailyTrend.isNotEmpty()) {
                    val trendDirection = calculateTrendDirection(state.dailyTrend)
                    val trendColor = when {
                        trendDirection > 0.1f -> Color(0xFF4CAF50) // Green for decreasing spending
                        trendDirection < -0.1f -> Color(0xFFF44336) // Red for increasing spending
                        else -> Color(0xFFFF9800) // Orange for stable
                    }
                    val trendIcon = when {
                        trendDirection > 0.1f -> "↓"
                        trendDirection < -0.1f -> "↑"
                        else -> "→"
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = trendIcon,
                            color = trendColor,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${String.format("%.1f", kotlin.math.abs(trendDirection) * 100f)}%",
                            color = trendColor,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (state.isLoading) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.dailyTrend.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF5F5F5)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No spending data yet", color = Color.Gray)
                }
            } else {
                // Simple line chart implementation
                SimpleLineChart(
                    data = state.dailyTrend,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )
            }
        }
    }
}

@Composable
private fun TopCategoriesSection(state: DashboardState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Top Categories", style = MaterialTheme.typography.titleSmall)
                if (!state.isLoading && state.topCategories.isNotEmpty()) {
                    Text("🏆", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(12.dp))
            if (state.isLoading) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(4) {
                        Card(
                            modifier = Modifier.width(120.dp),
                            elevation = CardDefaults.cardElevation(2.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                SkeletonBlock(modifier = Modifier.fillMaxWidth().height(6.dp))
                                Spacer(Modifier.height(6.dp))
                                SkeletonBlock(modifier = Modifier.fillMaxWidth().height(14.dp))
                                Spacer(Modifier.height(6.dp))
                                SkeletonBlock(modifier = Modifier.fillMaxWidth().height(6.dp))
                            }
                        }
                    }
                }
            } else if (state.topCategories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No categories yet", color = Color.Gray)
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.topCategories.size) { idx ->
                        val cat = state.topCategories[idx]
                        val total = state.totalSpentThisMonth.coerceAtLeast(0.01)
                        val percentage = ((cat.amount / total) * 100f).toFloat()
                        
                        Card(
                            modifier = Modifier
                                .width(120.dp)
                                .clickable { /* Navigate to category details */ },
                            elevation = CardDefaults.cardElevation(2.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                // Category color indicator
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color(cat.color.toInt()))
                                )
                                Spacer(Modifier.height(8.dp))
                                
                                // Category name
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                
                                // Amount
                                Text(
                                    text = formatCurrency(cat.amount),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFF2196F3)
                                )
                                
                                Spacer(Modifier.height(4.dp))
                                
                                // Percentage
                                Text(
                                    text = "${String.format("%.1f", percentage)}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                
                                Spacer(Modifier.height(8.dp))
                                
                                // Progress bar
                                LinearProgressIndicator(
                                    progress = { percentage / 100f },
                                    trackColor = Color.LightGray.copy(alpha = 0.3f),
                                    color = Color(cat.color.toInt()).copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
// Removed extra navigation buttons; navigation is available via bottom bar

private fun formatCurrency(value: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "ZA")).apply {
    currency = java.util.Currency.getInstance("ZAR")
}.format(value)

private fun formatCurrencyNonComposable(value: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "ZA")).apply {
    currency = java.util.Currency.getInstance("ZAR")
}.format(value)

private fun statusLabel(status: BudgetStatus): String = when (status) {
    BudgetStatus.NO_GOALS -> "No Goals Set"
    BudgetStatus.UNDER_MINIMUM -> "Under Minimum"
    BudgetStatus.ON_TRACK_LOW -> "On Track"
    BudgetStatus.ON_TRACK_HIGH -> "On Track"
    BudgetStatus.OVER_BUDGET -> "Over Budget"
}

private fun getBudgetStatusColor(status: BudgetStatus): Color = when (status) {
    BudgetStatus.NO_GOALS -> Color(0xFF2196F3)
    BudgetStatus.UNDER_MINIMUM -> Color(0xFF2196F3)
    BudgetStatus.ON_TRACK_LOW -> Color(0xFF4CAF50)
    BudgetStatus.ON_TRACK_HIGH -> Color(0xFFFF9800)
    BudgetStatus.OVER_BUDGET -> Color(0xFFF44336)
}

private fun getBudgetStatusIcon(status: BudgetStatus): String = when (status) {
    BudgetStatus.NO_GOALS -> "🎯"
    BudgetStatus.UNDER_MINIMUM -> "📉"
    BudgetStatus.ON_TRACK_LOW -> "✅"
    BudgetStatus.ON_TRACK_HIGH -> "⚠️"
    BudgetStatus.OVER_BUDGET -> "🚨"
}

@Composable
private fun SkeletonBlock(modifier: Modifier) {
    val infinite = rememberInfiniteTransition(label = "skeleton")
    val alpha by infinite.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
        label = "alpha"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.LightGray.copy(alpha = alpha))
    )
}

@Composable
fun BrandHeader() {
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
private fun TipsCard(state: DashboardState) {
    val tips = generatePersonalizedTips(state)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("💡 Smart Tips", style = MaterialTheme.typography.titleMedium)
                Text("✨", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            
            // Main tip
            Text(
                text = tips.mainTip,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF455A64),
                lineHeight = 20.sp
            )
            
            Spacer(Modifier.height(16.dp))
            
            // Budget status tip
            BudgetStatusTip(state = state)
            
            if (tips.additionalTips.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Divider(color = Color(0xFF90CAF9), modifier = Modifier.padding(vertical = 8.dp))
                Spacer(Modifier.height(8.dp))
                
                // Additional tips
                tips.additionalTips.forEach { tip ->
                    TipRow(
                        title = tip.title,
                        value = tip.value,
                        icon = tip.icon
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun TipRow(title: String, value: String, icon: String = "💡") {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(icon, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = Color(0xFF607D8B))
            Text(value, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF263238))
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Color(0xFF37474F))
    }
}

@Composable
private fun SimpleLineChart(
    data: List<DailyPoint>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    
    val maxAmount = data.maxOfOrNull { it.amount } ?: 1.0
    val minAmount = data.minOfOrNull { it.amount } ?: 0.0
    val range = maxAmount - minAmount
    
    // Calculate trend line
    val trendLine = calculateTrendLine(data)
    
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val leftPadding = 40.dp.toPx() // Space for Y-axis labels
            val rightPadding = 16.dp.toPx()
            val topPadding = 16.dp.toPx()
            val bottomPadding = 30.dp.toPx() // Space for X-axis labels
            
            val chartWidth = width - leftPadding - rightPadding
            val chartHeight = height - topPadding - bottomPadding
            
            val points = data.mapIndexed { index, point ->
                val x = leftPadding + (index.toFloat() / (data.size - 1)) * chartWidth
                val normalizedAmount = if (range > 0) ((point.amount - minAmount) / range).toFloat() else 0.5f
                val y = topPadding + (1f - normalizedAmount) * chartHeight
                androidx.compose.ui.geometry.Offset(x, y)
            }
            
            // Draw grid lines
            val gridColor = Color.Gray.copy(alpha = 0.2f)
            val strokeWidth = 1.dp.toPx()
            
            // Horizontal grid lines with Y-axis labels
            for (i in 0..4) {
                val y = topPadding + (i / 4f) * chartHeight
                val value = minAmount + (range * (4 - i) / 4)
                
                // Draw grid line
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
                    topPadding + (1f - ((trendLine.first - minAmount) / range).toFloat()) * chartHeight
                )
                val trendEnd = androidx.compose.ui.geometry.Offset(
                    leftPadding + chartWidth,
                    topPadding + (1f - ((trendLine.second - minAmount) / range).toFloat()) * chartHeight
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
                val gradient = Brush.verticalGradient(
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
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
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
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(35.dp)
                .padding(top = 16.dp, bottom = 30.dp)
        ) {
            for (i in 0..4) {
                val value = minAmount + (range * (4 - i) / 4)
                val formattedValue = if (value >= 1000) {
                    "$${String.format("%.1f", value / 1000)}k"
                } else {
                    "$${String.format("%.0f", value)}"
                }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .padding(start = 40.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.forEachIndexed { index, point ->
                if (index % maxOf(1, data.size / 5) == 0 || index == data.size - 1) {
                    val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
                    Text(
                        text = dateFormat.format(point.date),
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
            
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(
                        Color.White.copy(alpha = 0.9f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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

private fun calculateTrendLine(data: List<DailyPoint>): Pair<Double, Double>? {
    if (data.size < 2) return null
    
    // Simple linear regression to calculate trend line
    val n = data.size
    val sumX = (0 until n).sum().toDouble()
    val sumY = data.sumOf { it.amount }
    val sumXY = data.mapIndexed { index, point -> index * point.amount }.sum()
    val sumXX = (0 until n).sumOf { it * it }.toDouble()
    
    val slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX)
    val intercept = (sumY - slope * sumX) / n
    
    return Pair(intercept, intercept + slope * (n - 1))
}

private fun calculateTrendDirection(dailyTrend: List<DailyPoint>): Float {
    if (dailyTrend.size < 2) return 0f
    
    val firstHalf = dailyTrend.take(dailyTrend.size / 2)
    val secondHalf = dailyTrend.drop(dailyTrend.size / 2)
    
    val firstHalfAvg = firstHalf.map { it.amount }.average()
    val secondHalfAvg = secondHalf.map { it.amount }.average()
    
    return if (firstHalfAvg > 0.0) {
        val change = secondHalfAvg - firstHalfAvg
        val percentage = change / firstHalfAvg
        percentage.toFloat()
    } else 0f
}

// Data classes for tips
data class TipInfo(val title: String, val value: String, val icon: String = "💡", val priority: Int = 1)
data class PersonalizedTips(val mainTip: String, val additionalTips: List<TipInfo> = emptyList())

// Enhanced tip categories for better organization
enum class TipCategory {
    BUDGET_STATUS, SPENDING_PATTERNS, PREDICTIONS, CATEGORY_INSIGHTS, 
    BEHAVIORAL, GOALS, SAVINGS, ALERTS
}

// Generate personalized tips based on user's spending patterns
@Composable
private fun generatePersonalizedTips(state: DashboardState): PersonalizedTips {
    val tips = mutableListOf<TipInfo>()
    
    // 1. BUDGET STATUS TIPS (Main tip)
    val mainTip = generateBudgetStatusTip(state)
    
    // 2. PREDICTIVE ANALYTICS TIPS
    tips.addAll(generatePredictiveTips(state))
    
    // 3. SPENDING PATTERN TIPS
    tips.addAll(generateSpendingPatternTips(state))
    
    // 4. CATEGORY INTELLIGENCE TIPS
    tips.addAll(generateCategoryTips(state))
    
    // 5. BEHAVIORAL INSIGHTS TIPS
    tips.addAll(generateBehavioralTips(state))
    
    // 6. SAVINGS OPPORTUNITY TIPS
    tips.addAll(generateSavingsTips(state))
    
    // 7. ALERT TIPS
    tips.addAll(generateAlertTips(state))
    
    // Sort tips by priority and limit to most important ones
    val sortedTips = tips.sortedBy { it.priority }.take(6)
    
    return PersonalizedTips(mainTip, sortedTips)
}

// 1. Budget Status Intelligence
private fun generateBudgetStatusTip(state: DashboardState): String {
    return when (state.budgetStatus) {
        BudgetStatus.NO_GOALS -> "🎯 Set up your budget goals to start tracking your spending effectively. Even small goals can make a big difference!"
        BudgetStatus.UNDER_MINIMUM -> "🎉 Great job staying under budget! Consider increasing your savings rate or investing the extra money."
        BudgetStatus.ON_TRACK_LOW -> "👍 You're doing well with your budget! Keep up the good work and consider setting higher savings goals."
        BudgetStatus.ON_TRACK_HIGH -> "⚠️ You're approaching your budget limit. Try to reduce discretionary spending for the rest of the month."
        BudgetStatus.OVER_BUDGET -> "🚨 You've exceeded your budget. Review your recent expenses and cut back on non-essential spending."
    }
}

// 2. Predictive Analytics
private fun generatePredictiveTips(state: DashboardState): List<TipInfo> {
    val tips = mutableListOf<TipInfo>()
    
    if (state.budgetGoal != null) {
        val daysInMonth = 30 // Approximate
        val daysPassed = daysInMonth - state.daysRemainingInMonth
        val dailyAverage = if (daysPassed > 0) state.totalSpentThisMonth / daysPassed else 0.0
        val projectedMonthly = dailyAverage * daysInMonth
        val budgetRemaining = state.budgetGoal.maximumGoal - state.totalSpentThisMonth
        
        // Budget projection
        if (projectedMonthly > state.budgetGoal.maximumGoal) {
            val overage = projectedMonthly - state.budgetGoal.maximumGoal
            tips.add(TipInfo(
                "Budget Alert", 
                "At current pace, you'll exceed budget by ${formatCurrencyNonComposable(overage)}", 
                "📈", 1
            ))
        } else if (projectedMonthly < state.budgetGoal.maximumGoal * 0.8) {
            val savings = state.budgetGoal.maximumGoal - projectedMonthly
            tips.add(TipInfo(
                "Savings Opportunity", 
                "You could save ${formatCurrencyNonComposable(savings)} this month", 
                "💰", 2
            ))
        }
        
        // Daily spending limit
        val dailyLimit = budgetRemaining / maxOf(state.daysRemainingInMonth, 1)
        if (dailyLimit > 0) {
            tips.add(TipInfo(
                "Daily Limit", 
                "Spend max ${formatCurrencyNonComposable(dailyLimit)} per day to stay on track", 
                "📅", 3
            ))
        }
    }
    
    return tips
}

// 3. Spending Pattern Analysis
private fun generateSpendingPatternTips(state: DashboardState): List<TipInfo> {
    val tips = mutableListOf<TipInfo>()
    
    // Expense frequency analysis
    when {
        state.expenseCountThisMonth == 0 -> {
            tips.add(TipInfo("Start Tracking", "Begin logging expenses to build better financial habits", "📝", 1))
        }
        state.expenseCountThisMonth < 3 -> {
            tips.add(TipInfo("Track More", "Log all expenses for better insights into spending patterns", "📊", 2))
        }
        state.expenseCountThisMonth > 20 -> {
            tips.add(TipInfo("Frequent Spending", "Consider consolidating small purchases to reduce transaction fees", "🔄", 3))
        }
    }
    
    // Average expense analysis
    when {
        state.averageExpenseThisMonth > 1000 -> {
            tips.add(TipInfo("Large Purchases", "Review if large expenses align with your financial goals", "🔍", 2))
        }
        state.averageExpenseThisMonth < 50 -> {
            tips.add(TipInfo("Small Purchases", "Track small expenses - they add up quickly!", "🔍", 3))
        }
    }
    
    // Spending trend analysis
    if (state.dailyTrend.size > 7) {
        val recentTrend = calculateTrendDirection(state.dailyTrend)
        when {
            recentTrend > 0.2f -> {
                tips.add(TipInfo("Spending Increase", "Your spending is trending upward - review recent expenses", "📈", 1))
            }
            recentTrend < -0.2f -> {
                tips.add(TipInfo("Spending Decrease", "Great job reducing your spending! Keep it up", "📉", 2))
            }
        }
    }
    
    return tips
}

// 4. Category Intelligence
private fun generateCategoryTips(state: DashboardState): List<TipInfo> {
    val tips = mutableListOf<TipInfo>()
    
    if (state.topCategories.isNotEmpty()) {
        val topCategory = state.topCategories.first()
        val totalSpent = state.totalSpentThisMonth
        val categoryPercentage = (topCategory.amount / totalSpent * 100).toInt()
        
        when {
            categoryPercentage > 60 -> {
                tips.add(TipInfo(
                    "Category Focus", 
                    "${topCategory.name} is ${categoryPercentage}% of spending - consider diversifying", 
                    "🎯", 1
                ))
            }
            categoryPercentage > 40 -> {
                tips.add(TipInfo(
                    "Top Category", 
                    "${topCategory.name} (${formatCurrencyNonComposable(topCategory.amount)}) is your biggest expense", 
                    "🏆", 2
                ))
            }
        }
        
        // Category balance analysis
        if (state.topCategories.size >= 3) {
            val top3Total = state.topCategories.take(3).sumOf { it.amount }
            val top3Percentage = (top3Total / totalSpent * 100).toInt()
            
            if (top3Percentage > 90) {
                tips.add(TipInfo(
                    "Category Balance", 
                    "Top 3 categories are ${top3Percentage}% of spending - consider spreading out", 
                    "⚖️", 3
                ))
            }
        }
    }
    
    return tips
}

// 5. Behavioral Insights
private fun generateBehavioralTips(state: DashboardState): List<TipInfo> {
    val tips = mutableListOf<TipInfo>()
    
    // Spending consistency
    if (state.expenseCountThisMonth > 5) {
        val daysWithExpenses = state.dailyTrend.count { it.amount > 0 }
        val consistency = daysWithExpenses.toFloat() / minOf(state.dailyTrend.size, 30)
        
        when {
            consistency > 0.8f -> {
                tips.add(TipInfo("Consistent Spender", "You spend almost daily - consider weekly budgeting", "📅", 2))
            }
            consistency < 0.3f -> {
                tips.add(TipInfo("Occasional Spender", "You spend infrequently - great for impulse control!", "🎯", 3))
            }
        }
    }
    
    // Recent activity analysis
    if (state.recentExpense != null) {
        val daysSinceLastExpense = (System.currentTimeMillis() - state.recentExpense.date.time) / (1000 * 60 * 60 * 24)
        when {
            daysSinceLastExpense > 7 -> {
                tips.add(TipInfo("No Recent Activity", "No expenses logged in ${daysSinceLastExpense} days", "⏰", 2))
            }
            daysSinceLastExpense < 1 -> {
                tips.add(TipInfo("Active Spender", "You logged an expense today - keep tracking!", "✅", 3))
            }
        }
    }
    
    return tips
}

// 6. Savings Opportunities
private fun generateSavingsTips(state: DashboardState): List<TipInfo> {
    val tips = mutableListOf<TipInfo>()
    
    if (state.budgetGoal != null) {
        val remaining = state.amountRemainingToMax
        val savingsRate = if (state.budgetGoal.maximumGoal > 0) {
            (remaining / state.budgetGoal.maximumGoal * 100).toInt()
        } else 0
        
        when {
            savingsRate > 30 -> {
                tips.add(TipInfo(
                    "High Savings Rate", 
                    "You're saving ${savingsRate}% of your budget - excellent work!", 
                    "💎", 2
                ))
            }
            savingsRate > 10 -> {
                tips.add(TipInfo(
                    "Good Savings", 
                    "You're saving ${savingsRate}% of your budget - keep it up!", 
                    "👍", 3
                ))
            }
            remaining > 0 -> {
                tips.add(TipInfo(
                    "Savings Potential", 
                    "You have ${formatCurrencyNonComposable(remaining)} left to save this month", 
                    "💰", 2
                ))
            }
        }
    }
    
    return tips
}

// 7. Alert Tips
private fun generateAlertTips(state: DashboardState): List<TipInfo> {
    val tips = mutableListOf<TipInfo>()
    
    // Budget alerts
    if (state.budgetGoal != null) {
        val progressPercent = state.progressPercent
        when {
            progressPercent > 100 -> {
                tips.add(TipInfo("Over Budget", "You've exceeded your budget by ${(progressPercent - 100).toInt()}%", "🚨", 1))
            }
            progressPercent > 90 -> {
                tips.add(TipInfo("Budget Warning", "You've used ${progressPercent.toInt()}% of your budget", "⚠️", 1))
            }
        }
    }
    
    // No recent activity alert
    if (state.expenseCountThisMonth == 0 && state.dailyTrend.isNotEmpty()) {
        tips.add(TipInfo("No Activity", "No expenses logged this month - start tracking!", "📝", 1))
    }
    
    return tips
}

@Composable
private fun BudgetStatusTip(state: DashboardState) {
    val (statusText, statusColor, statusIcon) = when (state.budgetStatus) {
        BudgetStatus.NO_GOALS -> Triple("Set Budget Goals", Color(0xFF2196F3), "🎯")
        BudgetStatus.UNDER_MINIMUM -> Triple("Under Budget", Color(0xFF4CAF50), "✅")
        BudgetStatus.ON_TRACK_LOW -> Triple("On Track", Color(0xFF4CAF50), "👍")
        BudgetStatus.ON_TRACK_HIGH -> Triple("Approaching Limit", Color(0xFFFF9800), "⚠️")
        BudgetStatus.OVER_BUDGET -> Triple("Over Budget", Color(0xFFF44336), "🚨")
    }
    
    Card(
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(statusIcon, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                if (state.budgetGoal != null) {
                    Text(
                        text = "${String.format("%.0f", state.progressPercent)}% of budget used",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666)
                    )
                }
            }
        }
    }
}


