package com.example.budgettrain.feature.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettrain.data.dao.CategoryTotal
import com.example.budgettrain.data.db.DatabaseProvider
import com.example.budgettrain.data.entity.Expense
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

class DashboardViewModel(app: Application) : AndroidViewModel(app) {
    private val db = DatabaseProvider.get(app)
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val (startOfMonth, now) = currentMonthRange()

            // Combine expenses in range, category totals for range, and categories (for colors)
            combine(
                db.expenseDao().getExpensesInRange(startOfMonth, now),
                db.expenseDao().getCategoryTotals(startOfMonth, now),
                db.categoryDao().getAll()
            ) { expenses, totals, categories ->
                Triple(expenses, totals, categories)
            }.collect { (expenses, totals, categories) ->
                val totalSpent = expenses.sumOf { it.amount }
                val expenseCount = expenses.size
                val average = if (expenseCount > 0) totalSpent / expenseCount else 0.0

                // Get budget goals from SharedPreferences for now
                val prefs = getApplication<Application>().getSharedPreferences("budget_goals", android.content.Context.MODE_PRIVATE)
                val minGoal = prefs.getFloat("min_goal", 1000f).toDouble()
                val maxGoal = prefs.getFloat("max_goal", 5000f).toDouble()
                val goal = BudgetGoal(minimumGoal = minGoal, maximumGoal = maxGoal)
                val status = calculateBudgetStatus(totalSpent, goal)
                val percent = calculateProgressPercentage(totalSpent, goal.maximumGoal)
                val amountRemaining = (goal.maximumGoal - totalSpent).coerceAtLeast(0.0)

                val recent = expenses.maxByOrNull { it.date }?.let { e ->
                    RecentExpense(Date(e.date), e.amount)
                }

                val categoryIdToColor = categories.associate { it.id to it.color }

                val topCats: List<CategorySpend> = totals
                    .sortedByDescending { it.total }
                    .take(3)
                    .map { row: CategoryTotal ->
                        val color = categoryIdToColor.entries.firstOrNull { it.key == categoryIdForName(row.categoryName, categories.map { c -> c.id to c.name }) }?.value
                            ?: 0xFF2196F3
                        CategorySpend(
                            categoryId = categoryIdForName(row.categoryName, categories.map { c -> c.id to c.name }) ?: -1L,
                            name = row.categoryName,
                            color = color,
                            amount = row.total
                        )
                    }

                val topCategory = topCats.firstOrNull()

                val daily = buildDailyTrend(expenses, startOfMonth, now)

                _state.value = _state.value.copy(
                    isLoading = false,
                    username = "User", // TODO: Get from user preferences or authentication
                    totalSpentThisMonth = totalSpent,
                    expenseCountThisMonth = expenseCount,
                    averageExpenseThisMonth = average,
                    budgetGoal = if (goal.maximumGoal > 0.0) goal else null,
                    budgetStatus = status,
                    progressPercent = percent,
                    amountRemainingToMax = amountRemaining,
                    topCategory = topCategory,
                    recentExpense = recent,
                    lastActivityAgo = recent?.let { timeAgo(it.date) } ?: "",
                    dailyTrend = daily,
                    topCategories = topCats
                )
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

        private fun currentMonthRange(): Pair<Long, Long> {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            val now = System.currentTimeMillis()
            return start to now
        }

        private fun buildDailyTrend(expenses: List<Expense>, start: Long, end: Long): List<DailyPoint> {
            val cal = Calendar.getInstance()
            cal.timeInMillis = start
            val days = mutableListOf<DailyPoint>()
            val byDay: Map<String, Double> = expenses.groupBy { e -> dayKey(e.date) }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
            while (cal.timeInMillis <= end) {
                val key = dayKey(cal.timeInMillis)
                val amount = byDay[key] ?: 0.0
                days.add(DailyPoint(Date(cal.timeInMillis), amount))
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return days
        }

        private fun dayKey(timeMs: Long): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date(timeMs))
        }

        private fun categoryIdForName(name: String, pairs: List<Pair<Long, String>>): Long? {
            return pairs.firstOrNull { it.second == name }?.first
        }
    }
}


