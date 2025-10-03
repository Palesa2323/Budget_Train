package com.example.budgettrain.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class BudgetGoal(val minimumGoal: Double, val maximumGoal: Double)

enum class BudgetStatus { NO_GOALS, UNDER_MINIMUM, ON_TRACK_LOW, ON_TRACK_HIGH, OVER_BUDGET }

data class CategorySpend(val categoryId: Long, val name: String, val color: Long, val amount: Double)

data class RecentExpense(val date: Date, val amount: Double)

data class DailyPoint(val date: Date, val amount: Double)

data class DashboardState(
    val isLoading: Boolean = true,
    val username: String = "",
    val currentDateFormatted: String = DashboardViewModel.formattedToday(),
    val currentMonthLabel: String = DashboardViewModel.monthYearLabel(),
    val totalSpentThisMonth: Double = 0.0,
    val expenseCountThisMonth: Int = 0,
    val averageExpenseThisMonth: Double = 0.0,
    val daysRemainingInMonth: Int = DashboardViewModel.daysRemainingInMonth(),
    val budgetGoal: BudgetGoal? = null,
    val budgetStatus: BudgetStatus = BudgetStatus.NO_GOALS,
    val progressPercent: Float = 0f,
    val amountRemainingToMax: Double = 0.0,
    val topCategory: CategorySpend? = null,
    val recentExpense: RecentExpense? = null,
    val lastActivityAgo: String = "",
    val dailyTrend: List<DailyPoint> = emptyList(),
    val topCategories: List<CategorySpend> = emptyList(),
    val error: String? = null
)

class DashboardViewModel : ViewModel() {
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state

    init {
        // Simulate initial load; replace with repository calls wired to Room DB
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true)
                delay(600) // shimmer time
                val sampleGoal = BudgetGoal(minimumGoal = 2000.0, maximumGoal = 5000.0)
                val total = 3450.0
                val expenseCount = 23
                val avg = if (expenseCount > 0) total / expenseCount else 0.0
                val status = calculateBudgetStatus(total, sampleGoal)
                val percent = calculateProgressPercentage(total, sampleGoal.maximumGoal)
                val amountRemaining = (sampleGoal.maximumGoal - total).coerceAtLeast(0.0)
                val topCat = CategorySpend(1, "Food", 0xFF4CAF50, 1200.0)
                val recent = RecentExpense(Date(System.currentTimeMillis() - 2 * 60 * 60 * 1000), 95.75)
                val daily = generateSampleDaily(14)
                val topCats = listOf(
                    topCat,
                    CategorySpend(2, "Transport", 0xFF2196F3, 800.0),
                    CategorySpend(3, "Entertainment", 0xFFFF9800, 520.0)
                )
                _state.value = _state.value.copy(
                    isLoading = false,
                    username = "User",
                    totalSpentThisMonth = total,
                    expenseCountThisMonth = expenseCount,
                    averageExpenseThisMonth = avg,
                    budgetGoal = sampleGoal,
                    budgetStatus = status,
                    progressPercent = percent,
                    amountRemainingToMax = amountRemaining,
                    topCategory = topCat,
                    recentExpense = recent,
                    lastActivityAgo = timeAgo(recent.date),
                    dailyTrend = daily,
                    topCategories = topCats
                )
            } catch (t: Throwable) {
                _state.value = _state.value.copy(isLoading = false, error = t.message)
            }
        }
    }

    companion object Calculations {
        fun calculateBudgetStatus(totalSpent: Double, goals: BudgetGoal?): BudgetStatus {
            return when {
                goals == null -> BudgetStatus.NO_GOALS
                totalSpent < goals.minimumGoal -> BudgetStatus.UNDER_MINIMUM
                totalSpent < goals.maximumGoal * 0.8 -> BudgetStatus.ON_TRACK_LOW
                totalSpent <= goals.maximumGoal -> BudgetStatus.ON_TRACK_HIGH
                else -> BudgetStatus.OVER_BUDGET
            }
        }

        fun calculateProgressPercentage(totalSpent: Double, maxGoal: Double): Float {
            if (maxGoal <= 0.0) return 0f
            val pct = (totalSpent / maxGoal) * 100.0
            return pct.coerceIn(0.0, 100.0).toFloat()
        }

        fun daysRemainingInMonth(): Int {
            val calendar = Calendar.getInstance()
            val lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
            return lastDay - currentDay
        }

        fun formattedToday(): String {
            val sdf = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
            return sdf.format(Date())
        }

        fun monthYearLabel(): String {
            val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            return sdf.format(Date())
        }

        private fun timeAgo(date: Date): String {
            val diffMs = System.currentTimeMillis() - date.time
            val minutes = diffMs / (60_000)
            val hours = diffMs / (3_600_000)
            val days = diffMs / (86_400_000)
            return when {
                minutes < 60 -> "$minutes min ago"
                hours < 24 -> "$hours h ago"
                else -> "$days d ago"
            }
        }

        private fun generateSampleDaily(days: Int): List<DailyPoint> {
            val now = Calendar.getInstance()
            return (days - 1 downTo 0).map { offset ->
                val cal = now.clone() as Calendar
                cal.add(Calendar.DAY_OF_YEAR, -offset)
                val amount = (50..400).random().toDouble()
                DailyPoint(cal.time, amount)
            }
        }
    }
}


