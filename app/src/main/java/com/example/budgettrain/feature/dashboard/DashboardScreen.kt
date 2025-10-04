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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
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
        TipsCard()
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
private fun TipsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Budgeting Tips", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Pay yourself first – set aside savings for your goals before spending on anything else. Even a small amount saved consistently builds up over time.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF455A64)
            )
            Spacer(Modifier.height(16.dp))
            TipRow(title = "August Monthly Budget", value = "R3000.00 Total Spend Allowed")
            Divider(color = Color(0xFF90CAF9), modifier = Modifier.padding(vertical = 12.dp))
            TipRow(title = "August Saving Goal", value = "Save R2000.00 in\nFixed Deposit")
            Spacer(Modifier.height(16.dp))
            // Button removed; use bottom navigation to manage goals
        }
    }
}

@Composable
private fun TipRow(title: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.Person, contentDescription = null, tint = Color(0xFF37474F))
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
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padding = 16.dp.toPx()
        
        val chartWidth = width - 2 * padding
        val chartHeight = height - 2 * padding
        
        val points = data.mapIndexed { index, point ->
            val x = padding + (index.toFloat() / (data.size - 1)) * chartWidth
            val normalizedAmount = if (range > 0) ((point.amount - minAmount) / range).toFloat() else 0.5f
            val y = padding + (1f - normalizedAmount) * chartHeight
            androidx.compose.ui.geometry.Offset(x, y)
        }
        
        // Draw grid lines
        val gridColor = Color.Gray.copy(alpha = 0.3f)
        val strokeWidth = 1.dp.toPx()
        
        // Horizontal grid lines
        for (i in 0..4) {
            val y = padding + (i / 4f) * chartHeight
            drawLine(
                color = gridColor,
                start = androidx.compose.ui.geometry.Offset(padding, y),
                end = androidx.compose.ui.geometry.Offset(padding + chartWidth, y),
                strokeWidth = strokeWidth
            )
        }
        
        // Draw the line
        if (points.size > 1) {
            val path = Path()
            path.moveTo(points[0].x, points[0].y)
            
            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
            }
            
            drawPath(
                path = path,
                color = Color(0xFF2196F3),
                style = Stroke(width = 3.dp.toPx())
            )
        }
        
        // Draw data points
        points.forEach { point ->
            drawCircle(
                color = Color(0xFF2196F3),
                radius = 4.dp.toPx(),
                center = point
            )
        }
    }
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


