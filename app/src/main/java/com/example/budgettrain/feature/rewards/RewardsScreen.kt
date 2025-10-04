package com.example.budgettrain.feature.rewards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun RewardsScreen(viewModel: RewardsViewModel = viewModel()) {
    val rewards by viewModel.rewards.collectAsState()
    val badges by viewModel.badges.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val challenges by viewModel.challenges.collectAsState()

    val context = LocalContext.current

    // Debug logging
    LaunchedEffect(challenges) {
        println("DEBUG: RewardsScreen - challenges updated: ${challenges.size} total, ${challenges.count { it.isActive }} active")
    }

    LaunchedEffect(Unit) {
        viewModel.refreshRewards()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            BrandHeader()
        }
        
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        item {
            Text(
                "Rewards & Achievements", 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3)
            )
        }

        item {
            ProgressOverviewCard(progress = progress)
        }

        item {
            StatsCards(
                rewardCount = rewards.count { it.isEarned },
                badgeCount = badges.count { it.isEarned },
                totalRewards = rewards.size,
                totalBadges = badges.size
            )
        }

        item {
            ChallengeProgressOverview(challenges = challenges)
        }
        
        item {
            ChallengeQuickActions(challenges = challenges)
        }
        
        item {
            ChallengeSection(challenges = challenges)
        }

        item {
            Text(
                "Available Rewards",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
        }

        items(rewards.size) { index ->
            EnhancedRewardCard(reward = rewards[index])
        }

        item {
            Text(
                "Achievement Badges",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
        }

        items(badges.size) { index ->
            EnhancedBadgeCard(badge = badges[index])
        }
        
        item {
            Spacer(modifier = Modifier.height(20.dp))
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
            text = "REWARDING SMART SPENDING",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF607D8B)
        )
    }
}

@Composable
private fun StatsCards(rewardCount: Int, badgeCount: Int, totalRewards: Int, totalBadges: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            title = "Rewards Earned",
            value = "$rewardCount/$totalRewards",
            icon = Icons.Default.CardGiftcard,
            color = Color(0xFF4CAF50),
            subtitle = "${(rewardCount.toFloat() / totalRewards * 100).toInt()}% Complete",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Badges Unlocked", 
            value = "$badgeCount/$totalBadges",
            icon = Icons.Default.Star,
            color = Color(0xFFFF9800),
            subtitle = "${(badgeCount.toFloat() / totalBadges * 100).toInt()}% Complete",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666)
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF888888),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// New Progress Overview Card
@Composable
private fun ProgressOverviewCard(progress: RewardProgress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Your Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            ProgressItem(
                label = "Daily Tracking Streak",
                value = "${progress.dailyStreak} days",
                progress = minOf(1f, progress.dailyStreak / 7f)
            )
            
            ProgressItem(
                label = "Expenses Tracked",
                value = "${progress.expensesTracked}",
                progress = minOf(1f, progress.expensesTracked / 50f)
            )
            
            ProgressItem(
                label = "Weekly Savings Achieved",
                value = "R${String.format("%.0f", progress.weeklySavings)}",
                progress = minOf(1f, progress.weeklySavings.toFloat() / 1000f)
            )
        }
    }
}

@Composable
private fun ProgressItem(label: String, value: String, progress: Float) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = Color(0xFF2196F3),
            trackColor = Color(0xFFE0E0E0)
        )
    }
}

