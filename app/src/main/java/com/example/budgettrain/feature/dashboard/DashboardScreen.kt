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
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

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
        HeaderSection(username = state.username, date = state.currentDateFormatted, monthLabel = state.currentMonthLabel)
        Spacer(modifier = Modifier.height(12.dp))
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
private fun HeaderSection(username: String, date: String, monthLabel: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Welcome, $username", style = MaterialTheme.typography.titleLarge)
        Text(text = date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(text = monthLabel, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun BudgetOverviewCard(state: DashboardState) {
    val currency = NumberFormat.getCurrencyInstance(Locale("en", "ZA")).apply {
        currency = java.util.Currency.getInstance("ZAR")
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Total Spent", style = MaterialTheme.typography.titleMedium)
            if (state.isLoading) {
                SkeletonBlock(modifier = Modifier.fillMaxWidth().height(36.dp))
                Spacer(Modifier.height(8.dp))
                SkeletonBlock(modifier = Modifier.fillMaxWidth().height(10.dp))
                Spacer(Modifier.height(8.dp))
                SkeletonBlock(modifier = Modifier.fillMaxWidth().height(16.dp))
                return@Column
            } else {
                Text(currency.format(state.totalSpentThisMonth), style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
            }
            val progress = if (state.budgetGoal?.maximumGoal ?: 0.0 > 0.0) state.progressPercent / 100f else 0f
            val animated = animateFloatAsState(targetValue = progress, label = "progress")
            val color = when (state.budgetStatus) {
                BudgetStatus.NO_GOALS -> Color(0xFF2196F3)
                BudgetStatus.UNDER_MINIMUM -> Color(0xFF2196F3)
                BudgetStatus.ON_TRACK_LOW -> Color(0xFF4CAF50)
                BudgetStatus.ON_TRACK_HIGH -> Color(0xFFFF9800)
                BudgetStatus.OVER_BUDGET -> Color(0xFFF44336)
            }
            LinearProgressIndicator(
                progress = { animated.value },
                trackColor = Color.LightGray.copy(alpha = 0.4f),
                color = color,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            Spacer(Modifier.height(8.dp))
            val goalText = state.budgetGoal?.let { goal ->
                "${currency.format(state.totalSpentThisMonth)} of ${currency.format(goal.maximumGoal)} (max goal)"
            } ?: "No budget goals set"
            Text(goalText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun QuickStatsGrid(state: DashboardState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(modifier = Modifier.weight(1f), elevation = CardDefaults.cardElevation(4.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("This Month", style = MaterialTheme.typography.titleSmall)
                if (state.isLoading) {
                    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(14.dp))
                    Spacer(Modifier.height(6.dp))
                    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(14.dp))
                    Spacer(Modifier.height(6.dp))
                    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(14.dp))
                } else {
                    Text("${state.expenseCountThisMonth} expenses")
                    Text("Avg ${formatCurrency(state.averageExpenseThisMonth)}")
                    Text("${state.daysRemainingInMonth} days left")
                }
            }
        }
        Card(modifier = Modifier.weight(1f), elevation = CardDefaults.cardElevation(4.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Budget Status", style = MaterialTheme.typography.titleSmall)
                if (state.isLoading) {
                    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(14.dp))
                    Spacer(Modifier.height(6.dp))
                    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(14.dp))
                    Spacer(Modifier.height(6.dp))
                    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(14.dp))
                } else {
                    Text(statusLabel(state.budgetStatus))
                    val percentText = String.format(Locale.getDefault(), "%.0f%% used", state.progressPercent)
                    Text(percentText)
                    if (state.budgetGoal != null) {
                        Text("${formatCurrency(state.amountRemainingToMax)} until max")
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(modifier = Modifier.weight(1f), elevation = CardDefaults.cardElevation(4.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Top Category", style = MaterialTheme.typography.titleSmall)
                if (state.isLoading) {
                    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(6.dp))
                    Spacer(Modifier.height(6.dp))
                    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(14.dp))
                } else {
                    val top = state.topCategory
                    if (top == null) {
                        Text("No categories yet")
                    } else {
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(top.color.toInt()))
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("${top.name} • ${formatCurrency(top.amount)}")
                    }
                }
            }
        }
        Card(modifier = Modifier.weight(1f), elevation = CardDefaults.cardElevation(4.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Recent Activity", style = MaterialTheme.typography.titleSmall)
                if (state.isLoading) {
                    SkeletonBlock(modifier = Modifier.fillMaxWidth().height(14.dp))
                    } else {
                    val recent = state.recentExpense
                    if (recent == null) {
                        Text("No expenses yet")
                    } else {
                        Text("${formatCurrency(recent.amount)} • ${state.lastActivityAgo}")
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                Text("Spending Trend", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(8.dp))
            if (state.isLoading) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Simple placeholder for line chart area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF5F5F5)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("[Trend Chart]")
                }
            }
        }
    }
}

@Composable
private fun TopCategoriesSection(state: DashboardState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Top Categories", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            if (state.isLoading) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(4) {
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .padding(12.dp)
                        ) {
                            SkeletonBlock(modifier = Modifier.fillMaxWidth().height(6.dp))
                            Spacer(Modifier.height(6.dp))
                            SkeletonBlock(modifier = Modifier.fillMaxWidth().height(14.dp))
                            Spacer(Modifier.height(6.dp))
                            SkeletonBlock(modifier = Modifier.fillMaxWidth().height(6.dp))
                        }
                    }
                }
            } else if (state.topCategories.isEmpty()) {
                Text("No categories yet")
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.topCategories.size) { idx ->
                        val cat = state.topCategories[idx]
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .padding(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(cat.color.toInt()))
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(cat.name)
                            Text(formatCurrency(cat.amount), style = MaterialTheme.typography.bodySmall)
                            Divider(Modifier.padding(vertical = 6.dp))
                            val total = state.totalSpentThisMonth.coerceAtLeast(0.01)
                            val ratio = (cat.amount / total).toFloat().coerceIn(0f, 1f)
                            LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth())
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


