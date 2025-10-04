package com.example.budgettrain.feature.rewards

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.example.budgettrain.data.db.DatabaseProvider
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

    private val expenseDao = DatabaseProvider.get(application).expenseDao()
    
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
            
            val expenseFlow = expenseDao.getExpensesInRange(weekStart, now)
            
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
            // Fallback: Set some default progress values for testing
            println("DEBUG: Error calculating user progress: ${e.message}")
            _progress.value = RewardProgress(
                dailyStreak = 1, // Start with some progress for testing
                expensesTracked = 5,
                weeklySavings = 2000.0,
                monthlyGoalAchievements = 0,
                lowSpendingDays = 2,
                budgetUnderTarget = true
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
                isEarned = currentProgress.weeklySavings >= 1000.0,
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
                isEarned = currentProgress.weeklySavings >= 2000.0,
                progress = minOf(1f, currentProgress.weeklySavings.toFloat() / 2000f),
                earnedDate = if (currentProgress.weeklySavings >= 2000.0) Date() else null,
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

    private fun generateDynamicChallenges() {
        val currentProgress = _progress.value
        println("DEBUG: Generating challenges with progress: $currentProgress")
        
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
                isCompleted = currentProgress.dailyStreak >= 3,
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
                isCompleted = currentProgress.budgetUnderTarget,
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
                isCompleted = currentProgress.weeklySavings >= 1000.0,
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
                currentValue = calculateExpenseFreedays().toDouble(),
                isCompleted = calculateExpenseFreedays() >= 2,
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
                currentValue = calculateDailySpending(),
                isCompleted = calculateDailySpending() <= 500.0,
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
                description = "Track your first expense before 10 AM",
                targetType = ChallengeType.MORNING_TRACKER,
                targetValue = 1.0,
                currentValue = calculateMorningTracking().toDouble(),
                isCompleted = calculateMorningTracking() >= 1,
                isActive = true,
                rewardPoints = 75,
                difficulty = ChallengeDifficulty.EASY,
                iconType = RewardIconType.GEM,
                timeFrame = ChallengeTimeFrame.DAILY,
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
                currentValue = calculateCategoryDiversity().toDouble(),
                isCompleted = calculateCategoryDiversity() >= 5,
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
                currentValue = calculateWeekendSpending(),
                isCompleted = calculateWeekendSpending() <= 300.0,
                isActive = true,
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
                isCompleted = calculateMonthlyExpenseCount() >= 100,
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
                isCompleted = calculateMonthlySavings() >= 5000.0,
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
                isCompleted = calculateCategorySpending("Food") <= 1500.0,
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
                isCompleted = calculateCategorySpending("Transport") <= 800.0,
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
                isCompleted = currentProgress.lowSpendingDays >= 10,
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
                isCompleted = currentProgress.dailyStreak >= 21,
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
                currentValue = calculateEveningTracking().toDouble(),
                isCompleted = calculateEveningTracking() >= 7,
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
    }
    
    private fun calculateExpenseFreedays(): Int {
        // This would calculate actual expense-free days from the database
        // For now, return calculated count based on low spending days pattern
        return (_progress.value.lowSpendingDays / 5).coerceAtMost(7)
    }
    
    private fun calculateDailySpending(): Double {
        // Calculate today's spending
        // For now, return a mock value based on weekly savings
        return maxOf(0.0, 500.0 - (_progress.value.weeklySavings / 7))
    }
    
    private fun calculateMorningTracking(): Int {
        // Calculate how many days user tracked expenses before 10 AM
        // For now, return based on daily streak
        return (_progress.value.dailyStreak / 3).coerceAtMost(7)
    }
    
    private fun calculateCategoryDiversity(): Int {
        // Calculate how many different categories user used this week
        // For now, return a mock value
        return minOf(5, (_progress.value.expensesTracked / 3).coerceAtLeast(1))
    }
    
    private fun calculateWeekendSpending(): Double {
        // Calculate weekend spending (Saturday + Sunday)
        // For now, return a mock value
        return maxOf(0.0, 300.0 - (_progress.value.weeklySavings / 10))
    }
    
    private fun calculateMonthlyExpenseCount(): Int {
        // Calculate expenses tracked this month
        // For now, return based on weekly count
        return _progress.value.expensesTracked * 4
    }
    
    private fun calculateMonthlySavings(): Double {
        // Calculate savings this month
        // For now, return based on weekly savings
        return _progress.value.weeklySavings * 4
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
    
    private fun calculateEveningTracking(): Int {
        // Calculate how many days user tracked expenses after 6 PM
        // For now, return based on daily streak
        return (_progress.value.dailyStreak / 2).coerceAtMost(7)
    }
    
    private fun calculateTotalPoints() {
        val earnedRewards = _rewards.value.filter { it.isEarned }.sumOf { it.pointsValue.toLong() }
        val earnedBadges = _badges.value.filter { it.isEarned }.sumOf { 100L } // Each badge worth 100 points
        val completedChallenges = _challenges.value.filter { it.isCompleted }.sumOf { it.rewardPoints.toLong() }
        
        _totalPoints.value = (earnedRewards + earnedBadges + completedChallenges).toInt()
    }

    fun refreshRewards() {
        viewModelScope.launch {
            loadRewardsData()
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
                calculateTotalPoints()
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
        val challenges = _challenges.value.filter { it.isActive }
        return ChallengeTimeFrame.values().associateWith { timeFrame ->
            val timeFrameChallenges = challenges.filter { it.timeFrame == timeFrame }
            val completed = timeFrameChallenges.count { it.isCompleted }
            val total = timeFrameChallenges.size
            completed to total
        }
    }
}