@Composable
private fun EnhancedRewardCard(reward: Reward) {
    val iconColor = if (reward.isEarned) Color(0xFF4CAF50) else Color(0xFFCCCCCC)
    val borderColor = if (reward.isEarned) Color(0xFF4CAF50) else Color(0xFFE0E0E0)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (reward.isEarned) Color(0xFFF1F8E9) else Color.White
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getRewardIcon(reward.iconType),
                        contentDescription = "Reward",
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reward.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (reward.isEarned) Color(0xFF333333) else Color(0xFF999999)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = reward.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (reward.isEarned) Color(0xFF666666) else Color(0xFFBBBBBB)
                    )
                    if (!reward.isEarned && reward.progress > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = reward.progress,
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF2196F3),
                            trackColor = Color(0xFFE0E0E0)
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    if (reward.isEarned) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Earned",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "${reward.pointsValue} pts",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF888888),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedBadgeCard(badge: Badge) {
    val iconColor = if (badge.isEarned) Color(0xFFFF9800) else Color(0xFFCCCCCC)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (badge.isEarned) Color(0xFFFFF3E0) else Color.White
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getBadgeIcon(badge.iconType),
                        contentDescription = "Badge",
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = badge.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (badge.isEarned) Color(0xFF333333) else Color(0xFF999999)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = badge.criteria,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (badge.isEarned) Color(0xFF666666) else Color(0xFFBBBBBB)
                    )
                    if (!badge.isEarned && badge.progress > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = badge.progress,
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFFF9800),
                            trackColor = Color(0xFFE0E0E0)
                        )
                    }
                    
                    if (badge.isEarned && badge.earnedDate != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Earned ${SimpleDateFormat("MMM dd", Locale.getDefault()).format(badge.earnedDate)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                if (badge.isEarned) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Achieved",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// Helper functions for icons
private fun getRewardIcon(iconType: RewardIconType): ImageVector {
    return when (iconType) {
        RewardIconType.GIFT_CARD -> Icons.Default.CardGiftcard
        RewardIconType.COINS -> Icons.Default.AttachMoney
        RewardIconType.TROPHY -> Icons.Default.EmojiEvents
        RewardIconType.GEM -> Icons.Default.Star
        RewardIconType.DIAMOND -> Icons.Default.Star
        RewardIconType.MEDAL -> Icons.Default.EmojiEvents
    }
}

private fun getBadgeIcon(iconType: BadgeIconType): ImageVector {
    return when (iconType) {
        BadgeIconType.STAR -> Icons.Default.Star
        BadgeIconType.MEDAL -> Icons.Default.EmojiEvents
        BadgeIconType.CROWN -> Icons.Default.StarBorder
        BadgeIconType.AWARD -> Icons.Default.Star
    }
}

@Composable
private fun ChallengeProgressOverview(challenges: List<Challenge>) {
    val activeChallenges = challenges.filter { it.isActive }
    val completedChallenges = activeChallenges.count { it.isCompleted }
    val totalChallenges = activeChallenges.size
    val overallProgress = if (totalChallenges > 0) (completedChallenges.toFloat() / totalChallenges) else 0f
    
    // Group by time frame for detailed progress
    val dailyChallenges = activeChallenges.filter { it.timeFrame == ChallengeTimeFrame.DAILY }
    val weeklyChallenges = activeChallenges.filter { it.timeFrame == ChallengeTimeFrame.WEEKLY }
    val monthlyChallenges = activeChallenges.filter { it.timeFrame == ChallengeTimeFrame.MONTHLY }
    val oneTimeChallenges = activeChallenges.filter { it.timeFrame == ChallengeTimeFrame.ONE_TIME }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Challenge Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
                Text(
                    "$completedChallenges/$totalChallenges completed",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Overall progress bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Overall Progress",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                    Text(
                        "${(overallProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { overallProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = Color(0xFF2196F3),
                    trackColor = Color(0xFFE0E0E0)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Time frame breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProgressBreakdown(
                    title = "Daily",
                    completed = dailyChallenges.count { it.isCompleted },
                    total = dailyChallenges.size,
                    color = Color(0xFF4CAF50)
                )
                ProgressBreakdown(
                    title = "Weekly",
                    completed = weeklyChallenges.count { it.isCompleted },
                    total = weeklyChallenges.size,
                    color = Color(0xFF2196F3)
                )
                ProgressBreakdown(
                    title = "Monthly",
                    completed = monthlyChallenges.count { it.isCompleted },
                    total = monthlyChallenges.size,
                    color = Color(0xFFFF9800)
                )
                ProgressBreakdown(
                    title = "Achievements",
                    completed = oneTimeChallenges.count { it.isCompleted },
                    total = oneTimeChallenges.size,
                    color = Color(0xFFE91E63)
                )
            }
            
            // Quick stats
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val totalPoints = activeChallenges.filter { it.isCompleted }.sumOf { it.rewardPoints }
                val availablePoints = activeChallenges.sumOf { it.rewardPoints }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$totalPoints",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                    Text(
                        "Points Earned",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF666666)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$availablePoints",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                    Text(
                        "Total Available",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF666666)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val almostComplete = activeChallenges.count { 
                        !it.isCompleted && it.targetValue > 0 && 
                        (it.currentValue / it.targetValue) >= 0.8 
                    }
                    Text(
                        "$almostComplete",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        "Almost Done",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF666666)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressBreakdown(
    title: String,
    completed: Int,
    total: Int,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF666666),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "$completed/$total",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        val progress = if (total > 0) completed.toFloat() / total else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .width(40.dp)
                .height(4.dp),
            color = color,
            trackColor = Color(0xFFE0E0E0)
        )
    }
}

