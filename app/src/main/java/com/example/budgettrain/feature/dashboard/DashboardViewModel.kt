package com.example.budgettrain.feature.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgettrain.data.repository.FirebaseRepository
import com.example.budgettrain.data.entity.Expense
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

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
    private val authRepository = com.example.budgettrain.data.repository.FirebaseAuthRepository()
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            // Load all expenses instead of just current month
            // We'll still calculate current month stats separately
            val (startOfMonth, now) = currentMonthRange()
            val userId = authRepository.currentUserId

            // Combine all expenses, current month expenses, category totals for all time, and categories
            combine(
                com.example.budgettrain.data.repository.FirebaseRepository.getExpensesWithCategory(userId).map { it.map { expenseWithCategory ->
                    Expense(
                        id = expenseWithCategory.id,
                        userId = expenseWithCategory.userId,
                        categoryId = expenseWithCategory.categoryId,
                        amount = expenseWithCategory.amount,
                        date = expenseWithCategory.date,
                        startTime = expenseWithCategory.startTime,
                        endTime = expenseWithCategory.endTime,
                        description = expenseWithCategory.description,
                        imagePath = expenseWithCategory.imagePath
                    )
                }},
                com.example.budgettrain.data.repository.FirebaseRepository.getExpensesInRange(userId, startOfMonth, now),
                com.example.budgettrain.data.repository.FirebaseRepository.getCategoryTotals(userId, startOfMonth, now),
                com.example.budgettrain.data.repository.FirebaseRepository.getAllCategories(userId)
            ) { allExpenses, currentMonthExpenses, totals, categories ->
                Quadruple(allExpenses, currentMonthExpenses, totals, categories)
            }.collect { (allExpenses, currentMonthExpenses, totals, categories) ->
                // Use all expenses for the main display
                val totalSpent = allExpenses.sumOf { it.amount }
                val expenseCount = allExpenses.size
                val average = if (expenseCount > 0) totalSpent / expenseCount else 0.0

                // Get budget goals from SharedPreferences for now
                val prefs = getApplication<Application>().getSharedPreferences("budget_goals", android.content.Context.MODE_PRIVATE)
                val minGoal = prefs.getFloat("min_goal", 1000f).toDouble()
                val maxGoal = prefs.getFloat("max_goal", 5000f).toDouble()
                val goal = BudgetGoal(minimumGoal = minGoal, maximumGoal = maxGoal)
                val status = calculateBudgetStatus(totalSpent, goal)
                val percent = calculateProgressPercentage(totalSpent, goal.maximumGoal)
                val amountRemaining = (goal.maximumGoal - totalSpent).coerceAtLeast(0.0)

                val recent = allExpenses.maxByOrNull { it.date }?.let { e ->
                    RecentExpense(Date(e.date), e.amount)
                }

                val categoryIdToColor = categories.associate { it.id to it.color }

                // Calculate category totals from all expenses instead of just current month
                val allExpensesByCategory = allExpenses.groupBy { it.categoryId }
                val topCats: List<CategorySpend> = allExpensesByCategory
                    .map { (categoryId, expenses) ->
                        val category = categories.find { it.id == categoryId }
                        val total = expenses.sumOf { it.amount }
                        CategorySpend(
                            categoryId = categoryId,
                            name = category?.name ?: "Unknown",
                            color = category?.color ?: 0xFF2196F3,
                            amount = total
                        )
                    }
                    .sortedByDescending { it.amount }
                    .take(3)

                val topCategory = topCats.firstOrNull()

                // Use all expenses for daily trend, but limit to reasonable range
                val daily = buildDailyTrend(allExpenses, startOfMonth, now)

                // Get username from Firebase (async)
                val username = authRepository.currentUser?.let { user ->
                    try {
                        com.example.budgettrain.data.repository.FirebaseRepository.getUserProfile(user.uid)?.get("username") as? String
                    } catch (e: Exception) {
                        null
                    }
                } ?: "User"
                
                _state.value = _state.value.copy(
                    isLoading = false,
                    username = username,
                    totalSpentThisMonth = totalSpent, // Now shows all-time total
                    expenseCountThisMonth = expenseCount, // Now shows all-time count
                    averageExpenseThisMonth = average, // Now shows all-time average
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


