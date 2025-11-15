package com.example.budgettrain.feature.rewards

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.example.budgettrain.data.repository.FirebaseRepository
import com.example.budgettrain.data.repository.FirebaseAuthRepository
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat

data class Reward(
    val id: String,
    val title: String, 
    val description: String,
    val isEarned: Boolean,
    val progress: Float = 0f, // 0.0 to 1.0
    val pointsValue: Int = 0,
    val iconType: RewardIconType = RewardIconType.GIFT_CARD
)

data class Badge(
    val id: String,
    val name: String, 
    val criteria: String,
    val isEarned: Boolean,
    val progress: Float = 0f, // 0.0 to 1.0
    val earnedDate: Date? = null,
    val iconType: BadgeIconType = BadgeIconType.STAR
)

data class Challenge(
    val id: String,
    val title: String,
    val description: String,
    val targetType: ChallengeType,
    val targetValue: Double, // What they need to hit
    val currentValue: Double, // Their current progress
    val isCompleted: Boolean,
    val isActive: Boolean,
    val expiresAt: Date? = null,
    val rewardPoints: Int,
    val difficulty: ChallengeDifficulty = ChallengeDifficulty.MEDIUM,
    val iconType: RewardIconType = RewardIconType.TROPHY,
    val categoryIcon: String? = null, // For category-specific challenges
    val categoryName: String? = null, // Category name for category-specific challenges
    val timeFrame: ChallengeTimeFrame = ChallengeTimeFrame.WEEKLY, // When the challenge resets
    val isRepeatable: Boolean = false, // Can be completed multiple times
    val completedCount: Int = 0 // How many times completed (for repeatable challenges)
)

enum class ChallengeTimeFrame {
    DAILY,    // Resets every day
    WEEKLY,   // Resets every week
    MONTHLY,  // Resets every month
    ONE_TIME  // Can only be completed once
}

enum class RewardIconType { GIFT_CARD, COINS, TROPHY, GEM, DIAMOND, MEDAL }
enum class BadgeIconType { STAR, MEDAL, CROWN, AWARD }
enum class ChallengeType { 
    DAILY_EXPENSES, // Track X expenses in Y days
    BUDGET_STREAK, // Stay under budget for X consecutive days
    SAVINGS_TARGET, // Save X amount in Y timeframe
    CATEGORY_LIMIT, // Keep specific category under X
    EXPENSE_FREE_DAYS, // Have X no-expense days
    WEEKLY_SPENDING_LIMIT, // Stay under weekly limit
    MONTHLY_SAVINGS_GOAL, // Save X amount monthly
    CATEGORY_SPENDING_LIMIT, // Keep specific category under X amount
    DAILY_SPENDING_LIMIT, // Stay under daily spending limit
    WEEKLY_EXPENSE_COUNT, // Track X expenses in a week
    MONTHLY_EXPENSE_COUNT, // Track X expenses in a month
    CATEGORY_DIVERSITY, // Use X different categories
    LOW_SPENDING_DAYS, // Have X days with spending under Y
    CONSISTENT_TRACKING, // Track expenses for X consecutive days
    WEEKEND_SAVER, // Save money on weekends
    MORNING_TRACKER, // Track expenses in the morning
    EVENING_REVIEWER // Review expenses in the evening
}

enum class ChallengeDifficulty { EASY, MEDIUM, HARD, EXPERT }

data class RewardProgress(
    val dailyStreak: Int = 0,
    val expensesTracked: Int = 0,
    val weeklySavings: Double = 0.0,
    val monthlyGoalAchievements: Int = 0,
    val lowSpendingDays: Int = 0,
    val budgetUnderTarget: Boolean = false
)

class RewardsViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = FirebaseAuthRepository()
    
    private val _rewards = MutableStateFlow<List<Reward>>(emptyList())
    val rewards: StateFlow<List<Reward>> = _rewards.asStateFlow()

    private val _badges = MutableStateFlow<List<Badge>>(emptyList())
    val badges: StateFlow<List<Badge>> = _badges.asStateFlow()
    
    private val _challenges = MutableStateFlow<List<Challenge>>(emptyList())
    val challenges: StateFlow<List<Challenge>> = _challenges.asStateFlow()
    
    private val _progress = MutableStateFlow(RewardProgress())
    val progress: StateFlow<RewardProgress> = _progress.asStateFlow()
    
    private val _totalPoints = MutableStateFlow(0)
    val totalPoints: StateFlow<Int> = _totalPoints.asStateFlow()

    init {
        viewModelScope.launch {
            loadRewardsData()
        }
    }

    private suspend fun loadRewardsData() {
        // Calculate user progress and status
        calculateUserProgress()
        generateSmartRewards()
        generateSmartBadges()
        generateDynamicChallenges()
        calculateTotalPoints()
    }

    private suspend fun calculateUserProgress() {
        try {
            val calendar = Calendar.getInstance()
            val now = System.currentTimeMillis()
            
            // Get this week's expenses
            val weekStart = calendar.apply { 
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val userId = authRepository.currentUserId
            val expenseFlow = FirebaseRepository.getExpensesInRange(userId, weekStart, now)
            
            // Calculate metrics
            var expensesCount = 0
            var totalSpent = 0.0
            var dailyStreakCount = 0
            
            // Use first() to get the current value instead of collect
            val expenseList = expenseFlow.first()
            expensesCount = expenseList.size
            totalSpent = expenseList.sumOf { it.amount }
            
            // Calculate daily streak (consecutive days with expenses tracked)
            val datesWithExpenses = expenseList.map { expense ->
                Calendar.getInstance().apply { 
                    timeInMillis = expense.date
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }.distinct()
            
            dailyStreakCount = calculateStreak(datesWithExpenses)
            
            // Update progress
            _progress.value = RewardProgress(
                dailyStreak = dailyStreakCount,
                expensesTracked = expensesCount,
                weeklySavings = maxOf(0.0, 5000.0 - totalSpent), // Assuming weekly budget of R5000
                monthlyGoalAchievements = if (totalSpent < 5000.0) 1 else 0,
                lowSpendingDays = expenseList.filter { it.amount < 200.0 }.size,
                budgetUnderTarget = totalSpent < 5000.0
            )
        } catch (e: Exception) {
            // Fallback: Set realistic default progress values
            println("DEBUG: Error calculating user progress: ${e.message}")
            _progress.value = RewardProgress(
                dailyStreak = 0, // No streak if no data
                expensesTracked = 0, // No expenses tracked if no data
                weeklySavings = 0.0, // No savings if no data
                monthlyGoalAchievements = 0,
                lowSpendingDays = 0, // No low spending days if no data
                budgetUnderTarget = false // Not under budget if no data
            )
        }
    }

    private fun calculateStreak(dates: List<Long>): Int {
        if (dates.isEmpty()) return 0
        
        val sortedDates = dates.sortedDescending()
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        var streak = 0
        var currentDate = today
        
        for (date in sortedDates) {
            val dayBefore = currentDate - (24 * 60 * 60 * 1000)
            if (date == currentDate || date == dayBefore) {
                streak++
                currentDate = dayBefore
            } else {
                break
            }
        }
        
        return streak
    }

    private fun generateSmartRewards() {
        val currentProgress = _progress.value
        
        _rewards.value = listOf(
            Reward(
                id = "first_expense",
                title = "First Expense",
                description = "Track your first expense",
                isEarned = currentProgress.expensesTracked > 0,
                progress = minOf(1f, currentProgress.expensesTracked.toFloat()),
                pointsValue = 10
            ),
            Reward(
                id = "weekly_budget",
                title = "Weekly Budget Keeper",
                description = "Stay within R5,000 weekly budget",
                isEarned = currentProgress.budgetUnderTarget,
                progress = if (currentProgress.budgetUnderTarget) 1f else 0f,
                pointsValue = 50
            ),
            Reward(
                id = "expense_explorer", 
                title = "Expense Explorer",
                description = "Track 10 expenses",
                isEarned = currentProgress.expensesTracked >= 10,
                progress = minOf(1f, currentProgress.expensesTracked / 10f),
                pointsValue = 25
            ),
            Reward(
                id = "consistent_tracker",
                title = "Consistent Tracker",
                description = "Track expenses for 7 consecutive days",
                isEarned = currentProgress.dailyStreak >= 7,
                progress = minOf(1f, currentProgress.dailyStreak / 7f),
                pointsValue = 100
            ),
            Reward(
                id = "smart_spender",
                title = "Smart Spender",
                description = "Have 5 low-spending days (under R200)",
                isEarned = currentProgress.lowSpendingDays >= 5,
                progress = minOf(1f, currentProgress.lowSpendingDays / 5f),
                pointsValue = 75
            ),
            Reward(
                id = "savings_champion",
                title = "Savings Champion", 
                description = "Save R1,000 this week",
                isEarned = currentProgress.weeklySavings >= 1000.0 && currentProgress.expensesTracked >= 5,
                progress = minOf(1f, currentProgress.weeklySavings.toFloat() / 1000f),
                pointsValue = 150
            )
        )
    }

    private fun generateSmartBadges() {
        val currentProgress = _progress.value
        
        _badges.value = listOf(
            Badge(
                id = "tracking_starter",
                name = "Tracking Starter",
                criteria = "Begin your expense tracking journey",
                isEarned = currentProgress.expensesTracked > 0,
                progress = if (currentProgress.expensesTracked > 0) 1f else 0f,
                earnedDate = if (currentProgress.expensesTracked > 0) Date() else null
            ),
            Badge(
                id = "budget_master",
                name = "Budget Master",
                criteria = "Stay within budget for 7 days",
                isEarned = currentProgress.dailyStreak >= 7 && currentProgress.budgetUnderTarget,
                progress = minOf(1f, currentProgress.dailyStreak / 7f),
                earnedDate = if (currentProgress.dailyStreak >= 7 && currentProgress.budgetUnderTarget) Date() else null
            ),
            Badge(
                id = "data_driven",
                name = "Data Driven",
                criteria = "Track over 50 expenses",
                isEarned = currentProgress.expensesTracked >= 50,
                progress = minOf(1f, currentProgress.expensesTracked / 50f),
                earnedDate = if (currentProgress.expensesTracked >= 50) Date() else null
            ),
            Badge(
                id = "savings_expert",
                name = "Savings Expert",
                criteria = "Save over R2,000 in a week",
                isEarned = currentProgress.weeklySavings >= 2000.0 && currentProgress.expensesTracked >= 10,
                progress = minOf(1f, currentProgress.weeklySavings.toFloat() / 2000f),
                earnedDate = if (currentProgress.weeklySavings >= 2000.0 && currentProgress.expensesTracked >= 10) Date() else null,
                iconType = BadgeIconType.CROWN
            ),
            Badge(
                id = "consistency_king",
                name = "Consistency King",
                criteria = "Track expenses for 30 consecutive days",
                isEarned = currentProgress.dailyStreak >= 30,
                progress = minOf(1f, currentProgress.dailyStreak / 30f),
                earnedDate = if (currentProgress.dailyStreak >= 30) Date() else null,
                iconType = BadgeIconType.MEDAL
            )
        )
    }

    private suspend fun generateDynamicChallenges() {
        val currentProgress = _progress.value
        println("DEBUG: Generating challenges with progress: $currentProgress")
        
        // Check if there are any expenses in the database
        val hasExpenses = hasAnyExpenses()
        println("DEBUG: Has expenses: $hasExpenses")
        
        // Calculate actual values using suspend functions
        val expenseFreeDays = calculateExpenseFreedays()
        val dailySpending = calculateDailySpending()
        val morningTracking = calculateMorningTracking()
        val categoryDiversity = calculateCategoryDiversity()
        val weekendSpending = calculateWeekendSpending()
        val eveningTracking = calculateEveningTracking()
        
        // Ensure we always have some challenges, even with minimal data
        val challenges = listOf(
            // Test challenge - should always be visible
            Challenge(
                id = "test_challenge",
                title = "Test Challenge",
                description = "This is a test challenge to verify the system works",
                targetType = ChallengeType.DAILY_EXPENSES,
                targetValue = 1.0,
                currentValue = 0.0,
                isCompleted = false,
                isActive = true,
                rewardPoints = 10,
                difficulty = ChallengeDifficulty.EASY,
                iconType = RewardIconType.COINS,
                timeFrame = ChallengeTimeFrame.DAILY,
                isRepeatable = true
            ),
            
            // Daily Challenges
            Challenge(
                id = "daily_tracker",
                title = "Daily Tracker",
                description = "Track expenses for 3 consecutive days",
                targetType = ChallengeType.DAILY_EXPENSES,
                targetValue = 3.0,
                currentValue = currentProgress.dailyStreak.toDouble(),
                isCompleted = hasExpenses && currentProgress.dailyStreak >= 3,
                isActive = true,
                rewardPoints = 150,
                difficulty = ChallengeDifficulty.EASY,
                iconType = RewardIconType.COINS
            ),
            
            // Weekly Challenges
            Challenge(
                id = "budget_keeper_week",
                title = "Weekly Budget Keeper",
                description = "Stay under R5,000 weekly spending",
                targetType = ChallengeType.WEEKLY_SPENDING_LIMIT,
                targetValue = 5000.0,
                currentValue = maxOf(0.0, 5000.0 - currentProgress.weeklySavings),
                isCompleted = hasExpenses && isEndOfWeek() && currentProgress.budgetUnderTarget && currentProgress.weeklySavings > 0,
                isActive = true,
                rewardPoints = 250,
                difficulty = ChallengeDifficulty.MEDIUM,
                iconType = RewardIconType.TROPHY
            ),
            
            Challenge(
                id = "savings_challenge",
                title = "Savings Challenge",
                description = "Save R1,000 this week",
                targetType = ChallengeType.SAVINGS_TARGET,
                targetValue = 1000.0,
                currentValue = currentProgress.weeklySavings,
                isCompleted = hasExpenses && isEndOfWeek() && currentProgress.weeklySavings >= 1000.0 && currentProgress.expensesTracked >= 5,
                isActive = true,
                rewardPoints = 300,
                difficulty = ChallengeDifficulty.HARD,
                iconType = RewardIconType.DIAMOND,
                timeFrame = ChallengeTimeFrame.WEEKLY,
                isRepeatable = true
            ),
            
            // Monthly Challenges
            Challenge(
                id = "consistent_summer",
                title = "Consistent Summer",
                description = "Track expenses for 14 consecutive days",
                targetType = ChallengeType.DAILY_EXPENSES,
                targetValue = 14.0,
                currentValue = currentProgress.dailyStreak.toDouble(),
                isCompleted = currentProgress.dailyStreak >= 14,
                isActive = true,
                rewardPoints = 500,
                difficulty = ChallengeDifficulty.EXPERT,
                iconType = RewardIconType.MEDAL
            ),
            
            // Advanced Challenges
            Challenge(
                id = "smart_spender",
                title = "Smart Spender",
                description = "Have 7 days with expenses under R200",
                targetType = ChallengeType.CATEGORY_LIMIT,
                targetValue = 7.0,
                currentValue = currentProgress.lowSpendingDays.toDouble(),
                isCompleted = currentProgress.lowSpendingDays >= 7,
                isActive = true,
                rewardPoints = 400,
                difficulty = ChallengeDifficulty.HARD,
                iconType = RewardIconType.GEM
            ),
            
            Challenge(
                id = "data_master",
                title = "Data Master", 
                description = "Track 30 expenses this month",
                targetType = ChallengeType.DAILY_EXPENSES,
                targetValue = 30.0,
                currentValue = currentProgress.expensesTracked.toDouble(),
                isCompleted = currentProgress.expensesTracked >= 30,
                isActive = true,
                rewardPoints = 450,
                difficulty = ChallengeDifficulty.EXPERT,
                iconType = RewardIconType.COINS
            ),
            
            Challenge(
                id = "expense_free_challenge",
                title = "No-Spend Challenge",
                description = "Have 2 expense-free days this week",
                targetType = ChallengeType.EXPENSE_FREE_DAYS,
                targetValue = 2.0,
                currentValue = expenseFreeDays.toDouble(),
                isCompleted = hasExpenses && isEndOfWeek() && expenseFreeDays >= 2 && currentProgress.expensesTracked >= 3,
                isActive = true,
                rewardPoints = 200,
                difficulty = ChallengeDifficulty.MEDIUM,
                iconType = RewardIconType.TROPHY,
                timeFrame = ChallengeTimeFrame.WEEKLY,
                isRepeatable = true
            ),
            
            // Daily Challenges
            Challenge(
                id = "daily_budget_keeper",
                title = "Daily Budget Keeper",
                description = "Stay under R500 daily spending",
                targetType = ChallengeType.DAILY_SPENDING_LIMIT,
                targetValue = 500.0,
                currentValue = dailySpending,
                isCompleted = hasExpenses && dailySpending <= 500.0,
                isActive = true,
                rewardPoints = 100,
                difficulty = ChallengeDifficulty.EASY,
                iconType = RewardIconType.COINS,
                timeFrame = ChallengeTimeFrame.DAILY,
                isRepeatable = true
            ),
            
            Challenge(
                id = "morning_tracker",
                title = "Morning Tracker",
                description = "Track expenses before 10 AM for 5 days",
                targetType = ChallengeType.MORNING_TRACKER,
                targetValue = 5.0,
                currentValue = morningTracking.toDouble(),
                isCompleted = hasExpenses && isEndOfWeek() && morningTracking >= 5,
                isActive = true,
                rewardPoints = 200,
                difficulty = ChallengeDifficulty.MEDIUM,
                iconType = RewardIconType.TROPHY,
                timeFrame = ChallengeTimeFrame.WEEKLY,
                isRepeatable = true
            ),
            
            // Weekly Challenges
            Challenge(
                id = "weekly_expense_counter",
                title = "Weekly Expense Counter",
                description = "Track 15 expenses this week",
                targetType = ChallengeType.WEEKLY_EXPENSE_COUNT,
                targetValue = 15.0,
                currentValue = currentProgress.expensesTracked.toDouble(),
                isCompleted = currentProgress.expensesTracked >= 15,
                isActive = true,
                rewardPoints = 200,
                difficulty = ChallengeDifficulty.MEDIUM,
                iconType = RewardIconType.TROPHY,
                timeFrame = ChallengeTimeFrame.WEEKLY,
                isRepeatable = true
            ),
            
            Challenge(
                id = "category_diversity",
                title = "Category Explorer",
                description = "Use 5 different expense categories this week",
                targetType = ChallengeType.CATEGORY_DIVERSITY,
                targetValue = 5.0,
                currentValue = categoryDiversity.toDouble(),
                isCompleted = hasExpenses && isEndOfWeek() && categoryDiversity >= 5,
                isActive = true,
                rewardPoints = 150,
                difficulty = ChallengeDifficulty.MEDIUM,
                iconType = RewardIconType.GEM,
                timeFrame = ChallengeTimeFrame.WEEKLY,
                isRepeatable = true
            ),
            
            Challenge(
                id = "weekend_saver",
                title = "Weekend Saver",
                description = "Spend less than R300 on weekends",
                targetType = ChallengeType.WEEKEND_SAVER,
                targetValue = 300.0,
                currentValue = weekendSpending,
                isCompleted = hasExpenses && isEndOfWeek() && weekendSpending <= 300.0,
                isActive = isWeekend(),
                rewardPoints = 180,
                difficulty = ChallengeDifficulty.MEDIUM,
                iconType = RewardIconType.DIAMOND,
                timeFrame = ChallengeTimeFrame.WEEKLY,
                isRepeatable = true
            ),
            
            // Monthly Challenges
            Challenge(
                id = "monthly_expense_master",
                title = "Monthly Expense Master",
                description = "Track 100 expenses this month",
                targetType = ChallengeType.MONTHLY_EXPENSE_COUNT,
                targetValue = 100.0,
                currentValue = calculateMonthlyExpenseCount().toDouble(),
                isCompleted = hasExpenses && isEndOfMonth() && calculateMonthlyExpenseCount() >= 100 && currentProgress.expensesTracked >= 20,
                isActive = true,
                rewardPoints = 500,
                difficulty = ChallengeDifficulty.EXPERT,
                iconType = RewardIconType.MEDAL,
                timeFrame = ChallengeTimeFrame.MONTHLY,
                isRepeatable = true
            ),
            
            Challenge(
                id = "monthly_savings_champion",
                title = "Monthly Savings Champion",
                description = "Save R5,000 this month",
                targetType = ChallengeType.MONTHLY_SAVINGS_GOAL,
                targetValue = 5000.0,
                currentValue = calculateMonthlySavings(),
                isCompleted = hasExpenses && isEndOfMonth() && calculateMonthlySavings() >= 5000.0 && currentProgress.expensesTracked >= 20,
                isActive = true,
                rewardPoints = 750,
                difficulty = ChallengeDifficulty.EXPERT,
                iconType = RewardIconType.DIAMOND,
                timeFrame = ChallengeTimeFrame.MONTHLY,
                isRepeatable = true
            ),
            
            // Category-Specific Challenges
            Challenge(
                id = "food_budget_keeper",
                title = "Food Budget Keeper",
                description = "Keep food expenses under R1,500 this week",
                targetType = ChallengeType.CATEGORY_SPENDING_LIMIT,
                targetValue = 1500.0,
                currentValue = calculateCategorySpending("Food"),
                isCompleted = hasExpenses && isEndOfWeek() && calculateCategorySpending("Food") <= 1500.0,
                isActive = true,
                rewardPoints = 200,
                difficulty = ChallengeDifficulty.MEDIUM,
                iconType = RewardIconType.TROPHY,
                categoryName = "Food",
                timeFrame = ChallengeTimeFrame.WEEKLY,
                isRepeatable = true
            ),
            
            Challenge(
                id = "transport_saver",
                title = "Transport Saver",
                description = "Keep transport expenses under R800 this week",
                targetType = ChallengeType.CATEGORY_SPENDING_LIMIT,
                targetValue = 800.0,
                currentValue = calculateCategorySpending("Transport"),
                isCompleted = hasExpenses && isEndOfWeek() && calculateCategorySpending("Transport") <= 800.0,
                isActive = true,
                rewardPoints = 150,
                difficulty = ChallengeDifficulty.EASY,
                iconType = RewardIconType.COINS,
                categoryName = "Transport",
                timeFrame = ChallengeTimeFrame.WEEKLY,
                isRepeatable = true
            ),
            
            // Advanced Challenges
            Challenge(
                id = "low_spending_master",
                title = "Low Spending Master",
                description = "Have 10 days with expenses under R200",
                targetType = ChallengeType.LOW_SPENDING_DAYS,
                targetValue = 10.0,
                currentValue = currentProgress.lowSpendingDays.toDouble(),
                isCompleted = hasExpenses && isEndOfMonth() && currentProgress.lowSpendingDays >= 10,
                isActive = true,
                rewardPoints = 400,
                difficulty = ChallengeDifficulty.HARD,
                iconType = RewardIconType.GEM,
                timeFrame = ChallengeTimeFrame.MONTHLY,
                isRepeatable = true
            ),
            
            Challenge(
                id = "consistent_tracker_pro",
                title = "Consistent Tracker Pro",
                description = "Track expenses for 21 consecutive days",
                targetType = ChallengeType.CONSISTENT_TRACKING,
                targetValue = 21.0,
                currentValue = currentProgress.dailyStreak.toDouble(),
                isCompleted = hasExpenses && currentProgress.dailyStreak >= 21,
                isActive = true,
                rewardPoints = 600,
                difficulty = ChallengeDifficulty.EXPERT,
                iconType = RewardIconType.MEDAL,
                timeFrame = ChallengeTimeFrame.ONE_TIME,
                isRepeatable = false
            ),
            
            Challenge(
                id = "evening_reviewer",
                title = "Evening Reviewer",
                description = "Review and track expenses after 6 PM for 7 days",
                targetType = ChallengeType.EVENING_REVIEWER,
                targetValue = 7.0,
                currentValue = eveningTracking.toDouble(),
                isCompleted = hasExpenses && isEndOfWeek() && eveningTracking >= 7,
                isActive = true,
                rewardPoints = 250,
                difficulty = ChallengeDifficulty.MEDIUM,
                iconType = RewardIconType.TROPHY,
                timeFrame = ChallengeTimeFrame.WEEKLY,
                isRepeatable = true
            )
        )
        
        // Set the challenges
        _challenges.value = challenges
        println("DEBUG: Set ${challenges.size} challenges, active challenges: ${challenges.count { it.isActive }}")
        
        // Check for natural completions after setting challenges
        checkAndCompleteChallenges()
    }
    
    private suspend fun calculateExpenseFreedays(): Int {
        return try {
            val userId = authRepository.currentUserId
            val expenseList = FirebaseRepository.getExpensesWithCategory(userId).first()
            val calendar = Calendar.getInstance()
            val today = calendar.timeInMillis
            
            // Get the start of the week (Monday)
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val weekStart = calendar.timeInMillis
            
            // Get all days in the current week
            val weekDays = mutableListOf<Long>()
            var currentDay = weekStart
            while (currentDay <= today) {
                weekDays.add(currentDay)
                currentDay += (24 * 60 * 60 * 1000) // Add one day
            }
            
            // Count days with no expenses
            val daysWithExpenses = expenseList
                .filter { it.date >= weekStart && it.date <= today }
                .map { expense ->
                    val expenseCalendar = Calendar.getInstance()
                    expenseCalendar.timeInMillis = expense.date
                    expenseCalendar.set(Calendar.HOUR_OF_DAY, 0)
                    expenseCalendar.set(Calendar.MINUTE, 0)
                    expenseCalendar.set(Calendar.SECOND, 0)
                    expenseCalendar.set(Calendar.MILLISECOND, 0)
                    expenseCalendar.timeInMillis
                }
                .distinct()
            
            val expenseFreeDays = weekDays.count { day -> day !in daysWithExpenses }
            expenseFreeDays
        } catch (e: Exception) {
            println("DEBUG: Error calculating expense-free days: ${e.message}")
            0
        }
    }
    
    private suspend fun calculateDailySpending(): Double {
        return try {
            val userId = authRepository.currentUserId
            val expenseList = FirebaseRepository.getExpensesWithCategory(userId).first()
            val calendar = Calendar.getInstance()
            val today = calendar.timeInMillis
            
            // Get start of today
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val todayStart = calendar.timeInMillis
            
            // Get end of today
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val todayEnd = calendar.timeInMillis
            
            // Calculate total spending for today
            val todayExpenses = expenseList.filter { expense ->
                expense.date >= todayStart && expense.date <= todayEnd
            }
            
            todayExpenses.sumOf { it.amount }
        } catch (e: Exception) {
            println("DEBUG: Error calculating daily spending: ${e.message}")
            0.0
        }
    }
    
    private suspend fun calculateMorningTracking(): Int {
        return try {
            val userId = authRepository.currentUserId
            val expenseList = FirebaseRepository.getExpensesWithCategory(userId).first()
            val calendar = Calendar.getInstance()
            val today = calendar.timeInMillis
            
            // Get the start of the week (Monday)
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val weekStart = calendar.timeInMillis
            
            // Count days where expenses were tracked before 10 AM
            val morningTrackingDays = expenseList
                .filter { expense -> 
                    expense.date >= weekStart && expense.date <= today &&
                    expense.startTime != null
                }
                .map { expense ->
                    val expenseCalendar = Calendar.getInstance()
                    expenseCalendar.timeInMillis = expense.startTime!!
                    expenseCalendar.set(Calendar.HOUR_OF_DAY, 0)
                    expenseCalendar.set(Calendar.MINUTE, 0)
                    expenseCalendar.set(Calendar.SECOND, 0)
                    expenseCalendar.set(Calendar.MILLISECOND, 0)
                    expenseCalendar.timeInMillis
                }
                .distinct()
                .count { dayTimestamp ->
                    val dayCalendar = Calendar.getInstance()
                    dayCalendar.timeInMillis = dayTimestamp
                    val dayExpenses = expenseList.filter { expense ->
                        val expenseDayCalendar = Calendar.getInstance()
                        expenseDayCalendar.timeInMillis = expense.startTime ?: expense.date
                        expenseDayCalendar.set(Calendar.HOUR_OF_DAY, 0)
                        expenseDayCalendar.set(Calendar.MINUTE, 0)
                        expenseDayCalendar.set(Calendar.SECOND, 0)
                        expenseDayCalendar.set(Calendar.MILLISECOND, 0)
                        expenseDayCalendar.timeInMillis == dayTimestamp
                    }
                    
                    // Check if any expense on this day was tracked before 10 AM
                    dayExpenses.any { expense ->
                        val expenseTimeCalendar = Calendar.getInstance()
                        expenseTimeCalendar.timeInMillis = expense.startTime ?: expense.date
                        expenseTimeCalendar.get(Calendar.HOUR_OF_DAY) < 10
                    }
                }
            
            morningTrackingDays
        } catch (e: Exception) {
            println("DEBUG: Error calculating morning tracking: ${e.message}")
            0
        }
    }
    
    private suspend fun calculateCategoryDiversity(): Int {
        return try {
            val userId = authRepository.currentUserId
            val expenseList = FirebaseRepository.getExpensesWithCategory(userId).first()
            val calendar = Calendar.getInstance()
            val today = calendar.timeInMillis
            
            // Get the start of the week (Monday)
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val weekStart = calendar.timeInMillis
            
            // Get unique categories used this week
            val uniqueCategories = expenseList
                .filter { expense -> expense.date >= weekStart && expense.date <= today }
                .map { expense -> expense.categoryName }
                .distinct()
                .count()
            
            uniqueCategories
        } catch (e: Exception) {
            println("DEBUG: Error calculating category diversity: ${e.message}")
            0
        }
    }
    
    private suspend fun calculateWeekendSpending(): Double {
        return try {
            val userId = authRepository.currentUserId
            val expenseList = FirebaseRepository.getExpensesWithCategory(userId).first()
            val calendar = Calendar.getInstance()
            val today = calendar.timeInMillis
            
            // Get the start of the week (Monday)
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val weekStart = calendar.timeInMillis
            
            // Calculate weekend spending (Saturday + Sunday)
            val weekendExpenses = expenseList.filter { expense ->
                expense.date >= weekStart && expense.date <= today
            }.filter { expense ->
                val expenseCalendar = Calendar.getInstance()
                expenseCalendar.timeInMillis = expense.date
                val dayOfWeek = expenseCalendar.get(Calendar.DAY_OF_WEEK)
                dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
            }
            
            weekendExpenses.sumOf { it.amount }
        } catch (e: Exception) {
            println("DEBUG: Error calculating weekend spending: ${e.message}")
            0.0
        }
    }
    
    private fun isWeekend(): Boolean {
        val calendar = java.util.Calendar.getInstance()
        val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        // Saturday = 7, Sunday = 1
        return dayOfWeek == java.util.Calendar.SATURDAY || dayOfWeek == java.util.Calendar.SUNDAY
    }
    
    private suspend fun hasAnyExpenses(): Boolean {
        return try {
            val userId = authRepository.currentUserId
            val expenseList = FirebaseRepository.getExpensesWithCategory(userId).first()
            expenseList.isNotEmpty()
        } catch (e: Exception) {
            println("DEBUG: Error checking for expenses: ${e.message}")
            false
        }
    }
    
    private fun isEndOfWeek(): Boolean {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        // Allow completion on Saturday (7) or Sunday (1) - end of week
        return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
    }
    
    private fun isEndOfMonth(): Boolean {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_MONTH)
        val lastDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        return today == lastDayOfMonth
    }
    
    private suspend fun calculateMonthlyExpenseCount(): Int {
        return try {
            val calendar = Calendar.getInstance()
            val now = System.currentTimeMillis()
            
            // Get this month's expenses
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val monthStart = calendar.timeInMillis
            
            val userId = authRepository.currentUserId
            val expenseFlow = FirebaseRepository.getExpensesInRange(userId, monthStart, now)
            val expenseList = expenseFlow.first()
            expenseList.size
        } catch (e: Exception) {
            println("DEBUG: Error calculating monthly expense count: ${e.message}")
            0
        }
    }
    
    private suspend fun calculateMonthlySavings(): Double {
        return try {
            val calendar = Calendar.getInstance()
            val now = System.currentTimeMillis()
            
            // Get this month's expenses
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val monthStart = calendar.timeInMillis
            
            val userId = authRepository.currentUserId
            val expenseFlow = FirebaseRepository.getExpensesInRange(userId, monthStart, now)
            val expenseList = expenseFlow.first()
            val totalSpent = expenseList.sumOf { it.amount }
            
            // Assuming monthly budget of R20,000
            maxOf(0.0, 20000.0 - totalSpent)
        } catch (e: Exception) {
            println("DEBUG: Error calculating monthly savings: ${e.message}")
            0.0
        }
    }
    
    private fun calculateCategorySpending(categoryName: String): Double {
        // Calculate spending in a specific category this week
        // For now, return mock values based on category
        return when (categoryName) {
            "Food" -> maxOf(0.0, 1500.0 - (_progress.value.weeklySavings / 3))
            "Transport" -> maxOf(0.0, 800.0 - (_progress.value.weeklySavings / 5))
            else -> maxOf(0.0, 500.0 - (_progress.value.weeklySavings / 7))
        }
    }
    
    private suspend fun calculateEveningTracking(): Int {
        return try {
            val userId = authRepository.currentUserId
            val expenseList = FirebaseRepository.getExpensesWithCategory(userId).first()
            val calendar = Calendar.getInstance()
            val today = calendar.timeInMillis
            
            // Get the start of the week (Monday)
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val weekStart = calendar.timeInMillis
            
            // Count days where expenses were tracked after 6 PM
            val eveningTrackingDays = expenseList
                .filter { expense -> 
                    expense.date >= weekStart && expense.date <= today &&
                    expense.startTime != null
                }
                .map { expense ->
                    val expenseCalendar = Calendar.getInstance()
                    expenseCalendar.timeInMillis = expense.startTime!!
                    expenseCalendar.set(Calendar.HOUR_OF_DAY, 0)
                    expenseCalendar.set(Calendar.MINUTE, 0)
                    expenseCalendar.set(Calendar.SECOND, 0)
                    expenseCalendar.set(Calendar.MILLISECOND, 0)
                    expenseCalendar.timeInMillis
                }
                .distinct()
                .count { dayTimestamp ->
                    val dayCalendar = Calendar.getInstance()
                    dayCalendar.timeInMillis = dayTimestamp
                    val dayExpenses = expenseList.filter { expense ->
                        val expenseDayCalendar = Calendar.getInstance()
                        expenseDayCalendar.timeInMillis = expense.startTime ?: expense.date
                        expenseDayCalendar.set(Calendar.HOUR_OF_DAY, 0)
                        expenseDayCalendar.set(Calendar.MINUTE, 0)
                        expenseDayCalendar.set(Calendar.SECOND, 0)
                        expenseDayCalendar.set(Calendar.MILLISECOND, 0)
                        expenseDayCalendar.timeInMillis == dayTimestamp
                    }
                    
                    // Check if any expense on this day was tracked after 6 PM
                    dayExpenses.any { expense ->
                        val expenseTimeCalendar = Calendar.getInstance()
                        expenseTimeCalendar.timeInMillis = expense.startTime ?: expense.date
                        expenseTimeCalendar.get(Calendar.HOUR_OF_DAY) >= 18 // 6 PM
                    }
                }
            
            eveningTrackingDays
        } catch (e: Exception) {
            println("DEBUG: Error calculating evening tracking: ${e.message}")
            0
        }
    }
    
    private fun calculateTotalPoints() {
        val earnedRewards = _rewards.value.filter { it.isEarned }.sumOf { it.pointsValue.toLong() }
        val earnedBadges = _badges.value.filter { it.isEarned }.sumOf { 100L } // Each badge worth 100 points
        val completedChallenges = _challenges.value.filter { it.isCompleted }.sumOf { it.rewardPoints.toLong() }
        
        val totalPoints = (earnedRewards + earnedBadges + completedChallenges).toInt()
        _totalPoints.value = totalPoints
        
        println("DEBUG: Total points calculated - Rewards: $earnedRewards, Badges: $earnedBadges, Challenges: $completedChallenges, Total: $totalPoints")
    }

    fun refreshRewards() {
        viewModelScope.launch {
            loadRewardsData()
        }
    }
    
    // Force refresh all data - useful for testing
    fun forceRefreshAll() {
        viewModelScope.launch {
            try {
                calculateUserProgress()
                generateSmartRewards()
                generateSmartBadges()
                generateDynamicChallenges()
                calculateTotalPoints()
                println("DEBUG: Force refresh completed - All data regenerated")
            } catch (e: Exception) {
                println("DEBUG: Error in force refresh: ${e.message}")
            }
        }
    }
    
    fun completeChallenge(challengeId: String) {
        viewModelScope.launch {
            val currentChallenges = _challenges.value.toMutableList()
            val challengeIndex = currentChallenges.indexOfFirst { it.id == challengeId }
            
            if (challengeIndex != -1) {
                val challenge = currentChallenges[challengeIndex]
                if (challenge.isRepeatable) {
                    // For repeatable challenges, increment completed count
                    currentChallenges[challengeIndex] = challenge.copy(
                        completedCount = challenge.completedCount + 1,
                        isCompleted = true
                    )
                } else {
                    // For one-time challenges, mark as completed
                    currentChallenges[challengeIndex] = challenge.copy(isCompleted = true)
                }
                
                _challenges.value = currentChallenges
                
                // Debug logging for challenge completion
                val completedChallenges = currentChallenges.filter { it.isCompleted }
                val totalPoints = completedChallenges.sumOf { it.rewardPoints }
                println("DEBUG: Challenge '$challengeId' completed:")
                println("  Total completed challenges: ${completedChallenges.size}")
                println("  Total points from challenges: $totalPoints")
                println("  Completed challenges: ${completedChallenges.map { "${it.title} (${it.rewardPoints}pts)" }}")
                
                // Regenerate rewards and badges to reflect new completion status
                generateSmartRewards()
                generateSmartBadges()
                calculateTotalPoints()
                
                println("DEBUG: All data regenerated after challenge completion")
            }
        }
    }
    
    fun getChallengesByTimeFrame(timeFrame: ChallengeTimeFrame): List<Challenge> {
        return _challenges.value.filter { it.timeFrame == timeFrame && it.isActive }
    }
    
    fun getChallengesByDifficulty(difficulty: ChallengeDifficulty): List<Challenge> {
        return _challenges.value.filter { it.difficulty == difficulty && it.isActive }
    }
    
    fun getCompletedChallenges(): List<Challenge> {
        return _challenges.value.filter { it.isCompleted }
    }
    
    fun getActiveChallenges(): List<Challenge> {
        return _challenges.value.filter { it.isActive && !it.isCompleted }
    }
    
    fun getChallengeProgress(): Map<ChallengeTimeFrame, Pair<Int, Int>> {
        val challenges = _challenges.value // Include all challenges, not just active ones
        return ChallengeTimeFrame.values().associateWith { timeFrame ->
            val timeFrameChallenges = challenges.filter { it.timeFrame == timeFrame }
            val completed = timeFrameChallenges.count { it.isCompleted }
            val total = timeFrameChallenges.size
            completed to total
        }
    }
    
    // Test function to verify challenges are working
    fun testChallenges(): String {
        val challenges = _challenges.value
        val activeChallenges = challenges.filter { it.isActive }
        val completedChallenges = challenges.filter { it.isCompleted }
        
        return buildString {
            appendLine("=== CHALLENGE TEST RESULTS ===")
            appendLine("Total challenges: ${challenges.size}")
            appendLine("Active challenges: ${activeChallenges.size}")
            appendLine("Completed challenges: ${completedChallenges.size}")
            appendLine()
            appendLine("Active Challenge Details:")
            activeChallenges.forEach { challenge ->
                appendLine("- ${challenge.title}: ${challenge.currentValue}/${challenge.targetValue} (${challenge.timeFrame})")
            }
            appendLine()
            appendLine("Progress Data:")
            appendLine("- Daily Streak: ${_progress.value.dailyStreak}")
            appendLine("- Expenses Tracked: ${_progress.value.expensesTracked}")
            appendLine("- Weekly Savings: R${_progress.value.weeklySavings}")
            appendLine("- Low Spending Days: ${_progress.value.lowSpendingDays}")
        }
    }
    
    // Test function to simulate expense tracking for testing challenges
    fun simulateExpenseTracking() {
        viewModelScope.launch {
            try {
                // Simulate adding an expense to trigger challenge updates
                val currentProgress = _progress.value
                val newProgress = currentProgress.copy(
                    expensesTracked = currentProgress.expensesTracked + 1,
                    dailyStreak = currentProgress.dailyStreak + 1
                )
                _progress.value = newProgress
                
                // Regenerate challenges with new progress
                generateDynamicChallenges()
                calculateTotalPoints()
                
                println("DEBUG: Simulated expense tracking - Progress updated")
            } catch (e: Exception) {
                println("DEBUG: Error simulating expense tracking: ${e.message}")
            }
        }
    }
    
    // Reset function to reset all challenges and progress for testing
    fun resetChallenges() {
        viewModelScope.launch {
            try {
                // Reset progress to initial state
                _progress.value = RewardProgress(
                    dailyStreak = 0,
                    expensesTracked = 0,
                    weeklySavings = 0.0,
                    monthlyGoalAchievements = 0,
                    lowSpendingDays = 0,
                    budgetUnderTarget = false
                )
                
                // Regenerate all data with reset progress
                generateSmartRewards()
                generateSmartBadges()
                generateDynamicChallenges()
                calculateTotalPoints()
                
                println("DEBUG: Challenges and progress reset to initial state")
            } catch (e: Exception) {
                println("DEBUG: Error resetting challenges: ${e.message}")
            }
        }
    }
    
    // Function to check and complete challenges naturally based on progress
    private fun checkAndCompleteChallenges() {
        val currentChallenges = _challenges.value.toMutableList()
        var hasChanges = false
        
        currentChallenges.forEachIndexed { index, challenge ->
            if (!challenge.isCompleted && challenge.isActive) {
                val shouldComplete = when {
                    challenge.targetValue <= 0 -> false
                    challenge.currentValue >= challenge.targetValue -> true
                    else -> false
                }
                
                if (shouldComplete) {
                    if (challenge.isRepeatable) {
                        currentChallenges[index] = challenge.copy(
                            completedCount = challenge.completedCount + 1,
                            isCompleted = true
                        )
                    } else {
                        currentChallenges[index] = challenge.copy(isCompleted = true)
                    }
                    hasChanges = true
                    println("DEBUG: Challenge '${challenge.title}' completed naturally")
                }
            }
        }
        
        if (hasChanges) {
            _challenges.value = currentChallenges
            generateSmartRewards()
            generateSmartBadges()
            calculateTotalPoints()
        }
    }
}