@Composable
private fun ChallengeQuickActions(challenges: List<Challenge>) {
    val activeChallenges = challenges.filter { it.isActive && !it.isCompleted }
    
    // Find challenges that are close to completion (80% or more)
    val almostComplete = activeChallenges.filter { 
        it.targetValue > 0 && (it.currentValue / it.targetValue) >= 0.8 
    }
    
    // Find easy challenges that can be completed quickly
    val easyChallenges = activeChallenges.filter { 
        it.difficulty == ChallengeDifficulty.EASY && it.targetValue > 0 && 
        (it.currentValue / it.targetValue) >= 0.5 
    }
    
    // Find daily challenges that can be done today
    val dailyChallenges = activeChallenges.filter { 
        it.timeFrame == ChallengeTimeFrame.DAILY 
    }
    
    if (almostComplete.isNotEmpty() || easyChallenges.isNotEmpty() || dailyChallenges.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Quick Actions",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Quick Actions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Almost complete challenges
                if (almostComplete.isNotEmpty()) {
                    QuickActionItem(
                        title = "Almost Complete!",
                        description = "${almostComplete.size} challenge${if (almostComplete.size > 1) "s" else ""} nearly finished",
                        action = "Complete them now",
                        color = Color(0xFF4CAF50),
                        icon = Icons.Default.CheckCircle
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Easy challenges
                if (easyChallenges.isNotEmpty()) {
                    QuickActionItem(
                        title = "Easy Wins",
                        description = "${easyChallenges.size} easy challenge${if (easyChallenges.size > 1) "s" else ""} ready to complete",
                        action = "Quick points available",
                        color = Color(0xFF4CAF50),
                        icon = Icons.Default.Star
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Daily challenges
                if (dailyChallenges.isNotEmpty()) {
                    QuickActionItem(
                        title = "Daily Tasks",
                        description = "${dailyChallenges.size} daily challenge${if (dailyChallenges.size > 1) "s" else ""} to do today",
                        action = "Don't miss out!",
                        color = Color(0xFFFF9800),
                        icon = Icons.Default.Schedule
                    )
                }
                
                // Show specific next actions
                val nextActions = mutableListOf<String>()
                activeChallenges.forEach { challenge ->
                    val remaining = challenge.targetValue - challenge.currentValue
                    if (remaining > 0) {
                        when (challenge.targetType) {
                            ChallengeType.DAILY_EXPENSES -> {
                                if (remaining <= 3) nextActions.add("Track ${remaining.toInt()} more expenses")
                            }
                            ChallengeType.SAVINGS_TARGET -> {
                                if (remaining <= 500) nextActions.add("Save R${String.format("%.0f", remaining)} more")
                            }
                            ChallengeType.WEEKLY_SPENDING_LIMIT -> {
                                if (remaining <= 1000) nextActions.add("Stay R${String.format("%.0f", remaining)} under budget")
                            }
                            ChallengeType.EXPENSE_FREE_DAYS -> {
                                if (remaining <= 2) nextActions.add("Have ${remaining.toInt()} more expense-free days")
                            }
                            else -> {}
                        }
                    }
                }
                
                if (nextActions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Next Steps:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    nextActions.take(3).forEach { action ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Next step",
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                action,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF666666)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionItem(
    title: String,
    description: String,
    action: String,
    color: Color,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666)
            )
        }
        
        Text(
            action,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
private fun ChallengeSection(challenges: List<Challenge>) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Active Challenges",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
            
            // Challenge stats
            val completedCount = challenges.count { it.isCompleted }
            val totalCount = challenges.count { it.isActive }
            Text(
                "$completedCount/$totalCount completed",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF2196F3),
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        val activeChallenges = challenges.filter { it.isActive }
        println("DEBUG: ChallengeSection - total challenges: ${challenges.size}, active challenges: ${activeChallenges.size}")
        if (activeChallenges.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No active challenges",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF888888)
                )
            }
        } else {
            // Group challenges by time frame
            val dailyChallenges = activeChallenges.filter { it.timeFrame == ChallengeTimeFrame.DAILY }
            val weeklyChallenges = activeChallenges.filter { it.timeFrame == ChallengeTimeFrame.WEEKLY }
            val monthlyChallenges = activeChallenges.filter { it.timeFrame == ChallengeTimeFrame.MONTHLY }
            val oneTimeChallenges = activeChallenges.filter { it.timeFrame == ChallengeTimeFrame.ONE_TIME }
            
            // Daily Challenges
            if (dailyChallenges.isNotEmpty()) {
                ChallengeGroup(
                    title = "Daily Challenges",
                    challenges = dailyChallenges,
                    color = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Weekly Challenges
            if (weeklyChallenges.isNotEmpty()) {
                ChallengeGroup(
                    title = "Weekly Challenges", 
                    challenges = weeklyChallenges,
                    color = Color(0xFF2196F3)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Monthly Challenges
            if (monthlyChallenges.isNotEmpty()) {
                ChallengeGroup(
                    title = "Monthly Challenges",
                    challenges = monthlyChallenges,
                    color = Color(0xFFFF9800)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // One-time Challenges
            if (oneTimeChallenges.isNotEmpty()) {
                ChallengeGroup(
                    title = "Achievement Challenges",
                    challenges = oneTimeChallenges,
                    color = Color(0xFFE91E63)
                )
            }
        }
    }
}

@Composable
private fun ChallengeGroup(
    title: String,
    challenges: List<Challenge>,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(color, CircleShape)
            )
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "${challenges.count { it.isCompleted }}/${challenges.size}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(challenges) { challenge ->
                EnhancedChallengeCard(challenge = challenge, groupColor = color)
            }
        }
    }
}

@Composable
private fun EnhancedChallengeCard(challenge: Challenge, groupColor: Color) {
    val progress = if (challenge.targetValue > 0) {
        (challenge.currentValue.toFloat() / challenge.targetValue.toFloat()).coerceIn(0f, 1f)
    } else 0f
    
    val difficultyColor = when (challenge.difficulty) {
        ChallengeDifficulty.EASY -> Color(0xFF4CAF50)
        ChallengeDifficulty.MEDIUM -> Color(0xFF2196F3)
        ChallengeDifficulty.HARD -> Color(0xFFFF9800)
        ChallengeDifficulty.EXPERT -> Color(0xFFE91E63)
    }
    
    val timeFrameColor = when (challenge.timeFrame) {
        ChallengeTimeFrame.DAILY -> Color(0xFF4CAF50)
        ChallengeTimeFrame.WEEKLY -> Color(0xFF2196F3)
        ChallengeTimeFrame.MONTHLY -> Color(0xFFFF9800)
        ChallengeTimeFrame.ONE_TIME -> Color(0xFFE91E63)
    }
    
    Card(
        modifier = Modifier.width(220.dp),
        elevation = CardDefaults.cardElevation(if (challenge.isCompleted) 4.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (challenge.isCompleted) 
                Color(0xFFE8F5E8) else Color.White
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header with icon, difficulty, and time frame
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = getRewardIcon(challenge.iconType),
                        contentDescription = "Challenge",
                        tint = if (challenge.isCompleted) Color(0xFF4CAF50) else groupColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = challenge.difficulty.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = difficultyColor,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Time frame indicator
                Box(
                    modifier = Modifier
                        .background(timeFrameColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = challenge.timeFrame.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = timeFrameColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Title
            Text(
                text = challenge.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Description
            Text(
                text = challenge.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            // Category indicator if applicable
            if (challenge.categoryName != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Category: ${challenge.categoryName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF666666),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Progress section with percentage
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${challenge.currentValue.toInt()}/${challenge.targetValue.toInt()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666),
                            fontWeight = FontWeight.Medium
                        )
                        // Progress percentage
                        val progressPercentage = if (challenge.targetValue > 0) {
                            ((challenge.currentValue / challenge.targetValue) * 100).toInt()
                        } else 0
                        Text(
                            text = "${progressPercentage}% complete",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (progressPercentage >= 100) Color(0xFF4CAF50) else groupColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Points",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${challenge.rewardPoints}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
                
                // Remaining progress indicator
                if (!challenge.isCompleted && challenge.targetValue > 0) {
                    val remaining = challenge.targetValue - challenge.currentValue
                    if (remaining > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when {
                                challenge.targetType == ChallengeType.DAILY_EXPENSES -> 
                                    "${remaining.toInt()} more expenses needed"
                                challenge.targetType == ChallengeType.SAVINGS_TARGET -> 
                                    "R${String.format("%.0f", remaining)} more to save"
                                challenge.targetType == ChallengeType.WEEKLY_SPENDING_LIMIT -> 
                                    "R${String.format("%.0f", remaining)} under budget"
                                challenge.targetType == ChallengeType.CATEGORY_SPENDING_LIMIT -> 
                                    "R${String.format("%.0f", remaining)} under ${challenge.categoryName} limit"
                                challenge.targetType == ChallengeType.EXPENSE_FREE_DAYS -> 
                                    "${remaining.toInt()} more expense-free days"
                                challenge.targetType == ChallengeType.LOW_SPENDING_DAYS -> 
                                    "${remaining.toInt()} more low-spending days"
                                else -> "${remaining.toInt()} more needed"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF888888),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (challenge.isCompleted) Color(0xFF4CAF50) else groupColor,
                trackColor = Color(0xFFE0E0E0)
            )
            
            // Completion status
            if (challenge.isCompleted) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Completed!",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Repeatable indicator
            if (challenge.isRepeatable && challenge.completedCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Completed ${challenge.completedCount} times",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ChallengeCard(challenge: Challenge) {
    EnhancedChallengeCard(challenge = challenge, groupColor = Color(0xFF2196F3))
}
